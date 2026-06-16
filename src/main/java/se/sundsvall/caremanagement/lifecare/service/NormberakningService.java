package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.NormberakningAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;

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

	public NormberakningService(final SsbtekIncomeFetchService ssbtekIncomeFetchService, final LifecareFcIntegration lifecareFcIntegration) {
		this.ssbtekIncomeFetchService = ssbtekIncomeFetchService;
		this.lifecareFcIntegration = lifecareFcIntegration;
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
}
