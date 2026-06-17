package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ClassifiedIncomeToFcMapper;
import se.sundsvall.caremanagement.lifecare.service.mapper.NormberakningAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Builds and posts the normberäkning to Lifecare FC from incomes already classified by the operaton regelverk.
 * caremanagement no longer fetches SSBTEK or evaluates the rålista — the regelverk lives in the process. This service
 * resolves each classified income's category to an FC income-type id (via {@link ClassifiedIncomeToFcMapper}),
 * assembles
 * the calculation against the applicant's proposal (via {@link NormberakningAssembler}), and posts it.
 */
@Service
public class NormberakningService {

	private final LifecareFcIntegration lifecareFcIntegration;
	private final ObjectMapper objectMapper;

	public NormberakningService(final LifecareFcIntegration lifecareFcIntegration, final ObjectMapper objectMapper) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.objectMapper = objectMapper;
	}

	/**
	 * Build and post the normberäkning from incomes already classified by the operaton regelverk.
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
