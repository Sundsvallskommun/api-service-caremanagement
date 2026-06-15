package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.PreparedSsbtekIncomes;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static se.sundsvall.caremanagement.lifecare.service.mapper.SsbtekToFcIncomeMapper.toCalculationIncomes;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

/**
 * Composes the SSBTEK→FC income pipeline for one normberäkning: extract the applicant (and optional co-applicant)
 * incomes from their SSBTEK baskets, apply the kontroll-/jämförelseperiod transfer rule, detect significant changes,
 * then map the transferable incomes to FC income rows against the person's FC calculation proposal. Returns a
 * {@link PreparedSsbtekIncomes} — the rows to post plus the warnings to raise.
 *
 * <p>
 * The SSBTEK basis is passed in (a generic per-agency map), so this service stays inside the lifecare module — the
 * cross-module fetch via {@code FinancialAidIntegration} and the assembly + POST of the full
 * {@code PostCalculationBodyRequest} (norm, links, household, dates) are the caller's, who has the errand context.
 * Change detection is run over the combined household incomes.
 */
@Service
public class SsbtekIncomeService {

	private final LifecareFcIntegration lifecareFcIntegration;

	public SsbtekIncomeService(final LifecareFcIntegration lifecareFcIntegration) {
		this.lifecareFcIntegration = lifecareFcIntegration;
	}

	/**
	 * Prepare the FC income rows + warnings for the given household and ansökningsmånad.
	 *
	 * @param  applicantPersonId the applicant's personnummer (used to fetch the FC calculation proposal)
	 * @param  applicantBasis    the applicant's SSBTEK basis (per-agency map); may be {@code null}
	 * @param  coApplicantBasis  the co-applicant's SSBTEK basis, or {@code null} if there is no co-applicant
	 * @param  applicationMonth  the month the application concerns
	 * @return                   the prepared income rows, unhandled incomes, and change warnings
	 */
	public PreparedSsbtekIncomes prepareCalculationIncomes(
		final String applicantPersonId,
		final Map<String, Map<String, Object>> applicantBasis,
		final Map<String, Map<String, Object>> coApplicantBasis,
		final YearMonth applicationMonth) {

		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(SsbtekIncomeExtractor.extract(applicantBasis, APPLICANT));
		incomes.addAll(extractCoApplicant(coApplicantBasis));

		final var transferable = SsbtekPeriodSelector.selectTransferable(incomes, applicationMonth);
		final var changeWarnings = SsbtekChangeDetector.detectIncomeChanges(incomes, applicationMonth);
		final var proposal = lifecareFcIntegration.getCalculationProposal(applicantPersonId);
		final var mapping = toCalculationIncomes(transferable, proposal);

		return new PreparedSsbtekIncomes(mapping.calculationIncomes(), mapping.unhandledIncomes(), changeWarnings);
	}

	private static List<SsbtekIncome> extractCoApplicant(final Map<String, Map<String, Object>> coApplicantBasis) {
		if (coApplicantBasis == null) {
			return List.of();
		}
		return SsbtekIncomeExtractor.extract(coApplicantBasis, CO_APPLICANT);
	}
}
