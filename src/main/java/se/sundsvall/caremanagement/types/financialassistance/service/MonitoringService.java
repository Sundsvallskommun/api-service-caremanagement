package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Monitoring;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaMonitoringRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaMonitoringEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;

/**
 * financial assistance monitorings (watch/reminder objects) — date-bound watch/reminder objects on an errand. Unlike
 * the income
 * {@link WarningService warnings} these carry no acknowledge lifecycle: a caseworker simply creates, edits and removes
 * them. Every method is scoped to the errand's namespace/municipality (404 when the errand is missing there); each
 * monitoring has a required start date and an optional end date that, when set, must not precede the start.
 *
 * <p>
 * A monitoring carries a {@code source} ({@code CASEWORKER}, the default, or {@code LIFECARE}) and an optional
 * {@code lifecareId}. Lifecare FC exposes no watch/reminder endpoint, so the mirror is driven out-of-band by RPA: RPA
 * surfaces a Lifecare watch/reminder by POSTing {@code source=LIFECARE} with its {@code lifecareId} (idempotent per
 * errand + lifecareId, so re-runs don't duplicate), and mirrors a caseworker monitoring the other way, later stamping
 * back the {@code lifecareId} it was given in Lifecare. The provenance fields are system/RPA-managed: an update only
 * touches them when supplied, so a caseworker edit never drops them.
 * </p>
 */
@Service
public class MonitoringService {

	static final String SOURCE_CASEWORKER = "CASEWORKER";
	static final String SOURCE_LIFECARE = "LIFECARE";

	private final ErrandService errandService;
	private final FaMonitoringRepository repository;

	MonitoringService(final ErrandService errandService, final FaMonitoringRepository repository) {
		this.errandService = errandService;
		this.repository = repository;
	}

	/** The monitorings on an errand, oldest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<Monitoring> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return repository.findByErrandId(errandId).stream()
			.sorted(comparing(FaMonitoringEntity::getCreated, nullsLast(naturalOrder())))
			.map(MonitoringService::toMonitoring)
			.toList();
	}

	/** How many monitorings are on an errand. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public long count(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return repository.countByErrandId(errandId);
	}

	/** A single monitoring on an errand. Scoped: throws {@code 404} when the errand or monitoring is missing here. */
	@Transactional(readOnly = true)
	public Monitoring get(final String municipalityId, final String namespace, final String errandId, final String monitoringId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toMonitoring(require(errandId, monitoringId));
	}

	/**
	 * Create a monitoring on an errand — or, for a Lifecare-sourced one carrying a {@code lifecareId}, upsert it so RPA
	 * re-runs don't duplicate. Scoped: throws {@code 404} when the errand is missing, {@code 400} on a bad date range.
	 */
	@Transactional
	public Monitoring create(final String municipalityId, final String namespace, final String errandId, final MonitoringRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		validateDates(request.getStartDate(), request.getEndDate());

		final var entity = hasText(request.getLifecareId())
			? repository.findByErrandIdAndLifecareId(errandId, request.getLifecareId()).orElseGet(FaMonitoringEntity::create)
			: FaMonitoringEntity.create();

		return toMonitoring(repository.save(entity
			.withErrandId(errandId)
			.withSource(resolveSource(request.getSource()))
			.withLifecareId(request.getLifecareId())
			.withTitle(request.getTitle())
			.withDescription(request.getDescription())
			.withStartDate(request.getStartDate())
			.withEndDate(request.getEndDate())
			.withCreatedBy(request.getCreatedBy())));
	}

	/**
	 * Replace a monitoring's mutable fields. Scoped: throws {@code 404} when the errand or monitoring is missing,
	 * {@code 400} on a bad date range.
	 */
	@Transactional
	public Monitoring update(final String municipalityId, final String namespace, final String errandId, final String monitoringId, final MonitoringRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		validateDates(request.getStartDate(), request.getEndDate());
		final var entity = require(errandId, monitoringId)
			.withTitle(request.getTitle())
			.withDescription(request.getDescription())
			.withStartDate(request.getStartDate())
			.withEndDate(request.getEndDate())
			.withCreatedBy(request.getCreatedBy());
		// Provenance is system/RPA-managed: only overwrite when the request supplies it, so a caseworker edit keeps it.
		if (hasText(request.getSource())) {
			entity.setSource(resolveSource(request.getSource()));
		}
		if (hasText(request.getLifecareId())) {
			entity.setLifecareId(request.getLifecareId());
		}
		return toMonitoring(repository.save(entity));
	}

	/** Remove a monitoring from an errand. Scoped: throws {@code 404} when the errand or monitoring is missing here. */
	@Transactional
	public void delete(final String municipalityId, final String namespace, final String errandId, final String monitoringId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		repository.delete(require(errandId, monitoringId));
	}

	private FaMonitoringEntity require(final String errandId, final String monitoringId) {
		return repository.findByIdAndErrandId(monitoringId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Monitoring not found on errand"));
	}

	private static void validateDates(final LocalDate startDate, final LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw Problem.valueOf(BAD_REQUEST, "endDate must not be before startDate");
		}
	}

	/** The supplied source, defaulting to {@link #SOURCE_CASEWORKER} when blank (the form omits it for caseworker rows). */
	private static String resolveSource(final String source) {
		if (hasText(source)) {
			return source;
		}
		return SOURCE_CASEWORKER;
	}

	private static Monitoring toMonitoring(final FaMonitoringEntity entity) {
		return Monitoring.create()
			.withId(entity.getId())
			.withSource(entity.getSource())
			.withLifecareId(entity.getLifecareId())
			.withTitle(entity.getTitle())
			.withDescription(entity.getDescription())
			.withStartDate(entity.getStartDate())
			.withEndDate(entity.getEndDate())
			.withCreatedBy(entity.getCreatedBy())
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}
}
