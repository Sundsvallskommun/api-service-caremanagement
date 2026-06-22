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

/**
 * EB monitorings — date-bound watch/reminder objects on an errand. Unlike the income {@link WarningService warnings}
 * these carry no acknowledge lifecycle: a caseworker simply creates, edits and removes them. Every method is scoped to
 * the errand's namespace/municipality (404 when the errand is missing there); each monitoring has a required start date
 * and an optional end date that, when set, must not precede the start.
 */
@Service
public class MonitoringService {

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

	/** A single monitoring on an errand. Scoped: throws {@code 404} when the errand or monitoring is missing here. */
	@Transactional(readOnly = true)
	public Monitoring get(final String municipalityId, final String namespace, final String errandId, final String monitoringId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toMonitoring(require(errandId, monitoringId));
	}

	/**
	 * Create a monitoring on an errand. Scoped: throws {@code 404} when the errand is missing, {@code 400} on a bad date
	 * range.
	 */
	@Transactional
	public Monitoring create(final String municipalityId, final String namespace, final String errandId, final MonitoringRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		validateDates(request.getStartDate(), request.getEndDate());
		return toMonitoring(repository.save(FaMonitoringEntity.create()
			.withErrandId(errandId)
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

	private static Monitoring toMonitoring(final FaMonitoringEntity entity) {
		return Monitoring.create()
			.withId(entity.getId())
			.withTitle(entity.getTitle())
			.withDescription(entity.getDescription())
			.withStartDate(entity.getStartDate())
			.withEndDate(entity.getEndDate())
			.withCreatedBy(entity.getCreatedBy())
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}
}
