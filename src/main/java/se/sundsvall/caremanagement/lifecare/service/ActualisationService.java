package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ActualisationAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

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

	private static final Logger LOG = LoggerFactory.getLogger(ActualisationService.class);

	private final LifecareFcIntegration lifecareFcIntegration;
	private final CaseworkerResolver caseworkerResolver;

	public ActualisationService(final LifecareFcIntegration lifecareFcIntegration, final CaseworkerResolver caseworkerResolver) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.caseworkerResolver = caseworkerResolver;
	}

	/**
	 * Build and post the actualisation for the applicant and intake date. The handläggare is resolved off the applicant's
	 * most recent Lifecare Service and, when found, set as the actualisation {@code CaseworkerId}; the same user's network
	 * id is returned so the caller can assign the careM errand. Caseworker resolution is best-effort — a lookup failure is
	 * logged and the intake is still created without a caseworker.
	 *
	 * @param  applicantPersonId the applicant's personnummer (the FC actualisation owner)
	 * @param  date              the intake date
	 * @return                   the created actualisation id and the resolved errand assignee (assignee {@code null} when
	 *                           no caseworker was resolved)
	 */
	public ActualisationResult create(final String applicantPersonId, final LocalDate date) {
		final var caseworker = resolveCaseworker(applicantPersonId, date);

		final var proposal = lifecareFcIntegration.getActualisationProposal(applicantPersonId);
		final var body = ActualisationAssembler.assemble(applicantPersonId, proposal, date,
			caseworker.map(ResolvedCaseworker::caseworkerId).orElse(null));
		final var actualisationId = lifecareFcIntegration.createActualisation(body);

		return new ActualisationResult(actualisationId, caseworker.map(ResolvedCaseworker::assignedUserId).orElse(null));
	}

	/** Best-effort caseworker resolution — never blocks intake creation; a lookup failure resolves to no caseworker. */
	private Optional<ResolvedCaseworker> resolveCaseworker(final String applicantPersonId, final LocalDate date) {
		try {
			return caseworkerResolver.resolve(applicantPersonId, date);
		} catch (final RuntimeException e) {
			LOG.warn("Could not resolve caseworker for actualisation; creating intake without one: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * List the actualisations (case intakes) registered on a person in the given period, mapped to the privacy-safe
	 * {@link ActualisationSummary} projection (the personnummer is dropped). The dates bound the Lifecare query and are
	 * formatted as ISO local dates. An empty/absent FC page maps to an empty list.
	 *
	 * @param  personId the person's personnummer (the actualisation owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's actualisations in the period (newest-first as Lifecare returns them)
	 */
	public List<ActualisationSummary> list(final String personId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFcIntegration.getActualisations(personId, fromDate.format(ISO_LOCAL_DATE), toDate.format(ISO_LOCAL_DATE), null, null, false))
			.map(ApiPaginationCompositePersonBasedAktualiseringDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(ActualisationService::toSummary)
			.toList();
	}

	/** Project the generated FC DTO onto the privacy-safe summary — deliberately omitting the personId (personnummer). */
	private static ActualisationSummary toSummary(final PersonBasedAktualiseringDTO dto) {
		return new ActualisationSummary(dto.getId(), dto.getType(), dto.getName(), dto.getDate(), dto.getReason(), dto.getRegards(),
			dto.getFromWho(), dto.getCaseworker(), dto.getOrganization(), dto.getStatus(), dto.getInvestigationId(), dto.getServiceId(), dto.getDecisionId());
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
