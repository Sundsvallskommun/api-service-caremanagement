package se.sundsvall.caremanagement.lifecare.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ActualisationAssembler;

/**
 * Creates an financial-assistance intake (actualisation) in Lifecare FC — the "API instead of RPA" case-intake step.
 * Fetches the applicant's FC actualisation proposal, assembles the {@code PostAktualiseringsBodyRequest} against it
 * (via
 * {@link ActualisationAssembler}), posts it, and returns the created actualisation id.
 *
 * <p>
 * The write is a two-call exchange (proposal GET → actualisation POST); both go through {@link LifecareFcIntegration},
 * which keeps the generated FC DTOs and the privacy-safe logging inside the integration layer. Mirrors
 * {@link CalculationService}.
 */
@Service
public class ActualisationService {

	private static final String PDF_MIME_TYPE = "application/pdf";

	private final LifecareFcIntegration lifecareFcIntegration;

	public ActualisationService(final LifecareFcIntegration lifecareFcIntegration) {
		this.lifecareFcIntegration = lifecareFcIntegration;
	}

	/**
	 * Build and post the actualisation for the applicant and intake date.
	 *
	 * @param  applicantPersonId the applicant's personnummer (the FC actualisation owner)
	 * @param  date              the intake date
	 * @return                   the id of the actualisation created in Lifecare FC
	 */
	public Integer create(final String applicantPersonId, final LocalDate date) {
		final var proposal = lifecareFcIntegration.getActualisationProposal(applicantPersonId);
		final var body = ActualisationAssembler.assemble(applicantPersonId, proposal, date);
		return lifecareFcIntegration.createActualisation(body);
	}

	/**
	 * Upload a generated PDF and bind it to an existing Lifecare actualisation (the "API instead of RPA" document
	 * write-back used by the conversation-archiving job). The file is sent as {@code application/pdf}.
	 *
	 * @param actualisationId    the Lifecare actualisation the document is bound to
	 * @param fileName           the file name shown in Lifecare
	 * @param content            the PDF bytes
	 * @param documentType       the Lifecare {@code InsertDocumentType} code
	 * @param documentSenderType the Lifecare {@code InsertDocumentSenderType} code
	 * @param title              the document title
	 * @param senderName         the sender name
	 */
	public void uploadAttachment(final Integer actualisationId, final String fileName, final byte[] content,
		final String documentType, final String documentSenderType, final String title, final String senderName) {
		lifecareFcIntegration.postActualisationAttachment(actualisationId, documentType, documentSenderType, title, senderName, fileName, PDF_MIME_TYPE, content);
	}
}
