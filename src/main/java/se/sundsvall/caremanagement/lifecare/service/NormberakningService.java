package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.List;
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
 * Builds and posts the SSBTEK-driven normberäkning to Lifecare FC — the end-to-end step that turns a household's SSBTEK
 * income data into a normberäkning created in Lifecare. Fetches and prepares the income rows (via
 * {@link SsbtekIncomeFetchService}), assembles the full {@code PostCalculationBodyRequest} against the person's FC
 * calculation proposal (via {@link NormberakningAssembler}), posts it, and returns the created calculation id together
 * with the warnings the handläggare must review.
 *
 * <p>
 * The FC calculation proposal is fetched twice — once inside {@code fetchAndPrepare} (to resolve income-type ids) and
 * once here (to resolve the service / investigation / norm / aktualisering links). Both are idempotent GETs and the
 * proposal is stable for a given person, so the small extra call is accepted to keep the income pipeline untouched.
 */
@Service
public class NormberakningService {

	private final SsbtekIncomeFetchService ssbtekIncomeFetchService;
	private final LifecareFcIntegration lifecareFcIntegration;
	private final ObjectMapper objectMapper;

	public NormberakningService(final SsbtekIncomeFetchService ssbtekIncomeFetchService, final LifecareFcIntegration lifecareFcIntegration, final ObjectMapper objectMapper) {
		this.ssbtekIncomeFetchService = ssbtekIncomeFetchService;
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the normberäkning for the household and application month.
	 *
	 * @param  municipalityId      the id of the municipality
	 * @param  applicantPersonId   the applicant's personnummer
	 * @param  coApplicantPersonId the co-applicant's personnummer, or {@code null}/blank if there is none
	 * @param  applicationMonth    the month the application concerns
	 * @return                     the created FC calculation id plus the income warnings to review
	 */
	public NormberakningResult buildAndPost(final String municipalityId, final String applicantPersonId, final String coApplicantPersonId, final YearMonth applicationMonth) {
		final var prepared = ssbtekIncomeFetchService.fetchAndPrepare(municipalityId, applicantPersonId, coApplicantPersonId, applicationMonth);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var body = NormberakningAssembler.assemble(applicantPersonId, proposal, prepared.calculationIncomes(), applicationMonth);
		final var calculationId = lifecareFcIntegration.createCalculation(body);

		return new NormberakningResult(calculationId, prepared.unhandledIncomes(), prepared.changeWarnings());
	}

	/**
	 * Build and post the normberäkning from incomes already classified by the operaton regelverk. caremanagement no
	 * longer fetches SSBTEK or evaluates the rålista — it resolves each classified income's category to an FC income-type
	 * id, assembles the calculation against the applicant's proposal, posts it, and returns the created calculation id.
	 * The income warnings travel separately (from operaton), so they are not recomputed here.
	 *
	 * @param  applicantPersonId     the applicant's personnummer (the FC proposal owner)
	 * @param  applicationMonth      the month the application concerns
	 * @param  classifiedIncomesJson the operaton {@code classifiedIncomes} JSON
	 * @return                       the created FC calculation id
	 */
	public Integer buildAndPostFromClassified(final String applicantPersonId, final YearMonth applicationMonth, final String classifiedIncomesJson) {
		final var classified = parse(classifiedIncomesJson);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var incomes = ClassifiedIncomeToFcMapper.toCalculationIncomes(classified, proposal);
		final var body = NormberakningAssembler.assemble(applicantPersonId, proposal, incomes, applicationMonth);

		return lifecareFcIntegration.createCalculation(body);
	}

	private List<ClassifiedIncome> parse(final String classifiedIncomesJson) {
		try {
			return List.of(objectMapper.readValue(classifiedIncomesJson, ClassifiedIncome[].class));
		} catch (final JacksonException e) {
			throw Problem.valueOf(BAD_REQUEST, "Invalid classifiedIncomes JSON");
		}
	}
}
