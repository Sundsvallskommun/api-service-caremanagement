package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;

import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

class ApplicationIncomeToFamilyCareMapperTest {

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(11).name("Lön efter skatt"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(12).name("Swish/Insättningar/Överföringar"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(13).name("Övriga inkomster"));
	}

	private static ApplicationIncome income(final String type, final String amount, final LocalDate date, final ApplicantRole role) {
		return new ApplicationIncome(type, new BigDecimal(amount), date, role);
	}

	@Test
	void resolvesApplicationTypeToFcTypeIdByName() {
		final var lines = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(
			income("SALARY", "18500", LocalDate.of(2026, MAY, 25), APPLICANT),
			income("SWISH_DEPOSITS", "300", LocalDate.of(2026, MAY, 10), CO_APPLICANT)),
			proposal());

		assertThat(lines).hasSize(2);
		final var salary = lines.stream().filter(line -> line.typeId() == 11).findFirst().orElseThrow();
		assertThat(salary.typeName()).isEqualTo("Lön efter skatt");
		assertThat(salary.recipient()).isEqualTo("APPLICANT");
		assertThat(salary.amount()).isEqualByComparingTo("18500");
		assertThat(salary.date()).isEqualTo(OffsetDateTime.of(2026, 5, 25, 0, 0, 0, 0, ZoneOffset.UTC));
		assertThat(salary.note()).isEqualTo("Ansökan");

		final var swish = lines.stream().filter(line -> line.typeId() == 12).findFirst().orElseThrow();
		assertThat(swish.recipient()).isEqualTo("CO_APPLICANT");
	}

	@Test
	void foldsTheManyOtherTypesOntoTheSameFcType() {
		final var lines = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(
			income("OTHER_INCOME", "100", null, APPLICANT),
			income("RENT_SHARE_FROM_CHILD", "200", null, APPLICANT),
			income("FINANCIAL_AID_OTHER_MUNICIPALITY", "300", null, APPLICANT)),
			proposal());

		// All three map to "Övriga inkomster" (id 13); the downstream feeder folds them — here we just confirm the id.
		assertThat(lines).hasSize(3).allMatch(line -> line.typeId() == 13);
	}

	@Test
	void skipsUnknownApplicationTypeAndTypeNotOfferedByProposal() {
		final var lines = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(
			income("MADE_UP_TYPE", "100", null, APPLICANT),               // not in the application→FamilyCare table
			income("CHILD_SUPPORT", "100", null, APPLICANT)),             // maps to "Underhållsstöd", absent from this proposal
			proposal());

		assertThat(lines).isEmpty();
	}

	@Test
	void nullIncomeTypeIsSkipped() {
		final var lines = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(
			income(null, "100", null, APPLICANT)),
			proposal());

		assertThat(lines).isEmpty();
	}

	@Test
	void nullInputsYieldEmpty() {
		assertThat(ApplicationIncomeToFamilyCareMapper.toIncomeLines(null, proposal())).isEmpty();
		assertThat(ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(), new PersonBasedCalculationProposalDTO())).isEmpty();
	}
}
