package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaJobStimulusPeriodRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaJobStimulusPeriodEntity;

/**
 * The errand's jobbstimulans periods, as stored. They were mirrored out of Lifecare until that mirroring was retired
 * (2026-09-24) and are no longer refreshed; this read path stays until Draken reads jobbstimulans live from Lifecare.
 * Periods are decision support for the handläggare; the jobbstimulans amount on the
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

	private static JobStimulusPeriod toPeriod(final FaJobStimulusPeriodEntity entity) {
		return new JobStimulusPeriod(entity.getRole(), entity.getFromDate(), entity.getToDate());
	}
}
