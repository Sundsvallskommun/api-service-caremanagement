package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAddressDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServicePersonDTO;
import generated.se.sundsvall.lifecarefamilycare.User;
import generated.se.sundsvall.lifecareintegrator.Actualisation;
import generated.se.sundsvall.lifecareintegrator.Address;
import generated.se.sundsvall.lifecareintegrator.CaseService;
import generated.se.sundsvall.lifecareintegrator.Caseworker;
import generated.se.sundsvall.lifecareintegrator.Contact;
import generated.se.sundsvall.lifecareintegrator.Decision;
import generated.se.sundsvall.lifecareintegrator.DecisionsResponse;
import generated.se.sundsvall.lifecareintegrator.DocumentMetadata;
import generated.se.sundsvall.lifecareintegrator.FamilyCareDecisionDetails;
import generated.se.sundsvall.lifecareintegrator.PagedActualisationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedDocumentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedPaymentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedServiceResponse;
import generated.se.sundsvall.lifecareintegrator.Payment;
import generated.se.sundsvall.lifecareintegrator.Person;
import generated.se.sundsvall.lifecareintegrator.RelatedPerson;
import java.util.List;

import static generated.se.sundsvall.lifecareintegrator.Decision.SourceEnum.FAMILY_CARE;
import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.mapEach;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.toDouble;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.toInteger;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.toText;

/**
 * Translates the integrator's case models — decisions, actualisations, payments, services, documents, the person and
 * the caseworker directory — back into the FamilyCare DTOs the {@code lifecare} services consume.
 *
 * <p>
 * Two things are worth knowing before reading a mapping here. Wherever FamilyCare carries a person's identity, the
 * integrator carries a {@code partyId} instead and never emits a personal identity number; the party id goes into the
 * {@code personId} field, because that is the field the services read, and it is an identifier either way — but it is
 * not a personnummer and nothing may treat it as one. And the integrator's decisions cover both FamilyCare and
 * elderly care, while this interface is FamilyCare's, so the elderly-care ones are dropped rather than mapped into a
 * shape that has nowhere to put them.
 */
final class IntegratorCaseMapper {

	private IntegratorCaseMapper() {}

	// ---- Decisions -------------------------------------------------------------------------------------------------

	static ApiPaginationCompositePersonBasedDecisionDTO toDecisions(final DecisionsResponse response) {
		final var composite = new ApiPaginationCompositePersonBasedDecisionDTO();
		composite.setResult(ofNullable(response)
			.map(DecisionsResponse::getDecisions)
			.orElseGet(List::<Decision>of).stream()
			.filter(decision -> FAMILY_CARE.equals(decision.getSource()))
			.map(IntegratorCaseMapper::toDecision)
			.toList());

		// DecisionsResponse is unpaged — it carries per-source statuses instead of a page. Leaving the paging fields
		// unset says "unknown" rather than inventing a single page, and no caller reads them.
		return composite;
	}

	private static PersonBasedDecisionDTO toDecision(final Decision decision) {
		final var details = ofNullable(decision.getFamilyCareDetails()).orElseGet(FamilyCareDecisionDetails::new);

		return new PersonBasedDecisionDTO()
			.id(toInteger(decision.getDecisionId()))
			.date(toText(decision.getDecided()))
			.type(decision.getType())
			.fromDate(toText(decision.getValidFrom()))
			.toDate(toText(decision.getValidTo()))
			.reason(decision.getReason())
			.decisionMaker(decision.getDecisionMaker())
			.amount(toDouble(decision.getAmount()))
			.organization(details.getOrganization())
			.investigationExecutionId(details.getInvestigationExecutionId())
			.serviceId(details.getServiceId())
			.coApplicant(details.getCoApplicant())
			.reasonCoApplicant(details.getReasonCoApplicant())
			.connectedApplication(details.getConnectedApplication())
			.decisionPersonDTOs(mapEach(details.getPersons(), IntegratorCaseMapper::toDecisionPerson));
	}

	private static PersonBasedDecisionPersonDTO toDecisionPerson(final RelatedPerson person) {
		return new PersonBasedDecisionPersonDTO()
			.personId(person.getPartyId())
			.name(person.getName())
			.isCoApplicant(person.getCoApplicant());
	}

	// ---- Actualisations --------------------------------------------------------------------------------------------

