package se.sundsvall.caremanagement.core.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.FindErrandsResponse;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeRegistry;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withNamespaceAndMunicipalityId;
import static se.sundsvall.caremanagement.core.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.caremanagement.core.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.caremanagement.core.service.mapper.ErrandMapper.toErrandList;
import static se.sundsvall.caremanagement.core.service.mapper.ErrandMapper.toFindErrandsResponse;
import static se.sundsvall.caremanagement.core.service.mapper.PatchMapper.patchErrand;

/**
 * Envelope service. Owns the {@code errand} table and publishes envelope-level events
 * ({@link ErrandCreated}, {@link ErrandStatusChanged}, {@link ErrandAssigned},
 * {@link ErrandDeleted}) on the application bus — other modules ({@code statushistory},
 * {@code notifications}, type modules) react via {@code @ApplicationModuleListener}.
 *
 * BPMN process start is intentionally NOT here (D6: per-type, deploy-time concern).
 */
@Service
@Transactional
public class ErrandService implements ErrandAccessGuard {

	private static final String NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String TYPED_CREATE_ONLY_MESSAGE = "Errands of type '%s' must be created via the type's own endpoint (which seeds the type data and starts its process), not the generic POST /errands";

	private final ErrandRepository errandRepository;
	private final ErrandNumberGenerator errandNumberGenerator;
	private final ApplicationEventPublisher eventPublisher;
	private final ErrandNotificationFilter errandNotificationFilter;
	private final ErrandTypeRegistry errandTypeRegistry;

	ErrandService(final ErrandRepository errandRepository, final ErrandNumberGenerator errandNumberGenerator, final ApplicationEventPublisher eventPublisher,
		final ErrandNotificationFilter errandNotificationFilter, final ErrandTypeRegistry errandTypeRegistry) {
		this.errandRepository = errandRepository;
		this.errandNumberGenerator = errandNumberGenerator;
		this.eventPublisher = eventPublisher;
		this.errandNotificationFilter = errandNotificationFilter;
		this.errandTypeRegistry = errandTypeRegistry;
	}

	/**
	 * Generic create (the public {@code POST /errands} endpoint). Rejects types whose module owns a dedicated create
	 * endpoint — those must go through it (which seeds the type data and starts the process), so the generic path can't
	 * produce a half-formed, process-less errand. Type modules use {@link #createTypedErrand} instead.
	 */
	public String createErrand(final String municipalityId, final String namespace, final Errand errand) {
		if (hasText(errand.getTypeSlug()) && errandTypeRegistry.requiresTypedCreate(errand.getTypeSlug())) {
			throw Problem.valueOf(BAD_REQUEST, TYPED_CREATE_ONLY_MESSAGE.formatted(errand.getTypeSlug()));
		}
		return create(municipalityId, namespace, errand);
	}

	/**
	 * Create the errand envelope for a type module's own create flow — bypasses the generic-create guard, since the
	 * module's endpoint is the authoritative create (it seeds the typed data and starts the process). Not for external
	 * callers.
	 */
	public String createTypedErrand(final String municipalityId, final String namespace, final Errand errand) {
		return create(municipalityId, namespace, errand);
	}

	private String create(final String municipalityId, final String namespace, final Errand errand) {
		final var entity = toErrandEntity(errand, namespace, municipalityId);
		if (!hasText(entity.getErrandNumber())) {
			entity.setErrandNumber(errandNumberGenerator.generate(municipalityId, namespace));
		}

		final var saved = errandRepository.save(entity);
		final var timestamp = nowTs();

		eventPublisher.publishEvent(new ErrandCreated(
			saved.getId(), saved.getTypeSlug(), municipalityId, namespace,
			saved.getReporterUserId(), saved.getAssignedUserId(), timestamp));

		publishAssignmentNotification(municipalityId, namespace, saved.getId(), saved.getAssignedUserId(), saved.getReporterUserId(),
			"CREATE", "New errand assigned to you");
		return saved.getId();
	}

	public Errand readErrand(final String municipalityId, final String namespace, final String errandId) {
		return toErrand(findEntity(municipalityId, namespace, errandId));
	}

