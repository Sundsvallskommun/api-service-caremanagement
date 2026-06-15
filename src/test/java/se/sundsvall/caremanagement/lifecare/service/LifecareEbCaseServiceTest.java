package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionPersonDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareEbCaseServiceTest {

	private static final String APPLICANT = "198001012389";
	private static final LocalDate REFERENCE = LocalDate.of(2026, 6, 15);

	@Mock
	private LifecareFcIntegration integrationMock;

	private LifecareEbCaseService service() {
		return new LifecareEbCaseService(integrationMock, 13);
	}

	@Test
	void openCaseWithDecisionForReferenceMonthAndCoApplicant() {
		final var decision = new PersonBasedDecisionDTO()
			.fromDate("2026-06-01")
			.toDate("2026-06-30")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId("198202022397").isCoApplicant(true))
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId(APPLICANT).isCoApplicant(false));

		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(new PersonBasedAktualiseringDTO().status("OPEN")));
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(decision));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO()));

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasOpenCase()).isTrue();
		assertThat(summary.hasDecisionForReferenceMonth()).isTrue();
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 6));
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.coApplicantPersonIds()).containsExactly("198202022397");
	}

	@Test
	void picksMostRecentDecisionForConstellationAndPeriod() {
		final var older = new PersonBasedDecisionDTO().fromDate("2026-03-01").toDate("2026-03-31")
			.coApplicant("197001010000");
		final var newer = new PersonBasedDecisionDTO().fromDate("2026-05-01").toDate("2026-05-31");

		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().result(List.of(older, newer)));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasOpenCase()).isTrue(); // a decision exists even though no aktualisering
		assertThat(summary.hasDecisionForReferenceMonth()).isFalse(); // neither covers June
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 5));
		assertThat(summary.coApplicantPersonIds()).isEmpty(); // newest decision has no co-applicant
	}

	@Test
	void noFootprintYieldsEmptySummary() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasOpenCase()).isFalse();
		assertThat(summary.hasDecisionForReferenceMonth()).isFalse();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.hasCalculation()).isFalse();
		assertThat(summary.coApplicantPersonIds()).isEmpty();
	}

	@Test
	void nullCompositesAreToleratedAsEmpty() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasOpenCase()).isFalse();
		assertThat(summary.hasCalculation()).isFalse();
	}

	@Test
	void decisionCoveringWithOnlyFromDate() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().fromDate("2026-06-01")));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasDecisionForReferenceMonth()).isTrue();
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 6));
	}

	@Test
	void decisionCoveringWithOnlyToDate() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().toDate("2026-06-30")));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasDecisionForReferenceMonth()).isTrue();
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 6));
	}

	@Test
	void decisionWithoutDatesHasOpenCaseButNoPeriod() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(new PersonBasedDecisionDTO()));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasOpenCase()).isTrue();
		assertThat(summary.hasDecisionForReferenceMonth()).isFalse();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.coApplicantPersonIds()).isEmpty();
	}

	@Test
	void toYearMonthHandlesVariousFormats() {
		assertThat(LifecareEbCaseService.toYearMonth("2026-06")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15T08:30:00")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15 08:30:00")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth(null)).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("  ")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("2026")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("garbage")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("2026-13")).isEmpty();
	}
}
