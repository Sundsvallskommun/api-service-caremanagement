package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFcMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.NormberakningAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Builds and posts the normberäkning to Lifecare FC from incomes already classified by the operaton regelverk.
 * caremanagement no longer fetches SSBTEK or evaluates the rålista — the regelverk lives in the process. This service
 * resolves each classified income's category to an FC income-type id (via {@link ClassifiedIncomeToFcMapper}),
 * assembles
 * the calculation against the applicant's proposal (via {@link NormberakningAssembler}), and posts it. It also reports
 * whether this month's normberäkning covers every income type the previous month's did — the EB process polls SSBTEK
 * daily until it does.
 */
@Service
public class NormberakningService {

	private static final Logger LOG = LoggerFactory.getLogger(NormberakningService.class);

	private final LifecareFcIntegration lifecareFcIntegration;
	private final LifecareEbCaseService lifecareEbCaseService;
	private final ObjectMapper objectMapper;

	public NormberakningService(final LifecareFcIntegration lifecareFcIntegration, final LifecareEbCaseService lifecareEbCaseService,
		final ObjectMapper objectMapper) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.lifecareEbCaseService = lifecareEbCaseService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the normberäkning from incomes already classified by the operaton regelverk, and report whether the
	 * information is complete — i.e. whether every income type on the previous month's normberäkning is present this
	 * month. The completeness lookup is best-effort: a failure reading the previous normberäkning never fails the just-
	 * created calculation; it is reported as complete (with no missing types) so the process is not wedged.
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the created calculation id plus the completeness verdict + missing income types
	 */
	public NormberakningResult buildAndPostFromClassified(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var body = NormberakningAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);
		final var calculationId = lifecareFcIntegration.createCalculation(body);

		final var missing = missingPreviousIncomeTypes(applicantPersonId, applicationMonth, classified, proposal);
		return new NormberakningResult(calculationId, missing.isEmpty(), missing);
	}

	private List<String> missingPreviousIncomeTypes(final String applicantPersonId, final YearMonth applicationMonth,
		final List<ClassifiedIncome> classified, final PersonBasedCalculationProposalDTO proposal) {
		try {
			final var previousTypes = lifecareEbCaseService.previousNormberakningIncomeTypes(applicantPersonId, applicationMonth);
			return ClassifiedIncomeToFcMapper.missingPreviousIncomeTypes(previousTypes, classified, proposal);
		} catch (final RuntimeException e) {
			LOG.warn("Could not determine normberäkning completeness against the previous month — treating as complete", e);
			return List.of();
		}
	}

	private List<ClassifiedIncome> parse(final String classifiedIncomesJson) {
		try {
			return List.of(objectMapper.readValue(classifiedIncomesJson, ClassifiedIncome[].class));
		} catch (final JacksonException e) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid classifiedIncomes JSON");
		}
	}
}
