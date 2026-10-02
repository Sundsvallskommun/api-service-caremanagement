package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.buildJobStimulusAdd;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.buildJobStimulusRemove;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.toJobStimulusPeriods;

/**
 * The jobbstimulans periods of an errand's sökande and medsökande, read from and written to the insats in Lifecare,
 * the register of record. Every call lands in the errand's access log.
 */
@Service
public class ErrandLifecareJobStimulusService {

	static final String TARGET = "JOB_STIMULUS";
	static final String PERIOD_GONE = "Perioden finns inte längre i Lifecare. Läs om listan: Lifecare ger perioderna nya id vid varje sparning.";
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
	 * Adds a period for the sökande or the medsökande. Lifecare's save replaces both persons' periods, so the current sets
	 * are read first and sent back whole with the new one; its end is Lifecare's two-year rule unless the caseworker set
	 * one. A period for a medsökande the insats does not have in Lifecare is refused (422).
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
		final var role = Optional.ofNullable(request.role()).orElse(APPLICANT);

		final var current = jobStimulusApi.readForService(serviceId);
		final var toDate = Optional.ofNullable(request.toDate()).orElseGet(() -> readToDate(request.fromDate()));
		accessRecorder.read(errand, TARGET, READ_DESCRIPTION);

		final var saved = jobStimulusApi.save(buildJobStimulusAdd(current, role, request.fromDate(), toDate));
		accessRecorder.written(errand, LifecareAccessEntry.CREATE, TARGET, "Lade till en jobbstimulansperiod%s i Lifecare (%s – %s)".formatted(whose(role), request.fromDate(),
			toDate), null);
		return toJobStimulusPeriods(saved);
	}

	/**
	 * Removes one of the sökandes periods. Lifecare has no remove call: its web app saves the set without the period, and
	 * so does this. Every period that is saved gets a new id in Lifecare, so the answer carries the ids to use from now
	 * on. A period that is no longer in Lifecare (404) is usually one whose id an earlier save replaced. The sökandes and
	 * the medsökandes periods go back together, as for adding.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  periodId       Lifecare's jobStimulusId of the period, as last read
	 * @return                every period after the save
	 */
	public List<LifecareJobStimulusPeriod> removePeriod(final String municipalityId, final String namespace, final String errandId, final int periodId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();

		final var current = jobStimulusApi.readForService(serviceId);
		accessRecorder.read(errand, TARGET, READ_DESCRIPTION);
		final var removed = toJobStimulusPeriods(current).stream()
			.filter(period -> Objects.equals(period.id(), periodId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PERIOD_GONE));

		final var saved = jobStimulusApi.save(buildJobStimulusRemove(current, periodId));
		accessRecorder.written(errand, LifecareAccessEntry.DELETE, TARGET,
			"Tog bort en jobbstimulansperiod%s i Lifecare (%s – %s)".formatted(whose(removed.role()), removed.fromDate(), Objects.requireNonNullElse(removed.toDate(), "tills vidare")),
			String.valueOf(periodId));
		return toJobStimulusPeriods(saved);
	}

	/** Whose period the access log names: nothing for the sökande's, as before, the medsökande's said. */
	private static String whose(final String role) {
		if (CO_APPLICANT.equals(role)) {
			return " för medsökanden";
		}
		return "";
	}

	private String readToDate(final String fromDate) {
		return Optional.ofNullable(jobStimulusApi.readToDate(fromDate))
			.filter(JsonNode::isString)
			.map(JsonNode::asString)
			.filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Lifecare gave no end date for a jobbstimulans period starting " + fromDate));
	}
}
