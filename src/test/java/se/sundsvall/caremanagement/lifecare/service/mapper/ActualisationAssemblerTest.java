package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsOrganizationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsSpecifyTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsWorkingStatusDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.ActualisationProperties;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;

class ActualisationAssemblerTest {

	private static final String PERSON_ID = "198001012389";
	private static final LocalDate DATE = LocalDate.of(2026, JUNE, 1);

	/** The names verksamheten gave for an EB återansökan. */
	private static final ActualisationProperties NAMES = new ActualisationProperties(
		"Ek Återansökan Digital Ekonomiskt bistånd", "Den enskilde", "Ekonomiskt bistånd", "Ekonomiskt bistånd",
		"EK Nyansökan Digital Ekonomiskt bistånd");

	@Test
	void assemblesPersonAndDateWithoutProposal() {
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, null, NAMES).body();

		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		assertThat(body.getDate()).isEqualTo("2026-06-01T00:00:00");
		// No proposal → no codes resolved.
		assertThat(body.getType()).isNull();
		assertThat(body.getReason()).isNull();
		assertThat(body.getFromWho()).isNull();
		assertThat(body.getOrganisationId()).isNull();
		assertThat(body.getOrganisationUnitId()).isNull();
		assertThat(body.getServiceId()).isNull();
		assertThat(body.getInvestigationId()).isNull();
		assertThat(body.getSpecifies()).isNull();
		assertThat(body.getWorkingStatus()).isNull();
		assertThat(body.getCaseworkerId()).isNull();
	}

	@Test
	void setsCaseworkerIdWhenProvided() {
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, "9001", NAMES).body();

		assertThat(body.getCaseworkerId()).isEqualTo("9001");
	}

	@Test
	void leavesCaseworkerIdUnsetWhenBlank() {
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, "   ", NAMES).body();

		assertThat(body.getCaseworkerId()).isNull();
	}

	@Test
	void fallsBackToTheFirstOfferedValueWhenNoCatalogueEntryMatchesTheConfiguredName() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(11))
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(12))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(21))
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(7))
				.addInvestigationTypesItem(new PersonBasedAktualiseringsInvestigationTypeDTO().id(8)))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(2))
			.addOrganizationsItem(new PersonBasedAktualiseringsOrganizationDTO().id(31).unitId("unit-A"))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(41).type(7))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(42).type(7))
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(51).type(8));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getType()).isEqualTo(1);
		assertThat(body.getReason()).isEqualTo(11);
		assertThat(body.getFromWho()).isEqualTo(21);
		assertThat(body.getOrganisationId()).isEqualTo(31);
		assertThat(body.getOrganisationUnitId()).isEqualTo("unit-A");
		assertThat(body.getServiceId()).isEqualTo(41);
		assertThat(body.getInvestigationId()).isEqualTo(51);
		// Type does not require a specify-type or working-status → neither set.
		assertThat(body.getSpecifies()).isNull();
		assertThat(body.getWorkingStatus()).isNull();
	}

	@Test
	void setsSpecifyAndWorkingStatusWhenTypeRequiresThem() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.specifyTypeMandatory(true)
				.workingStatus(true))
			.addSpecifyTypesItem(new PersonBasedAktualiseringsSpecifyTypeDTO().id(61))
			.addWorkingStatusItem(new PersonBasedAktualiseringsWorkingStatusDTO().id(71).name("Ny"));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getSpecifies()).isEqualTo(61);
		assertThat(body.getWorkingStatus()).isEqualTo(71);
	}

	@Test
	void omitsSpecifyAndWorkingStatusWhenTypeDoesNotRequireThem() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.specifyTypeMandatory(false)
				.workingStatus(false))
			.addSpecifyTypesItem(new PersonBasedAktualiseringsSpecifyTypeDTO().id(61))
			.addWorkingStatusItem(new PersonBasedAktualiseringsWorkingStatusDTO().id(71));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getSpecifies()).isNull();
		assertThat(body.getWorkingStatus()).isNull();
	}

	/**
	 * The regression that produced a 400 from FamilyCare on every EB återansökan: the person's open investigations are
	 * everything they have anywhere in socialtjänsten, so the first offered one was a "Vux Utredning 14 kap 2 § SoL"
	 * hung on an EB intake. Only what the chosen type accepts may be linked.
	 */
	@Test
	void linksOnlyTheServiceAndInvestigationTheChosenTypeAccepts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27))
				.addInvestigationTypesItem(new PersonBasedAktualiseringsInvestigationTypeDTO().id(40)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(5).type(3))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(2).type(27))
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(12).type(18))
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(99).type(40));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getServiceId()).isEqualTo(2);
		assertThat(body.getInvestigationId()).isEqualTo(99);
	}

	@Test
	void linkedServiceIdIsTheServiceTheConfiguredTypeAccepts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(5).type(3))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(2).type(27));

		assertThat(ActualisationAssembler.linkedServiceId(proposal, NAMES)).contains(2);
	}

	@Test
	void linkedServiceIdIsEmptyWhenNoOpenServiceHasAnAcceptedType() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(5).type(3));

		assertThat(ActualisationAssembler.linkedServiceId(proposal, NAMES)).isEmpty();
	}

	@Test
	void linkedServiceIdIsEmptyWithoutProposal() {
		assertThat(ActualisationAssembler.linkedServiceId(null, NAMES)).isEmpty();
	}

	/** An empty accepted-type list is a statement — the type takes no such link — not a reason to pick any of them. */
	@Test
	void sendsNoLinkWhenTheTypeAcceptsNoSuchType() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(29))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(2).type(27))
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(12).type(18));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getServiceId()).isNull();
		assertThat(body.getInvestigationId()).isNull();
	}

	/**
	 * FamilyCare's working-status catalogue leads with an unnamed {@code {id: 0}} placeholder meaning "no status",
	 * which is no answer for a type that marks the field mandatory.
	 */
	@Test
	void skipsTheUnnamedWorkingStatusPlaceholder() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(1).workingStatus(true))
			.addWorkingStatusItem(new PersonBasedAktualiseringsWorkingStatusDTO().id(0).name(""))
			.addWorkingStatusItem(new PersonBasedAktualiseringsWorkingStatusDTO().id(1).name("Ny"));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES).body();

		assertThat(body.getWorkingStatus()).isEqualTo(1);
	}

	@Test
	void picksTheNamedTypeReasonFromWhoAndOrganisation() {
		// The one that matters: reason and fromWho are looked up inside the CHOSEN type, not in the first type offered.
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1).name("Ek Nyansökan Digital Ekonomiskt bistånd")
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(11).name("Ekonomiskt bistånd"))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(21).name("Den enskilde")))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(2).name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(12).name("Annan orsak"))
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(13).name("Ekonomiskt bistånd"))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(22).name("Anhörig"))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(23).name("Den enskilde")))
			.addOrganizationsItem(new PersonBasedAktualiseringsOrganizationDTO().id(31).unitId("unit-A").name("Vuxenenheten"))
			.addOrganizationsItem(new PersonBasedAktualiseringsOrganizationDTO().id(32).unitId("unit-B").name("Ekonomiskt bistånd"));

		final var selection = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES);

		assertThat(selection.body().getType()).isEqualTo(2);
		assertThat(selection.body().getReason()).isEqualTo(13);
		assertThat(selection.body().getFromWho()).isEqualTo(23);
		assertThat(selection.body().getOrganisationId()).isEqualTo(32);
		assertThat(selection.body().getOrganisationUnitId()).isEqualTo("unit-B");
		assertThat(selection.misses()).isEmpty();
	}

	@Test
	void matchesTheNameIgnoringCaseAndSurroundingSpace() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(1).name("Något annat"))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(2).name("  ek återansökan digital ekonomiskt bistånd "));

		final var selection = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES);

		assertThat(selection.body().getType()).isEqualTo(2);
		assertThat(selection.misses()).isEmpty();
	}

	@Test
	void reportsEveryNameThatWasNotInTheCatalogue() {
		// The fallback keeps the intake working; the misses are what the service logs so a renamed catalogue entry is
		// never a silent guess.
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1).name("Ek Nyansökan Digital Ekonomiskt bistånd")
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(11).name("Annan orsak"))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(21).name("Anhörig")))
			.addOrganizationsItem(new PersonBasedAktualiseringsOrganizationDTO().id(31).unitId("unit-A").name("Vuxenenheten"));

		final var selection = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null, NAMES);

		assertThat(selection.body().getType()).isEqualTo(1);
		assertThat(selection.body().getReason()).isEqualTo(11);
		assertThat(selection.misses()).containsExactlyInAnyOrder(
			"type=Ek Återansökan Digital Ekonomiskt bistånd",
			"reason=Ekonomiskt bistånd",
			"fromWho=Den enskilde",
			"organisation=Ekonomiskt bistånd");
	}

	@Test
	void reportsNoMissWhenTheProposalOffersNothingToMatchAgainst() {
		// An empty catalogue is not a wrong name - there is nothing to warn about, and nothing gets set either.
		final var selection = ActualisationAssembler.assemble(PERSON_ID, new PersonBasedAktualiseringProposalDTO(), DATE, null, NAMES);

		assertThat(selection.body().getType()).isNull();
		assertThat(selection.misses()).isEmpty();
	}
}