	/**
	 * Asserts that an errand envelope exists for the given tenant. Shared pure existence guard for other modules
	 * (notes, notifications, conversation, permits, referrals, status history) so each does not duplicate the
	 * find-or-404 against the errand table — exposed to them through the {@link ErrandAccessGuard} port. Throws
	 * {@code NOT_FOUND} when no such errand exists; returns nothing.
	 */
	@Override
	@Transactional(readOnly = true)
	public void verifyExistingErrand(final String municipalityId, final String namespace, final String errandId) {
		if (!errandRepository.existsByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId));
		}
	}

	@Transactional(readOnly = true)
	public FindErrandsResponse findErrands(final String municipalityId, final String namespace, final Specification<ErrandEntity> filter,
		final boolean hasUnacknowledgedNotifications, final String notificationOwnerId, final Pageable pageable) {

		var combined = withNamespaceAndMunicipalityId(namespace, municipalityId);
		combined = ofNullable(filter).map(combined::and).orElse(combined);
		if (hasUnacknowledgedNotifications) {
			combined = combined.and(errandNotificationFilter.hasUnacknowledgedNotifications(municipalityId, namespace, notificationOwnerId));
		}
		return toFindErrandsResponse(errandRepository.findAll(combined, pageable));
	}

	@Transactional(readOnly = true)
	public long countErrands(final String municipalityId, final String namespace, final Specification<ErrandEntity> filter) {
		final var baseFilter = withNamespaceAndMunicipalityId(namespace, municipalityId);
		final var fullFilter = ofNullable(filter).map(baseFilter::and).orElse(baseFilter);
		return errandRepository.count(fullFilter);
	}

	/**
	 * Errands in the given status that have been untouched since on or before the cutoff — the "in status S since before
	 * T" query the conversation-archiving job runs to find long-closed errands.
	 */
	@Transactional(readOnly = true)
	public List<Errand> findByStatusTouchedBefore(final String municipalityId, final String namespace, final String status, final OffsetDateTime cutoff) {
		return toErrandList(errandRepository.findByMunicipalityIdAndNamespaceAndStatusAndTouchedLessThanEqual(municipalityId, namespace, status, cutoff));
	}

	public void updateErrand(final String municipalityId, final String namespace, final String errandId, final PatchErrand patch) {
		final var entity = findEntity(municipalityId, namespace, errandId);
		final var previousAssignee = entity.getAssignedUserId();
		final var previousStatus = entity.getStatus();

		patchErrand(entity, patch);
		errandRepository.save(entity);

		final var newAssignee = entity.getAssignedUserId();
		final var newStatus = entity.getStatus();
		final var timestamp = nowTs();

		ofNullable(newStatus)
			.filter(s -> !s.equals(previousStatus))
			.ifPresent(s -> eventPublisher.publishEvent(new ErrandStatusChanged(
				entity.getId(), entity.getTypeSlug(), municipalityId, namespace,
				previousStatus, s, /* changedBy */ null, timestamp)));

		if (hasText(newAssignee) && !newAssignee.equals(previousAssignee)) {
			eventPublisher.publishEvent(new ErrandAssigned(
				entity.getId(), entity.getTypeSlug(), municipalityId, namespace,
				previousAssignee, newAssignee, /* changedBy */ null, timestamp));

			publishAssignmentNotification(municipalityId, namespace, entity.getId(), newAssignee, entity.getReporterUserId(),
				"UPDATE", "Errand reassigned to you");
		}
	}

	/**
	 * Records the Operaton process instance started for an errand on its envelope. A type module calls this after
	 * starting the errand's BPMN process (process start is per-type — see the class note), so Cockpit/inspection and the
	 * frontend can link the errand to its running instance. Scoped to the namespace/municipality; publishes no events.
	 *
	 * <p>
	 * A targeted update rather than a load-then-save: linking runs in the async errand-created flow, concurrently with
	 * the applicant-name / classification writers on the same row, so a read-modify-write would risk a MariaDB
	 * snapshot-isolation conflict (error 1020).
	 */
	public void linkProcessInstance(final String municipalityId, final String namespace, final String errandId, final String processInstanceId) {
		if (errandRepository.updateProcessInstanceId(municipalityId, namespace, errandId, processInstanceId) == 0) {
			throw Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId));
		}
	}

	public void deleteErrand(final String municipalityId, final String namespace, final String errandId) {
		final var entity = findEntity(municipalityId, namespace, errandId);
		final var typeSlug = entity.getTypeSlug();
		errandRepository.delete(entity);

		eventPublisher.publishEvent(new ErrandDeleted(
			errandId, typeSlug, municipalityId, namespace, /* deletedBy */ null, nowTs()));
	}

	private ErrandEntity findEntity(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private void publishAssignmentNotification(final String municipalityId, final String namespace, final String errandId,
		final String assignedUserId, final String reporterUserId, final String type, final String description) {

		if (!hasText(assignedUserId) || assignedUserId.equals(reporterUserId)) {
			return;
		}
		eventPublisher.publishEvent(new NotificationRequest(
			municipalityId, namespace, errandId, assignedUserId, reporterUserId, type, "ERRAND", description));
	}

	private static OffsetDateTime nowTs() {
		return now(systemDefault()).truncatedTo(MILLIS);
	}
}
