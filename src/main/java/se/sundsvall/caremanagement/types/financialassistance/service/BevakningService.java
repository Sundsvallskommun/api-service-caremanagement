package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Bevakning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.BevakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaBevakningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaBevakningEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * EB bevakningar — date-bound watch/reminder objects on an errand. Unlike the income {@link WarningService warnings}
 * these carry no acknowledge lifecycle: a handläggare simply creates, edits and removes them. Every method is scoped to
 * the errand's namespace/municipality (404 when the errand is missing there); each bevakning has a required start date
 * and an optional end date that, when set, must not precede the start.
 */
@Service
public class BevakningService {

	private final ErrandService errandService;
	private final FaBevakningRepository repository;

	BevakningService(final ErrandService errandService, final FaBevakningRepository repository) {
		this.errandService = errandService;
		this.repository = repository;
	}

	/** The bevakningar on an errand, oldest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<Bevakning> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return repository.findByErrandId(errandId).stream()
			.sorted(comparing(FaBevakningEntity::getCreated, nullsLast(naturalOrder())))
			.map(BevakningService::toBevakning)
			.toList();
	}

	/** A single bevakning on an errand. Scoped: throws {@code 404} when the errand or bevakning is missing here. */
	@Transactional(readOnly = true)
	public Bevakning get(final String municipalityId, final String namespace, final String errandId, final String bevakningId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toBevakning(require(errandId, bevakningId));
	}

	/**
	 * Create a bevakning on an errand. Scoped: throws {@code 404} when the errand is missing, {@code 400} on a bad date
	 * range.
	 */
	@Transactional
	public Bevakning create(final String municipalityId, final String namespace, final String errandId, final BevakningRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		validateDates(request.getStartDate(), request.getEndDate());
		return toBevakning(repository.save(FaBevakningEntity.create()
			.withErrandId(errandId)
			.withTitle(request.getTitle())
			.withDescription(request.getDescription())
			.withStartDate(request.getStartDate())
			.withEndDate(request.getEndDate())
			.withCreatedBy(request.getCreatedBy())));
	}

	/**
	 * Replace a bevakning's mutable fields. Scoped: throws {@code 404} when the errand or bevakning is missing,
	 * {@code 400} on a bad date range.
	 */
	@Transactional
	public Bevakning update(final String municipalityId, final String namespace, final String errandId, final String bevakningId, final BevakningRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		validateDates(request.getStartDate(), request.getEndDate());
		final var entity = require(errandId, bevakningId)
			.withTitle(request.getTitle())
			.withDescription(request.getDescription())
			.withStartDate(request.getStartDate())
			.withEndDate(request.getEndDate())
			.withCreatedBy(request.getCreatedBy());
		return toBevakning(repository.save(entity));
	}

	/** Remove a bevakning from an errand. Scoped: throws {@code 404} when the errand or bevakning is missing here. */
	@Transactional
	public void delete(final String municipalityId, final String namespace, final String errandId, final String bevakningId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		repository.delete(require(errandId, bevakningId));
	}

	private FaBevakningEntity require(final String errandId, final String bevakningId) {
		return repository.findByIdAndErrandId(bevakningId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Bevakning not found on errand"));
	}

	private static void validateDates(final LocalDate startDate, final LocalDate endDate) {
		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
			throw Problem.valueOf(BAD_REQUEST, "endDate must not be before startDate");
		}
	}

	private static Bevakning toBevakning(final FaBevakningEntity entity) {
		return Bevakning.create()
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