	/**
	 * @param partyId the party id the reads were made for. FamilyCare stamps every actualisation with the person it
	 *                belongs to and the integrator does not repeat it, so it is carried in from the request.
	 */
	static ApiPaginationCompositePersonBasedAktualiseringDTO toActualisations(final PagedActualisationResponse response, final String partyId) {
		final var composite = new ApiPaginationCompositePersonBasedAktualiseringDTO();
		composite.setResult(mapEach(
			ofNullable(response).map(PagedActualisationResponse::getActualisations).orElse(null),
			actualisation -> toActualisation(actualisation, partyId)));
		ofNullable(response).map(PagedActualisationResponse::getMeta).ifPresent(meta -> {
			composite.setPageNumber(meta.getPage());
			composite.setPageSize(meta.getLimit());
			composite.setTotalNumberOfPages(meta.getTotalPages());
			composite.setTotalNumberOfRecords(toInteger(meta.getTotalRecords()));
		});

		return composite;
	}

	private static PersonBasedAktualiseringDTO toActualisation(final Actualisation actualisation, final String partyId) {
		return new PersonBasedAktualiseringDTO()
			.id(actualisation.getId())
			.type(actualisation.getType())
			.personId(partyId)
			.name(actualisation.getName())
			.date(toText(actualisation.getDate()))
			.reason(actualisation.getReason())
			.regards(actualisation.getRegards())
			.fromWho(actualisation.getFromWho())
			.caseworker(actualisation.getCaseworker())
			.organization(actualisation.getOrganization())
			.status(actualisation.getStatus())
			.investigationId(actualisation.getInvestigationId())
			.serviceId(actualisation.getServiceId())
			.decisionId(actualisation.getDecisionId());
	}

	// ---- Payments --------------------------------------------------------------------------------------------------

	static ApiPaginationCompositePersonBasedPaymentDTO toPayments(final PagedPaymentResponse response) {
		final var composite = new ApiPaginationCompositePersonBasedPaymentDTO();
		composite.setResult(mapEach(
			ofNullable(response).map(PagedPaymentResponse::getPayments).orElse(null),
			IntegratorCaseMapper::toPayment));
		ofNullable(response).map(PagedPaymentResponse::getMeta).ifPresent(meta -> {
			composite.setPageNumber(meta.getPage());
			composite.setPageSize(meta.getLimit());
			composite.setTotalNumberOfPages(meta.getTotalPages());
			composite.setTotalNumberOfRecords(toInteger(meta.getTotalRecords()));
		});

		return composite;
	}

	private static PersonBasedPaymentDTO toPayment(final Payment payment) {
		return new PersonBasedPaymentDTO()
			.id(payment.getId())
			.amount(toDouble(payment.getAmount()))
			.paymentMethod(payment.getPaymentMethod())
			.payDate(toText(payment.getPayDate()))
			.clearing(payment.getClearing())
			.accountNumber(payment.getAccountNumber())
			.name(payment.getName())
			.streetAddress(payment.getStreetAddress())
			.careOfAddress(payment.getCareOfAddress())
			.postalCode(payment.getPostalCode())
			.postalAddress(payment.getPostalAddress())
			.billingNumber(payment.getBillingNumber())
			.localNumber(payment.getLocalNumber())
			.voucherNumber(payment.getVoucherNumber())
			.message(payment.getMessage())
			.investigationExecutionId(payment.getInvestigationExecutionId())
			.serviceId(payment.getServiceId())
			.connectedApplication(payment.getConnectedApplication())
			.concernedMonth(payment.getConcernedMonth())
			.paymentPersonDTOs(mapEach(payment.getPersons(), person -> new PersonBasedPaymentPersonDTO()
				.personId(person.getPartyId())
				.name(person.getName())));
	}

	// ---- Services --------------------------------------------------------------------------------------------------

	static ApiPaginationCompositePersonBasedServiceDTO toServices(final PagedServiceResponse response) {
		final var composite = new ApiPaginationCompositePersonBasedServiceDTO();
		composite.setResult(mapEach(
			ofNullable(response).map(PagedServiceResponse::getServices).orElse(null),
			IntegratorCaseMapper::toService));
		ofNullable(response).map(PagedServiceResponse::getMeta).ifPresent(meta -> {
			composite.setPageNumber(meta.getPage());
			composite.setPageSize(meta.getLimit());
			composite.setTotalNumberOfPages(meta.getTotalPages());
			composite.setTotalNumberOfRecords(toInteger(meta.getTotalRecords()));
		});

		return composite;
	}

	private static PersonBasedServiceDTO toService(final CaseService service) {
		return new PersonBasedServiceDTO()
			.id(service.getId())
			.type(service.getType())
			.organization(service.getOrganization())
			.startDate(toText(service.getStartDate()))
			.endDate(toText(service.getEndDate()))
			.caseworker(service.getCaseworker())
			.coCaseworker(service.getCoCaseworker())
			.investigationId(service.getInvestigationId())
			.decisionId(service.getDecisionId())
			.applicant(service.getApplicant())
			.coApplicant(service.getCoApplicant())
			.servicePersonDTOs(mapEach(service.getPersons(), person -> new PersonBasedServicePersonDTO()
				.personId(person.getPartyId())
				.name(person.getName())));
	}

