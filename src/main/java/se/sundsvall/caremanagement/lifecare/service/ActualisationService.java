package se.sundsvall.caremanagement.lifecare.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.AktualiseringAssembler;

/**
 * Creates an ekonomiskt-bistånd intake (aktualisering) in Lifecare FC — the "API instead of RPA" case-intake step.
 * Fetches the applicant's FC aktualisering proposal, assembles the {@code PostAktualiseringsBodyRequest} against it
 * (via
 * {@link AktualiseringAssembler}), posts it, and returns the created aktualisering id.
 *
 * <p>
 * The write is a two-call exchange (proposal GET → aktualisering POST); both go through {@link LifecareFcIntegration},
 * which keeps the generated FC DTOs and the privacy-safe logging inside the integration layer. Mirrors
 * {@link NormberakningService}.
 */
@Service
public class ActualisationService {

	private final LifecareFcIntegration lifecareFcIntegration;

	public ActualisationService(final LifecareFcIntegration lifecareFcIntegration) {
		this.lifecareFcIntegration = lifecareFcIntegration;
	}

	/**
	 * Build and post the aktualisering for the applicant and intake date.
	 *
	 * @param  applicantPersonId the applicant's personnummer (the FC aktualisering owner)
	 * @param  date              the intake date
	 * @return                   the id of the aktualisering created in Lifecare FC
	 */
	public Integer create(final String applicantPersonId, final LocalDate date) {
		final var proposal = lifecareFcIntegration.getActualisationProposal(applicantPersonId);
		final var body = AktualiseringAssembler.assemble(applicantPersonId, proposal, date);
		return lifecareFcIntegration.createActualisation(body);
	}
}
