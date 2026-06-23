package se.sundsvall.caremanagement.core.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
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
public class ErrandService {

	private static final String NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final ErrandNumberGenerator errandNumberGenerator;
	private final ApplicationEventPublisher eventPublisher;

	ErrandService(final ErrandRepository errandRepository, final ErrandNumberGenerator errandNumberGenerator, final ApplicationEventPublisher eventPublisher) {
		this.errandRepository = errandRepository;
		this.errandNumberGenerator = errandNumberGenerator;
		this.eventPublisher = eventPublisher;
	}

	public String createErrand(final String municipalityId, final String namespace, final Errand errand) {
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

	@Transactional(readOnly = true)
	public FindErrandsResponse findErrands(final String municipalityId, final String namespace, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var baseSpec = withNamespaceAndMunicipalityId(namespace, municipalityId);
		final var combined = ofNullable(filter).map(baseSpec::and).orElse(baseSpec);
		return toFindErrandsResponse(errandRepository.findAll(combined, pageable));
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

		Optional.of(newStatus)
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
	 */
	public void linkProcessInstance(final String municipalityId, final String namespace, final String errandId, final String processInstanceId) {
		final var entity = findEntity(municipalityId, namespace, errandId);
		entity.setProcessInstanceId(processInstanceId);
		errandRepository.save(entity);
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
