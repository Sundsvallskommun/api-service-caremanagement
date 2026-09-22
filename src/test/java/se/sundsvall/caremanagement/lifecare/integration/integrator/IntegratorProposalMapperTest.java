package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.AktualisationsAttachmentSenderTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsSpecifyTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsWorkingStatusDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationHouseholdMemberDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpenseTypeDTO;
import generated.se.sundsvall.lifecareintegrator.ActualisationProposal;
import generated.se.sundsvall.lifecareintegrator.ActualisationReference;
import generated.se.sundsvall.lifecareintegrator.ActualisationType;
import generated.se.sundsvall.lifecareintegrator.AttachmentType;
import generated.se.sundsvall.lifecareintegrator.CalculationProposal;
import generated.se.sundsvall.lifecareintegrator.HouseholdMember;
import generated.se.sundsvall.lifecareintegrator.Lookup;
import generated.se.sundsvall.lifecareintegrator.Norm;
import generated.se.sundsvall.lifecareintegrator.Organization;
import generated.se.sundsvall.lifecareintegrator.ProposalCase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IntegratorProposalMapperTest {

	private static final String PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";

	@Test
	void theCalculationProposalIsMapped() {
		final var proposal = new CalculationProposal()
			.investigations(List.of(new ProposalCase().id(11).type(1).name("Utredning EB").startDate(LocalDate.of(2026, JANUARY, 15))))
			.services(List.of(new ProposalCase().id(42).type(2).name("Ekonomiskt bistånd").startDate(LocalDate.of(2026, JANUARY, 20))))
			.norms(List.of(new Norm().id(7).name("Riksnorm 2026").validFrom(LocalDate.of(2026, JANUARY, 1)).validTo(LocalDate.of(2026, 12, 31))))
			.householdMembers(List.of(new HouseholdMember().partyId(PARTY_ID).name("Berit Berg").childFromOtherHousehold(false)))
			.incomeTypes(List.of(new Lookup().id(1).name("Lön efter skatt")))
			.expenseTypes(List.of(new Lookup().id(2).name("Hyra")))
			.specialExpenseTypes(List.of(new Lookup().id(3).name("Tandvård")))
			.actualisationMandatory(true)
			.numberOfFamilyMembersNotInHousehold(1)
			.actualisations(List.of(new ActualisationReference().id(88).type("Ansökan").date(LocalDate.of(2026, JUNE, 12))));

		final var result = IntegratorProposalMapper.toCalculationProposal(proposal);

		assertThat(result.getInvestigations()).singleElement().satisfies(investigation -> {
			assertThat(investigation.getId()).isEqualTo(11);
			assertThat(investigation.getType()).isEqualTo(1);
			assertThat(investigation.getName()).isEqualTo("Utredning EB");
			assertThat(investigation.getStartDate()).isEqualTo("2026-01-15");
		});
		assertThat(result.getServices()).singleElement().satisfies(service -> {
			assertThat(service.getId()).isEqualTo(42);
			assertThat(service.getStartDate()).isEqualTo("2026-01-20");
		});
		assertThat(result.getNorms()).singleElement().satisfies(norm -> {
			assertThat(norm.getId()).isEqualTo(7);
			assertThat(norm.getName()).isEqualTo("Riksnorm 2026");
			assertThat(norm.getFromDate()).isEqualTo("2026-01-01");
			assertThat(norm.getToDate()).isEqualTo("2026-12-31");
		});
		assertThat(result.getHouseholdMembers())
			.extracting(PersonBasedCalculationHouseholdMemberDTO::getPersonId, PersonBasedCalculationHouseholdMemberDTO::getName,
				PersonBasedCalculationHouseholdMemberDTO::getChildFromOtherHousehold)
			.containsExactly(tuple(PARTY_ID, "Berit Berg", false));
		assertThat(result.getCalculationIncomeTypes())
			.extracting(PersonBasedCalculationCalculationIncomeTypeDTO::getId, PersonBasedCalculationCalculationIncomeTypeDTO::getName)
			.containsExactly(tuple(1, "Lön efter skatt"));
		assertThat(result.getCalculationExpenseTypes())
			.extracting(PersonBasedCalculationExpenseTypeDTO::getId, PersonBasedCalculationExpenseTypeDTO::getName)
			.containsExactly(tuple(2, "Hyra"));
		assertThat(result.getCalculationSpecialExpenseTypes())
			.extracting(PersonBasedCalculationSpecialExpenseTypeDTO::getId, PersonBasedCalculationSpecialExpenseTypeDTO::getName)
			.containsExactly(tuple(3, "Tandvård"));
		assertThat(result.getAktualiseringMandatory()).isTrue();
		assertThat(result.getNumberOfFamilyMembersNotInHousehold()).isEqualTo(1);
		assertThat(result.getAktualiserings()).singleElement().satisfies(actualisation -> {
			assertThat(actualisation.getId()).isEqualTo(88);
			assertThat(actualisation.getType()).isEqualTo("Ansökan");
			assertThat(actualisation.getDate()).isEqualTo("2026-06-12");
		});
	}

	/**
	 * A proposal is what the handläggare's form offers. An absent code list means "no choices", so it has to arrive as
	 * an empty list; a null there would be a NullPointerException in the assemblers rather than an empty dropdown.
	 */
	@Test
	void anEmptyCalculationProposalMapsToEmptyListsNotNulls() {
		final var result = IntegratorProposalMapper.toCalculationProposal(new CalculationProposal());

		assertThat(result.getInvestigations()).isEmpty();
		assertThat(result.getServices()).isEmpty();
		assertThat(result.getNorms()).isEmpty();
		assertThat(result.getHouseholdMembers()).isEmpty();
		assertThat(result.getCalculationIncomeTypes()).isEmpty();
		assertThat(result.getCalculationExpenseTypes()).isEmpty();
		assertThat(result.getCalculationSpecialExpenseTypes()).isEmpty();
		assertThat(result.getAktualiserings()).isEmpty();
		assertThat(result.getAktualiseringMandatory()).isNull();
	}

	@Test
	void noCalculationProposalMapsToNull() {
		assertThat(IntegratorProposalMapper.toCalculationProposal(null)).isNull();
	}

	@Test
	void theActualisationProposalIsMapped() {
		final var proposal = new ActualisationProposal()
			.actualisationTypes(List.of(new ActualisationType()
				.id(1)
				.name("Ansökan")
				.specifyTypeMandatory(true)
				.workingStatus(false)
				.reasons(List.of(new Lookup().id(10).name("Hyra")))
				.fromWho(List.of(new Lookup().id(20).name("Den enskilde")))
				.investigationTypes(List.of(new Lookup().id(30).name("Utredning EB")))
				.serviceTypes(List.of(new Lookup().id(40).name("Ekonomiskt bistånd")))))
			.specifyTypes(List.of(new Lookup().id(2).name("Återansökan")))
			.workingStatus(List.of(new Lookup().id(3).name("Arbetslös")))
			.organizations(List.of(new Organization().id(4).unitId("IFO-EB").name("IFO Ekonomiskt bistånd")))
			.investigations(List.of(new ProposalCase().id(11).type(1).name("Utredning EB")
				.startDate(LocalDate.of(2026, JANUARY, 15)).organisationId(4).organisationUnitId("IFO-EB").caseworkerId("kaka01")))
			.services(List.of(new ProposalCase().id(42).type(2).name("Ekonomiskt bistånd")
				.startDate(LocalDate.of(2026, JANUARY, 20)).organisationId(4).organisationUnitId("IFO-EB").caseworkerId("kaka01")))
			.attachmentTypes(List.of(new AttachmentType().id(5).name("Hyresavi").senderTypes(List.of(new Lookup().id(50).name("Den enskilde")))));

		final var result = IntegratorProposalMapper.toActualisationProposal(proposal);

		assertThat(result.getActualisationTypes()).singleElement().satisfies(type -> {
			assertThat(type.getId()).isEqualTo(1);
			assertThat(type.getName()).isEqualTo("Ansökan");
			assertThat(type.getSpecifyTypeMandatory()).isTrue();
			assertThat(type.getWorkingStatus()).isFalse();
			assertThat(type.getReasons()).extracting(PersonBasedAktualiseringsReasonDTO::getId, PersonBasedAktualiseringsReasonDTO::getName)
				.containsExactly(tuple(10, "Hyra"));
			assertThat(type.getFromWho()).extracting(PersonBasedAktualiseringsFromWhoDTO::getId, PersonBasedAktualiseringsFromWhoDTO::getName)
				.containsExactly(tuple(20, "Den enskilde"));
			assertThat(type.getInvestigationTypes())
				.extracting(PersonBasedAktualiseringsInvestigationTypeDTO::getId, PersonBasedAktualiseringsInvestigationTypeDTO::getName)
				.containsExactly(tuple(30, "Utredning EB"));
			assertThat(type.getServiceTypes())
				.extracting(PersonBasedAktualiseringsServiceTypeDTO::getId, PersonBasedAktualiseringsServiceTypeDTO::getName)
				.containsExactly(tuple(40, "Ekonomiskt bistånd"));
		});
		assertThat(result.getSpecifyTypes())
			.extracting(PersonBasedAktualiseringsSpecifyTypeDTO::getId, PersonBasedAktualiseringsSpecifyTypeDTO::getName)
			.containsExactly(tuple(2, "Återansökan"));
		assertThat(result.getWorkingStatus())
			.extracting(PersonBasedAktualiseringsWorkingStatusDTO::getId, PersonBasedAktualiseringsWorkingStatusDTO::getName)
			.containsExactly(tuple(3, "Arbetslös"));
		assertThat(result.getOrganizations()).singleElement().satisfies(organization -> {
			assertThat(organization.getId()).isEqualTo(4);
			assertThat(organization.getUnitId()).isEqualTo("IFO-EB");
			assertThat(organization.getName()).isEqualTo("IFO Ekonomiskt bistånd");
		});

		// The actualisation proposal's cases carry the organisation and caseworker the calculation proposal's do not.
		assertThat(result.getInvestigations()).singleElement().satisfies(investigation -> {
			assertThat(investigation.getId()).isEqualTo(11);
			assertThat(investigation.getStartDate()).isEqualTo("2026-01-15");
			assertThat(investigation.getOrganisationId()).isEqualTo(4);
			assertThat(investigation.getOrganisationUnitId()).isEqualTo("IFO-EB");
			assertThat(investigation.getCaseworkerId()).isEqualTo("kaka01");
		});
		assertThat(result.getServices()).singleElement().satisfies(service -> {
			assertThat(service.getId()).isEqualTo(42);
			assertThat(service.getCaseworkerId()).isEqualTo("kaka01");
		});
		assertThat(result.getAttachmentTypes()).singleElement().satisfies(attachmentType -> {
			assertThat(attachmentType.getId()).isEqualTo(5);
			assertThat(attachmentType.getName()).isEqualTo("Hyresavi");
			assertThat(attachmentType.getSenderTypes())
				.extracting(AktualisationsAttachmentSenderTypeDTO::getId, AktualisationsAttachmentSenderTypeDTO::getName)
				.containsExactly(tuple(50, "Den enskilde"));
		});
	}

	@Test
	void anEmptyActualisationProposalMapsToEmptyListsNotNulls() {
		final var result = IntegratorProposalMapper.toActualisationProposal(new ActualisationProposal());

		assertThat(result.getActualisationTypes()).isEmpty();
		assertThat(result.getSpecifyTypes()).isEmpty();
		assertThat(result.getWorkingStatus()).isEmpty();
		assertThat(result.getOrganizations()).isEmpty();
		assertThat(result.getInvestigations()).isEmpty();
		assertThat(result.getServices()).isEmpty();
		assertThat(result.getAttachmentTypes()).isEmpty();
	}

	@Test
	void noActualisationProposalMapsToNull() {
		assertThat(IntegratorProposalMapper.toActualisationProposal(null)).isNull();
	}
}
