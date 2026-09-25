package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.CO_APPLICANT_REFUSAL;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.buildJobStimulusAdd;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.toJobStimulusPeriods;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.refuse;

/**
 * The jobbstimulans periods of an errand's sökande and medsökande, read from and written to the insats in Lifecare,
 * the register of record. Every call lands in the errand's access log.
 */
@Service
public class ErrandLifecareJobStimulusService {

	static final String TARGET = "JOB_STIMULUS";
	private static final String READ_DESCRIPTION = "Läste jobbstimulans i Lifecare";

	private final LifecareErrandService errandService;
	private final LifecareAccessRecorder accessRecorder;
	private final LifecareJobStimulusApi jobStimulusApi;

	ErrandLifecareJobStimulusService(final LifecareErrandService errandService, final LifecareAccessRecorder accessRecorder, final LifecareJobStimulusApi jobStimulusApi) {
		this.errandService = errandService;
		this.accessRecorder = accessRecorder;
		this.jobStimulusApi = jobStimulusApi;
	}

	/**
	 * The periods on the errand's insats.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the sökandes and the medsökandes periods
	 */
	public List<LifecareJobStimulusPeriod> periods(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var raw = jobStimulusApi.readForService(errand.requireServiceId());
		accessRecorder.read(errand, TARGET, READ_DESCRIPTION);
		return toJobStimulusPeriods(raw);
	}

	/**
	 * Adds a period for the sökande. Lifecare's save replaces every period the person has, so the current set is read
	 * first and sent back whole with the new one; its end is Lifecare's two-year rule unless the caseworker set one.
	 * Refused (422) for a household with a medsökande, whose periods the save would otherwise lose.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the new period
	 * @return                every period after the save
	 */
	public List<LifecareJobStimulusPeriod> addPeriod(final String municipalityId, final String namespace, final String errandId, final LifecareJobStimulusPeriodRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		if (errandService.coApplicantPresent(errand)) {
			throw refuse(CO_APPLICANT_REFUSAL);
		}

		final var current = jobStimulusApi.readForService(serviceId);
		final var toDate = Optional.ofNullable(request.toDate()).orElseGet(() -> readToDate(request.fromDate()));
		accessRecorder.read(errand, TARGET, READ_DESCRIPTION);

		final var saved = jobStimulusApi.save(buildJobStimulusAdd(current, request.fromDate(), toDate));
		accessRecorder.written(errand, LifecareAccessEntry.CREATE, TARGET, "Lade till en jobbstimulansperiod i Lifecare (%s – %s)".formatted(request.fromDate(), toDate), null);
		return toJobStimulusPeriods(saved);
	}

	private String readToDate(final String fromDate) {
		return Optional.ofNullable(jobStimulusApi.readToDate(fromDate))
			.filter(JsonNode::isString)
			.map(JsonNode::asString)
			.filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Lifecare gave no end date for a jobbstimulans period starting " + fromDate));
	}
}
