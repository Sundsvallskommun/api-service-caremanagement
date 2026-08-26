package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaJobStimulusPeriodRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaJobStimulusPeriodEntity;

/**
 * The errand's jobbstimulans periods, mirrored out of Lifecare by the RPA supplements ingest. Lifecare deletes and
 * recreates its whole period set on every save (ids are one-shot), so the mirror follows the same semantics: each
 * delivery {@link #replaceAll replaces} the errand's full period set in one transaction — there is no per-period
 * identity to upsert on. Periods are decision support for the handläggare; the jobbstimulans amount on the
 * normberäkning stays the caseworker's call.
 */
@Service
public class JobStimulusPeriodService {

	private final ErrandService errandService;
	private final FaJobStimulusPeriodRepository repository;

	JobStimulusPeriodService(final ErrandService errandService, final FaJobStimulusPeriodRepository repository) {
		this.errandService = errandService;
		this.repository = repository;
	}

	/** The errand's jobbstimulans periods, earliest first. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public List<JobStimulusPeriod> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return repository.findByErrandIdOrderByFromDateAsc(errandId).stream()
			.map(JobStimulusPeriodService::toPeriod)
			.toList();
	}

	/**
	 * Replace the errand's full jobbstimulans period set with the given periods, in one transaction. An empty list
	 * empties the set ('fetched, nothing there'). Returns the number of stored periods. Scoped: throws {@code 404} when
	 * the errand is missing here.
	 */
	@Transactional
	public int replaceAll(final String municipalityId, final String namespace, final String errandId, final List<JobStimulusPeriod> periods) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		repository.deleteByErrandId(errandId);
		final var saved = repository.saveAll(periods.stream()
			.map(period -> FaJobStimulusPeriodEntity.create()
				.withErrandId(errandId)
				.withRole(period.role())
				.withFromDate(period.fromDate())
				.withToDate(period.toDate()))
			.toList());
		return saved.size();
	}

	private static JobStimulusPeriod toPeriod(final FaJobStimulusPeriodEntity entity) {
		return new JobStimulusPeriod(entity.getRole(), entity.getFromDate(), entity.getToDate());
	}
}
