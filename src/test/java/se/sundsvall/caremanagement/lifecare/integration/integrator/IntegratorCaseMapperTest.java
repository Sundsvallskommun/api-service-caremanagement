package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServicePersonDTO;
import generated.se.sundsvall.lifecareintegrator.Actualisation;
import generated.se.sundsvall.lifecareintegrator.Address;
import generated.se.sundsvall.lifecareintegrator.CaseService;
import generated.se.sundsvall.lifecareintegrator.Caseworker;
import generated.se.sundsvall.lifecareintegrator.Contact;
import generated.se.sundsvall.lifecareintegrator.Decision;
import generated.se.sundsvall.lifecareintegrator.DecisionsResponse;
import generated.se.sundsvall.lifecareintegrator.DocumentMetadata;
import generated.se.sundsvall.lifecareintegrator.ElderlyCareDecisionDetails;
import generated.se.sundsvall.lifecareintegrator.FamilyCareDecisionDetails;
import generated.se.sundsvall.lifecareintegrator.PagedActualisationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedDocumentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedPaymentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedServiceResponse;
import generated.se.sundsvall.lifecareintegrator.PagingMetaData;
import generated.se.sundsvall.lifecareintegrator.Payment;
import generated.se.sundsvall.lifecareintegrator.Person;
import generated.se.sundsvall.lifecareintegrator.RelatedPerson;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static generated.se.sundsvall.lifecareintegrator.Decision.SourceEnum.ELDERLY_CARE;
import static generated.se.sundsvall.lifecareintegrator.Decision.SourceEnum.FAMILY_CARE;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IntegratorCaseMapperTest {

	private static final String PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";
	private static final String OTHER_PARTY_ID = "0d1c2b3a-4f5e-6d7c-8b9a-0f1e2d3c4b5a";

	// ---- Decisions -------------------------------------------------------------------------------------------------

	@Test
	void decisionsAreMappedFromTheFamilyCareDetails() {
		final var response = new DecisionsResponse().decisions(List.of(new Decision()
			.source(FAMILY_CARE)
			.decisionId("4711")
			.decided(LocalDate.of(2026, JUNE, 3))
			.validFrom(LocalDate.of(2026, JUNE, 1))
			.validTo(LocalDate.of(2026, JUNE, 30))
			.type("Försörjningsstöd")
			.reason("Ansökan beviljad")
			.decisionMaker("Karin Karlsson")
			.amount(BigDecimal.valueOf(4820.5))
			.familyCareDetails(new FamilyCareDecisionDetails()
				.organization("IFO Ekonomiskt bistånd")
				.investigationExecutionId(31)
				.serviceId(42)
				.coApplicant("Bo Berg")
				.reasonCoApplicant("Sammanboende")
				.connectedApplication(53)
				.persons(List.of(
					new RelatedPerson().partyId(PARTY_ID).name("Berit Berg").coApplicant(false),
					new RelatedPerson().partyId(OTHER_PARTY_ID).name("Bo Berg").coApplicant(true))))));

		assertThat(IntegratorCaseMapper.toDecisions(response).getResult()).singleElement().satisfies(decision -> {
			assertThat(decision.getId()).isEqualTo(4711);
			assertThat(decision.getDate()).isEqualTo("2026-06-03");
			assertThat(decision.getFromDate()).isEqualTo("2026-06-01");
			assertThat(decision.getToDate()).isEqualTo("2026-06-30");
			assertThat(decision.getType()).isEqualTo("Försörjningsstöd");
			assertThat(decision.getReason()).isEqualTo("Ansökan beviljad");
			assertThat(decision.getDecisionMaker()).isEqualTo("Karin Karlsson");
			assertThat(decision.getAmount()).isEqualTo(4820.5);
			assertThat(decision.getOrganization()).isEqualTo("IFO Ekonomiskt bistånd");
			assertThat(decision.getInvestigationExecutionId()).isEqualTo(31);
			assertThat(decision.getServiceId()).isEqualTo(42);
			assertThat(decision.getCoApplicant()).isEqualTo("Bo Berg");
			assertThat(decision.getReasonCoApplicant()).isEqualTo("Sammanboende");
			assertThat(decision.getConnectedApplication()).isEqualTo(53);
			assertThat(decision.getDecisionPersonDTOs())
				.extracting(PersonBasedDecisionPersonDTO::getPersonId, PersonBasedDecisionPersonDTO::getName, PersonBasedDecisionPersonDTO::getIsCoApplicant)
				.containsExactly(
					tuple(PARTY_ID, "Berit Berg", false),
					tuple(OTHER_PARTY_ID, "Bo Berg", true));
		});
	}

	/**
	 * The integrator serves both FamilyCare and elderly care from one endpoint. This interface is FamilyCare's, and an
	 * elderly-care decision carries its payload in a details block that has no counterpart here, so letting one through
	 * would produce a decision that is mostly empty rather than one that is merely from elsewhere.
	 */
	@Test
	void elderlyCareDecisionsAreLeftOut() {
		final var response = new DecisionsResponse().decisions(List.of(
			new Decision().source(ELDERLY_CARE).decisionId("1").elderlyCareDetails(new ElderlyCareDecisionDetails().code("SoL")),
			new Decision().source(FAMILY_CARE).decisionId("2"),
			new Decision().decisionId("3")));

		assertThat(IntegratorCaseMapper.toDecisions(response).getResult())
			.extracting(PersonBasedDecisionDTO::getId).containsExactly(2);
	}

	/** A decision id that is not a number becomes null rather than failing the whole page. */
	@Test
	void anUnparseableDecisionIdDoesNotFailThePage() {
		final var response = new DecisionsResponse().decisions(List.of(
			new Decision().source(FAMILY_CARE).decisionId("FC-0001"),
			new Decision().source(FAMILY_CARE).decisionId("8")));

		assertThat(IntegratorCaseMapper.toDecisions(response).getResult())
			.extracting(PersonBasedDecisionDTO::getId).containsExactly(null, 8);
	}

	@Test
	void decisionsWithoutFamilyCareDetailsStillMap() {
		final var response = new DecisionsResponse().decisions(List.of(new Decision().source(FAMILY_CARE).decisionId("5").type("Avslag")));

		assertThat(IntegratorCaseMapper.toDecisions(response).getResult()).singleElement().satisfies(decision -> {
			assertThat(decision.getType()).isEqualTo("Avslag");
			assertThat(decision.getOrganization()).isNull();
			assertThat(decision.getDecisionPersonDTOs()).isEmpty();
		});
	}

	@Test
	void noDecisionResponseYieldsAnEmptyResult() {
		assertThat(IntegratorCaseMapper.toDecisions(null).getResult()).isEmpty();
	}

	// ---- Actualisations --------------------------------------------------------------------------------------------

	@Test
	void actualisationsAreMappedAndStampedWithThePartyId() {
		final var response = new PagedActualisationResponse()
			.meta(new PagingMetaData().page(2).limit(50).totalPages(4).totalRecords(151L))
			.actualisations(List.of(new Actualisation()
				.id(88)
				.type("Ansökan")
				.name("Återansökan hyra")
				.date(LocalDate.of(2026, JUNE, 12))
				.reason("Hyra")
				.regards("Juni")
				.fromWho("Den enskilde")
				.caseworker("Karin Karlsson")
				.organization("IFO Ekonomiskt bistånd")
				.status("Avslutad")
				.investigationId(11)
				.serviceId(22)
				.decisionId(33)));

		final var result = IntegratorCaseMapper.toActualisations(response, PARTY_ID);

		assertThat(result.getPageNumber()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(50);
		assertThat(result.getTotalNumberOfPages()).isEqualTo(4);
		assertThat(result.getTotalNumberOfRecords()).isEqualTo(151);
		assertThat(result.getResult()).singleElement().satisfies(actualisation -> {
			assertThat(actualisation.getId()).isEqualTo(88);
			assertThat(actualisation.getType()).isEqualTo("Ansökan");
			assertThat(actualisation.getPersonId()).isEqualTo(PARTY_ID);
			assertThat(actualisation.getName()).isEqualTo("Återansökan hyra");
			assertThat(actualisation.getDate()).isEqualTo("2026-06-12");
			assertThat(actualisation.getReason()).isEqualTo("Hyra");
			assertThat(actualisation.getRegards()).isEqualTo("Juni");
			assertThat(actualisation.getFromWho()).isEqualTo("Den enskilde");
			assertThat(actualisation.getCaseworker()).isEqualTo("Karin Karlsson");
			assertThat(actualisation.getOrganization()).isEqualTo("IFO Ekonomiskt bistånd");
			assertThat(actualisation.getStatus()).isEqualTo("Avslutad");
			assertThat(actualisation.getInvestigationId()).isEqualTo(11);
			assertThat(actualisation.getServiceId()).isEqualTo(22);
			assertThat(actualisation.getDecisionId()).isEqualTo(33);
		});
	}

	@Test
	void noActualisationResponseYieldsAnEmptyResult() {
		final var result = IntegratorCaseMapper.toActualisations(null, PARTY_ID);

		assertThat(result.getResult()).isEmpty();
		assertThat(result.getPageNumber()).isNull();
	}

	// ---- Payments --------------------------------------------------------------------------------------------------

	@Test
	void paymentsAreMapped() {
		final var response = new PagedPaymentResponse()
			.meta(new PagingMetaData().page(1).limit(20).totalPages(1).totalRecords(1L))
			.payments(List.of(new Payment()
				.id(901)
				.amount(BigDecimal.valueOf(6500.0))
				.paymentMethod("Bankgiro")
				.payDate(LocalDate.of(2026, JUNE, 27))
				.clearing("8327")
				.accountNumber("9012345")
				.name("Berit Berg")
				.streetAddress("Storgatan 1")
				.careOfAddress("c/o Berg")
				.postalCode("85230")
				.postalAddress("Sundsvall")
				.billingNumber("B-1")
				.localNumber("L-2")
				.voucherNumber("V-3")
				.message("Hyra juni")
				.investigationExecutionId(31)
				.serviceId(42)
				.connectedApplication(53)
				.concernedMonth("2026-06")
				.persons(List.of(new RelatedPerson().partyId(PARTY_ID).name("Berit Berg")))));

		final var result = IntegratorCaseMapper.toPayments(response);

		assertThat(result.getTotalNumberOfRecords()).isEqualTo(1);
		assertThat(result.getResult()).singleElement().satisfies(payment -> {
			assertThat(payment.getId()).isEqualTo(901);
			assertThat(payment.getAmount()).isEqualTo(6500.0);
			assertThat(payment.getPaymentMethod()).isEqualTo("Bankgiro");
			assertThat(payment.getPayDate()).isEqualTo("2026-06-27");
			assertThat(payment.getClearing()).isEqualTo("8327");
			assertThat(payment.getAccountNumber()).isEqualTo("9012345");
			assertThat(payment.getName()).isEqualTo("Berit Berg");
			assertThat(payment.getStreetAddress()).isEqualTo("Storgatan 1");
			assertThat(payment.getCareOfAddress()).isEqualTo("c/o Berg");
			assertThat(payment.getPostalCode()).isEqualTo("85230");
			assertThat(payment.getPostalAddress()).isEqualTo("Sundsvall");
			assertThat(payment.getBillingNumber()).isEqualTo("B-1");
			assertThat(payment.getLocalNumber()).isEqualTo("L-2");
			assertThat(payment.getVoucherNumber()).isEqualTo("V-3");
			assertThat(payment.getMessage()).isEqualTo("Hyra juni");
			assertThat(payment.getInvestigationExecutionId()).isEqualTo(31);
			assertThat(payment.getServiceId()).isEqualTo(42);
			assertThat(payment.getConnectedApplication()).isEqualTo(53);
			assertThat(payment.getConcernedMonth()).isEqualTo("2026-06");
			assertThat(payment.getPaymentPersonDTOs())
				.extracting(PersonBasedPaymentPersonDTO::getPersonId, PersonBasedPaymentPersonDTO::getName)
				.containsExactly(tuple(PARTY_ID, "Berit Berg"));
		});
	}

	@Test
	void noPaymentResponseYieldsAnEmptyResult() {
		assertThat(IntegratorCaseMapper.toPayments(null).getResult()).isEmpty();
	}

	// ---- Services --------------------------------------------------------------------------------------------------

	@Test
	void servicesAreMapped() {
		final var response = new PagedServiceResponse()
			.meta(new PagingMetaData().page(1).limit(20).totalPages(1).totalRecords(1L))
			.services(List.of(new CaseService()
				.id(42)
				.type("Ekonomiskt bistånd")
				.organization("IFO Ekonomiskt bistånd")
				.startDate(LocalDate.of(2026, JUNE, 1))
				.endDate(LocalDate.of(2026, JUNE, 30))
				.caseworker("Karin Karlsson")
				.coCaseworker("Lars Larsson")
				.investigationId(11)
				.decisionId(33)
				.applicant("Berit Berg")
				.coApplicant("Bo Berg")
				.persons(List.of(new RelatedPerson().partyId(PARTY_ID).name("Berit Berg")))));

		assertThat(IntegratorCaseMapper.toServices(response).getResult()).singleElement().satisfies(service -> {
			assertThat(service.getId()).isEqualTo(42);
			assertThat(service.getType()).isEqualTo("Ekonomiskt bistånd");
			assertThat(service.getOrganization()).isEqualTo("IFO Ekonomiskt bistånd");
			assertThat(service.getStartDate()).isEqualTo("2026-06-01");
			assertThat(service.getEndDate()).isEqualTo("2026-06-30");
			assertThat(service.getCaseworker()).isEqualTo("Karin Karlsson");
			assertThat(service.getCoCaseworker()).isEqualTo("Lars Larsson");
			assertThat(service.getInvestigationId()).isEqualTo(11);
			assertThat(service.getDecisionId()).isEqualTo(33);
			assertThat(service.getApplicant()).isEqualTo("Berit Berg");
			assertThat(service.getCoApplicant()).isEqualTo("Bo Berg");
			assertThat(service.getServicePersonDTOs())
				.extracting(PersonBasedServicePersonDTO::getPersonId, PersonBasedServicePersonDTO::getName)
				.containsExactly(tuple(PARTY_ID, "Berit Berg"));
		});
	}

	/**
	 * CaseworkerResolver orders services by start date and parses the string back to a LocalDate, so the plain
	 * {@code yyyy-MM-dd} this mapper writes has to stay parseable by it.
	 */
	@Test
	void serviceStartDatesAreWrittenAsPlainDates() {
		final var response = new PagedServiceResponse().services(List.of(new CaseService().startDate(LocalDate.of(2026, JUNE, 1))));

		assertThat(IntegratorCaseMapper.toServices(response).getResult())
			.singleElement().extracting(PersonBasedServiceDTO::getStartDate).isEqualTo("2026-06-01");
	}

	@Test
	void noServiceResponseYieldsAnEmptyResult() {
		assertThat(IntegratorCaseMapper.toServices(null).getResult()).isEmpty();
	}

	// ---- Documents -------------------------------------------------------------------------------------------------

	/**
	 * {@code ownerId} is the one field that cannot come across: FamilyCare's is polymorphic and sometimes a personal
	 * identity number, which the integrator will not emit. Asserting it is null keeps that a decision rather than an
	 * oversight someone later "fixes" by putting the party id there.
	 */
	@Test
	void documentsAreMappedWithoutAnOwnerId() {
		final var response = new PagedDocumentResponse()
			.meta(new PagingMetaData().page(1).limit(20).totalPages(1).totalRecords(1L))
			.documents(List.of(new DocumentMetadata()
				.id("doc-1")
				.title("Beslut försörjningsstöd juni")
				.date(LocalDate.of(2026, JUNE, 3))
				.documentType("Beslut")
				.ownerType("Utredning")));

		assertThat(IntegratorCaseMapper.toDocuments(response).getResult()).singleElement().satisfies(document -> {
			assertThat(document.getId()).isEqualTo("doc-1");
			assertThat(document.getTitle()).isEqualTo("Beslut försörjningsstöd juni");
			assertThat(document.getDate()).isEqualTo("2026-06-03");
			assertThat(document.getDocumentType()).isEqualTo("Beslut");
			assertThat(document.getOwnerType()).isEqualTo("Utredning");
			assertThat(document.getOwnerId()).isNull();
		});
	}

	@Test
	void noDocumentResponseYieldsAnEmptyResult() {
		assertThat(IntegratorCaseMapper.toDocuments(null).getResult()).isEmpty();
	}

	// ---- Person, contacts and users ----------------------------------------------------------------------------------

	@Test
	void thePersonIsMappedAndCarriesThePartyIdAsItsPersonId() {
		final var person = new Person()
			.customerNumber(12345)
			.name("Berit Berg")
			.streetAddress("Storgatan 1")
			.careOfAddress("c/o Berg")
			.postalCode("85230")
			.postalAddress("Sundsvall")
			.phoneHome("060-123456")
			.phoneWork("060-654321")
			.phoneMobile("070-1234567")
			.email("berit@example.com")
			.addressProtection(true)
			.secretPhone(false)
			.protectedRegistration(true);

		assertThat(IntegratorCaseMapper.toPerson(person, PARTY_ID)).satisfies(result -> {
			assertThat(result.getPersonId()).isEqualTo(PARTY_ID);
			assertThat(result.getCustomerNumber()).isEqualTo(12345);
			assertThat(result.getName()).isEqualTo("Berit Berg");
			assertThat(result.getStreetAddress()).isEqualTo("Storgatan 1");
			assertThat(result.getCareOfAddress()).isEqualTo("c/o Berg");
			assertThat(result.getPostalCode()).isEqualTo("85230");
			assertThat(result.getPostalAddress()).isEqualTo("Sundsvall");
			assertThat(result.getPhoneHome()).isEqualTo("060-123456");
			assertThat(result.getPhoneWork()).isEqualTo("060-654321");
			assertThat(result.getPhoneMobile()).isEqualTo("070-1234567");
			assertThat(result.getEmail()).isEqualTo("berit@example.com");
			assertThat(result.getAddressProtection()).isTrue();
			assertThat(result.getSecretPhone()).isFalse();
			assertThat(result.getProtectedRegistration()).isTrue();
			assertThat(result.getSource()).isNull();
		});
	}

	@Test
	void noPersonMapsToNull() {
		assertThat(IntegratorCaseMapper.toPerson(null, PARTY_ID)).isNull();
	}

	@Test
	void contactsAreMappedIncludingTheAddress() {
		final var contacts = List.of(
			new Contact()
				.id("c-1")
				.name("Anna Andersson")
				.employmentTitle("Socialsekreterare")
				.organizationName("IFO")
				.phone("060-111111")
				.email("anna@example.com")
				.typeOfContact("Handläggare")
				.address(new Address().visitingAddress("Norrmalmsgatan 4").streetAddress("Box 10").postalCode("85185").postalAddress("Sundsvall")),
			new Contact().id("c-2").name("Bertil Bertilsson"));

		assertThat(IntegratorCaseMapper.toContacts(contacts)).satisfiesExactly(
			first -> {
				assertThat(first.getId()).isEqualTo("c-1");
				assertThat(first.getName()).isEqualTo("Anna Andersson");
				assertThat(first.getEmploymentTitle()).isEqualTo("Socialsekreterare");
				assertThat(first.getOrganizationName()).isEqualTo("IFO");
				assertThat(first.getPhone()).isEqualTo("060-111111");
				assertThat(first.getEmail()).isEqualTo("anna@example.com");
				assertThat(first.getTypeOfContact()).isEqualTo("Handläggare");
				assertThat(first.getAddress()).isNotNull();
				assertThat(first.getAddress().getVisitingAddress()).isEqualTo("Norrmalmsgatan 4");
				assertThat(first.getAddress().getStreetAddress()).isEqualTo("Box 10");
				assertThat(first.getAddress().getPostalCode()).isEqualTo("85185");
				assertThat(first.getAddress().getPostalAddress()).isEqualTo("Sundsvall");
			},
			second -> assertThat(second.getAddress()).isNull());
	}

	@Test
	void noContactsMapToAnEmptyList() {
		assertThat(IntegratorCaseMapper.toContacts(null)).isEmpty();
	}

	@Test
	void caseworkersAreMappedToUsers() {
		final var validFrom = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final var caseworkers = List.of(new Caseworker()
			.id("u-1")
			.hsaId("SE2321000255-AAAA")
			.networkUserId("kaka01")
			.firstName("Karin")
			.lastName("Karlsson")
			.fullName("Karin Karlsson")
			.description("Socialsekreterare")
			.validFrom(validFrom)
			.disabled(false));

		assertThat(IntegratorCaseMapper.toUsers(caseworkers)).singleElement().satisfies(user -> {
			assertThat(user.getId()).isEqualTo("u-1");
			assertThat(user.getHsaId()).isEqualTo("SE2321000255-AAAA");
			assertThat(user.getNetworkUserId()).isEqualTo("kaka01");
			assertThat(user.getFirstName()).isEqualTo("Karin");
			assertThat(user.getLastName()).isEqualTo("Karlsson");
			assertThat(user.getFullName()).isEqualTo("Karin Karlsson");
			assertThat(user.getDescription()).isEqualTo("Socialsekreterare");
			assertThat(user.getValidFrom()).isEqualTo(validFrom.toString());
			assertThat(user.getValidTo()).isNull();
			assertThat(user.getDisabled()).isFalse();
			assertThat(user.getPersonId()).isNull();
			assertThat(user.getVrkId()).isNull();
		});
	}

	@Test
	void noCaseworkersMapToAnEmptyList() {
		assertThat(IntegratorCaseMapper.toUsers(null)).isEmpty();
	}

	/** Every mapped list is a plain list of the target type — no nulls sneak in for absent nested collections. */
	@Test
	void absentNestedCollectionsBecomeEmptyLists() {
		final var decisions = IntegratorCaseMapper.toDecisions(new DecisionsResponse()
			.decisions(List.of(new Decision().source(FAMILY_CARE).familyCareDetails(new FamilyCareDecisionDetails()))));
		final var payments = IntegratorCaseMapper.toPayments(new PagedPaymentResponse().payments(List.of(new Payment())));
		final var services = IntegratorCaseMapper.toServices(new PagedServiceResponse().services(List.of(new CaseService())));

		assertThat(decisions.getResult()).singleElement().satisfies(decision -> assertThat(decision.getDecisionPersonDTOs()).isEmpty());
		assertThat(payments.getResult()).singleElement().satisfies(payment -> assertThat(payment.getPaymentPersonDTOs()).isEmpty());
		assertThat(services.getResult()).singleElement().satisfies(service -> assertThat(service.getServicePersonDTOs()).isEmpty());
	}
}
