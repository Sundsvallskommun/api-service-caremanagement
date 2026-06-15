package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.PreparedSsbtekIncomes;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * The cross-module driver for the SSBTEK→FC income pipeline: fetches the applicant's (and optional co-applicant's)
 * SSBTEK basis from financial-aid for the period the rules need (jämförelseperiod start … kontrollperiod end) and hands
 * them to {@link SsbtekIncomeService} to prepare. This is the single entry point an EB type module / worker calls to go
 * from personnummer + ansökningsmånad to {@link PreparedSsbtekIncomes}; assembling the rest of the
 * {@code PostCalculationBodyRequest} (norm, links, household, dates) and posting it remains the caller's.
 */
@Service
public class SsbtekIncomeFetchService {

	private final FinancialAidIntegration financialAidIntegration;
	private final SsbtekIncomeService ssbtekIncomeService;

	public SsbtekIncomeFetchService(final FinancialAidIntegration financialAidIntegration, final SsbtekIncomeService ssbtekIncomeService) {
		this.financialAidIntegration = financialAidIntegration;
		this.ssbtekIncomeService = ssbtekIncomeService;
	}

	/**
	 * Fetch the household's SSBTEK basis and prepare the FC income rows + warnings.
	 *
	 * @param  municipalityId      the id of the municipality
	 * @param  applicantPersonId   the applicant's personnummer
	 * @param  coApplicantPersonId the co-applicant's personnummer, or {@code null}/blank if there is none
	 * @param  applicationMonth    the month the application concerns
	 * @return                     the prepared income rows, unhandled incomes, and change warnings
	 */
	public PreparedSsbtekIncomes fetchAndPrepare(final String municipalityId, final String applicantPersonId, final String coApplicantPersonId, final YearMonth applicationMonth) {
		final var periods = SsbtekPeriods.forApplicationMonth(applicationMonth);
		final var fromDate = periods.jamforelseperiod().atDay(1).format(ISO_LOCAL_DATE);
		final var toDate = periods.kontrollperiod().atEndOfMonth().format(ISO_LOCAL_DATE);

		final var applicantBasis = financialAidIntegration.getFinancialAidBasis(municipalityId, applicantPersonId, fromDate, toDate);
		final var coApplicantBasis = ofNullable(coApplicantPersonId)
			.filter(StringUtils::hasText)
			.map(personId -> financialAidIntegration.getFinancialAidBasis(municipalityId, personId, fromDate, toDate))
			.orElse(null);

		return ssbtekIncomeService.prepareCalculationIncomes(applicantPersonId, applicantBasis, coApplicantBasis, applicationMonth);
	}
}