	// ---- Documents -------------------------------------------------------------------------------------------------

	static ApiPaginationCompositePersonBasedDocumentDTO toDocuments(final PagedDocumentResponse response) {
		final var composite = new ApiPaginationCompositePersonBasedDocumentDTO();
		composite.setResult(mapEach(
			ofNullable(response).map(PagedDocumentResponse::getDocuments).orElse(null),
			IntegratorCaseMapper::toDocument));
		ofNullable(response).map(PagedDocumentResponse::getMeta).ifPresent(meta -> {
			composite.setPageNumber(meta.getPage());
			composite.setPageSize(meta.getLimit());
			composite.setTotalNumberOfPages(meta.getTotalPages());
			composite.setTotalNumberOfRecords(toInteger(meta.getTotalRecords()));
		});

		return composite;
	}

	/**
	 * {@code ownerId} stays unset on this route, and careM's own {@code LifecareDocument.ownerId} is therefore null for
	 * documents read through the integrator. FamilyCare's owner id is polymorphic — usually an entity id, but a
	 * personal identity number for some owner types — and the integrator refuses to emit a personnummer at all, so
	 * there is nothing faithful to map. Nothing in careM branches on it; it is passed through to the frontend.
	 */
	private static PersonBasedDocumentDTO toDocument(final DocumentMetadata document) {
		return new PersonBasedDocumentDTO()
			.id(document.getId())
			.title(document.getTitle())
			.date(toText(document.getDate()))
			.documentType(document.getDocumentType())
			.ownerType(document.getOwnerType());
	}

	// ---- Person, contacts and the caseworker directory ---------------------------------------------------------------

	/**
	 * @param partyId the party id the read was made for, placed in {@code personId} for the same reason as everywhere
	 *                else on this route.
	 */
	static PersonBasedPersonDTO toPerson(final Person person, final String partyId) {
		return ofNullable(person)
			.map(source -> new PersonBasedPersonDTO()
				.personId(partyId)
				.customerNumber(source.getCustomerNumber())
				.name(source.getName())
				.streetAddress(source.getStreetAddress())
				.careOfAddress(source.getCareOfAddress())
				.postalCode(source.getPostalCode())
				.postalAddress(source.getPostalAddress())
				.phoneHome(source.getPhoneHome())
				.phoneWork(source.getPhoneWork())
				.phoneMobile(source.getPhoneMobile())
				.email(source.getEmail())
				.addressProtection(source.getAddressProtection())
				.secretPhone(source.getSecretPhone())
				.protectedRegistration(source.getProtectedRegistration()))
			.orElse(null);
	}

	static List<PersonBasedContactDTO> toContacts(final List<Contact> contacts) {
		return mapEach(contacts, contact -> new PersonBasedContactDTO()
			.id(contact.getId())
			.name(contact.getName())
			.employmentTitle(contact.getEmploymentTitle())
			.organizationName(contact.getOrganizationName())
			.phone(contact.getPhone())
			.email(contact.getEmail())
			.typeOfContact(contact.getTypeOfContact())
			.address(toAddress(contact.getAddress())));
	}

	private static PersonBasedAddressDTO toAddress(final Address address) {
		return ofNullable(address)
			.map(source -> new PersonBasedAddressDTO()
				.visitingAddress(source.getVisitingAddress())
				.streetAddress(source.getStreetAddress())
				.postalCode(source.getPostalCode())
				.postalAddress(source.getPostalAddress()))
			.orElse(null);
	}

	/**
	 * The caseworker directory. FamilyCare's {@code User} also carries a {@code personId} and a {@code vrkId}; the
	 * integrator omits both, and careM reads neither — it matches on {@code fullName} and hands back
	 * {@code networkUserId} or {@code id}.
	 */
	static List<User> toUsers(final List<Caseworker> caseworkers) {
		return mapEach(caseworkers, caseworker -> new User()
			.id(caseworker.getId())
			.hsaId(caseworker.getHsaId())
			.networkUserId(caseworker.getNetworkUserId())
			.firstName(caseworker.getFirstName())
			.lastName(caseworker.getLastName())
			.fullName(caseworker.getFullName())
			.description(caseworker.getDescription())
			.validFrom(toText(caseworker.getValidFrom()))
			.validTo(toText(caseworker.getValidTo()))
			.disabled(caseworker.getDisabled()));
	}

}
