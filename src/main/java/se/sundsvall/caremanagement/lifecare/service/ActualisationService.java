package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.service.mapper.ActualisationAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;

import static java.util.Optional.ofNullable;

/**
 * Creates a financial-assistance intake (actualisation) in Lifecare FamilyCare — the "API instead of RPA" case-intake
 * step. Fetches the applicant's FamilyCare actualisation proposal, assembles the {@code PostAktualiseringsBodyRequest}
 * against it (via {@link ActualisationAssembler}), posts it, and returns the created actualisation id.
 *
 * <p>
 * The write is a two-call exchange (proposal GET → actualisation POST); both go through {@link
 * LifecareFamilyCare}, which keeps the generated FamilyCare DTOs and the privacy-safe logging inside the
 * integration layer. Mirrors {@link CalculationService}.
 */
@Service
@EnableConfigurationProperties(ActualisationProperties.class)
public class ActualisationService {

	private static final Logger LOG = LoggerFactory.getLogger(ActualisationService.class);

	private final LifecareFamilyCare lifecareFamilyCareIntegration;
	private final CaseworkerResolver caseworkerResolver;
	private final ActualisationProperties actualisationProperties;

	public ActualisationService(final LifecareFamilyCare lifecareFamilyCareIntegration, final CaseworkerResolver caseworkerResolver,
		final ActualisationProperties actualisationProperties) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.caseworkerResolver = caseworkerResolver;
		this.actualisationProperties = actualisationProperties;
	}

	/**
	 * Build and post the actualisation for the applicant and intake date. The caseworker is resolved off the applicant's
	 * most recent Lifecare Service and, when found, set as the actualisation {@code CaseworkerId}; the same user's network
	 * id is returned so the caller can assign the careM errand. Caseworker resolution is best-effort — a lookup failure
	 * is logged and the intake is still created without a caseworker.
	 *
	 * @param  applicantPersonId the applicant's personal identity number (the FamilyCare actualisation owner)
	 * @param  date              the intake date
	 * @return                   the created actualisation id and the errand assignee ({@code null} when no caseworker was
	 *                           found)
	 */
	public ActualisationResult createActualisation(final String municipalityId, final String applicantPersonId, final LocalDate date) {
		final var caseworker = resolveCaseworker(municipalityId, applicantPersonId, date);

		final var proposal = lifecareFamilyCareIntegration.getActualisationProposal(municipalityId, applicantPersonId);
		final var selection = ActualisationAssembler.assemble(applicantPersonId, proposal, date,
			caseworker.map(ResolvedCaseworker::caseworkerId).orElse(null), actualisationProperties);
		// A name that is not in the catalogue falls back to the first offered value, which is the guess the
		// configuration exists to remove - so it must never pass silently.
		if (!selection.misses().isEmpty()) {
			LOG.warn("Lifecare's actualisation catalogue has no entry for {} - falling back to the first offered value. "
				+ "Check the configured names against the catalogue.", String.join(", ", selection.misses()));
		}
		final var actualisationId = lifecareFamilyCareIntegration.createActualisation(municipalityId, selection.body());

		return new ActualisationResult(actualisationId, caseworker.map(ResolvedCaseworker::assignedUserId).orElse(null), selection.body().getServiceId());
	}

	/**
	 * The person's open financial-assistance service (insats) id in Lifecare — the key Lifecare's own case reads take
	 * (reminders, jobbstimulans, document proposals). Chosen by the same rule an intake is linked with, see
	 * {@link ActualisationAssembler#linkedServiceId}.
	 *
	 * @param  personId the person's personal identity number
	 * @return          the service id, or empty when the person has no open EB insats
	 */
	public Optional<Integer> findFinancialAssistanceServiceId(final String municipalityId, final String personId) {
		final var proposal = lifecareFamilyCareIntegration.getActualisationProposal(municipalityId, personId);
		return ActualisationAssembler.linkedServiceId(proposal, actualisationProperties);
	}

	/** Best-effort caseworker resolution — never blocks intake creation; a lookup failure resolves to no caseworker. */
	private Optional<ResolvedCaseworker> resolveCaseworker(final String municipalityId, final String applicantPersonId, final LocalDate date) {
		try {
			return caseworkerResolver.resolve(municipalityId, applicantPersonId, date);
		} catch (final RuntimeException e) {
			LOG.warn("Could not resolve caseworker for actualisation; creating intake without one: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * List the actualisations (case intakes) registered on a person in the given period, mapped to the privacy-safe
	 * {@link ActualisationSummary} projection (the personal identity number is dropped). The dates bound the Lifecare query
	 * and are
	 * formatted as ISO local dates. An empty/absent FamilyCare page maps to an empty list.
	 *
	 * @param  personId the person's personal identity number (the actualisation owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's actualisations in the period (newest-first as Lifecare returns them)
	 */
	public List<ActualisationSummary> listActualisations(final String municipalityId, final String personId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFamilyCareIntegration.getActualisations(municipalityId, personId, fromDate, toDate))
			.map(ApiPaginationCompositePersonBasedAktualiseringDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(ActualisationService::toSummary)
			.toList();
	}

	/**
	 * Project the generated FamilyCare DTO onto the privacy-safe summary — deliberately omitting the personId (personal
	 * identity number).
	 */
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
	public void uploadAttachment(final String municipalityId, final Integer actualisationId, final String fileName, final byte[] content,
		final String documentType, final String documentSenderType, final String title, final String senderName) {
		lifecareFamilyCareIntegration.postActualisationAttachment(municipalityId, actualisationId, documentType, documentSenderType, title, senderName, fileName, content);
	}
}
