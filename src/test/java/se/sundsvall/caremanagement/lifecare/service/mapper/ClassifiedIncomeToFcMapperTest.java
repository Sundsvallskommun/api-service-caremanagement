package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

class ClassifiedIncomeToFcMapperTest {

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("Bostadsbidrag"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("Dagersättning"));
	}

	private static ClassifiedIncome classified(final String benefit, final String calculation, final String atgard, final String amount, final ApplicantRole role) {
		return new ClassifiedIncome(new SsbtekIncome(benefit, null, "Månad", new BigDecimal(amount), LocalDate.of(2026, MAY, 15), role), atgard, calculation, false, "note");
	}

	@Test
	void resolvesCategoryToFcTypeIdAndMergesByRole() {
		final var rows = ClassifiedIncomeToFcMapper.toCalculationIncomes(List.of(
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT),
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "200", CO_APPLICANT),
			classified("Dagersättning", "Dagersättning", "TA_MED", "5000", APPLICANT)),
			proposal());

		assertThat(rows).hasSize(2);
		final var bostadsbidrag = rows.stream().filter(row -> row.getId() == 20).findFirst().orElseThrow();
		assertThat(bostadsbidrag.getApplicantAmount()).isEqualTo(1850.0);
		assertThat(bostadsbidrag.getCoApplicantAmount()).isEqualTo(200.0);
		final var dagersattning = rows.stream().filter(row -> row.getId() == 30).findFirst().orElseThrow();
		assertThat(dagersattning.getApplicantAmount()).isEqualTo(5000.0);
		assertThat(dagersattning.getCoApplicantAmount()).isNull();
	}

	@Test
	void skipsNonTransferableAndUnknownCategory() {
		final var rows = ClassifiedIncomeToFcMapper.toCalculationIncomes(List.of(
			classified("Handikappersättning", "-", "EJ_TA_MED", "100", APPLICANT),
			classified("Underhållsstöd", "-", "EJ_PA_LISTAN", "100", APPLICANT),
			classified("Okänd", "Okänd kategori", "TA_MED", "100", APPLICANT),
			classified("Tom", "-", "TA_MED", "100", APPLICANT)),
			proposal());

		assertThat(rows).isEmpty();
	}

	@Test
	void nullClassifiedYieldsEmpty() {
		assertThat(ClassifiedIncomeToFcMapper.toCalculationIncomes(null, proposal())).isEmpty();
	}

	@Test
	void toIncomeLinesDropsNullRoleInsteadOfFailing() {
		// A classified income with no role must be skipped (it can't be folded per-recipient) rather than NPE in the grouping
		// key.
		final var lines = ClassifiedIncomeToFcMapper.toIncomeLines(List.of(
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT),
			classified("Dagersättning", "Dagersättning", "TA_MED", "5000", null)),
			proposal());

		assertThat(lines).extracting(FcIncomeLine::typeId).containsExactly(20);
	}
}
