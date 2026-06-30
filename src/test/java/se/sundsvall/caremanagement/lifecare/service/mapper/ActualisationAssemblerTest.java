package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsOrganizationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsSpecifyTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsWorkingStatusDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;

class ActualisationAssemblerTest {

	private static final String PERSON_ID = "198001012389";
	private static final LocalDate DATE = LocalDate.of(2026, JUNE, 1);

	@Test
	void assemblesPersonAndDateWithoutProposal() {
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, null);

		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		assertThat(body.getDate()).isEqualTo("2026-06-01");
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
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, "9001");

		assertThat(body.getCaseworkerId()).isEqualTo("9001");
	}

	@Test
	void leavesCaseworkerIdUnsetWhenBlank() {
		final var body = ActualisationAssembler.assemble(PERSON_ID, null, DATE, "   ");

		assertThat(body.getCaseworkerId()).isNull();
	}

	@Test
	void picksFirstTypeReasonFromWhoOrganisationServiceAndInvestigation() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO()
				.id(1)
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(11))
				.addReasonsItem(new PersonBasedAktualiseringsReasonDTO().id(12))
				.addFromWhoItem(new PersonBasedAktualiseringsFromWhoDTO().id(21)))
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(2))
			.addOrganizationsItem(new PersonBasedAktualiseringsOrganizationDTO().id(31).unitId("unit-A"))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(41))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(42))
			.addInvestigationsItem(new PersonBasedAktualiseringsInvestigationDTO().id(51));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null);

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
			.addWorkingStatusItem(new PersonBasedAktualiseringsWorkingStatusDTO().id(71));

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null);

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

		final var body = ActualisationAssembler.assemble(PERSON_ID, proposal, DATE, null);

		assertThat(body.getSpecifies()).isNull();
		assertThat(body.getWorkingStatus()).isNull();
	}
}
