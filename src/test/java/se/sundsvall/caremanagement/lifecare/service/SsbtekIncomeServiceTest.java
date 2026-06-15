package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SsbtekIncomeServiceTest {

	private static final String APPLICANT = "200001012384";
	private static final String CO_APPLICANT = "200102034852";
	private static final YearMonth MAY_2026 = YearMonth.of(2026, 5); // kontroll = April, jämförelse = March

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@InjectMocks
	private SsbtekIncomeService service;

	/** SSBTEK SO arbetslöshetsersättning basis with one payment on the given date. */
	private static Map<String, Map<String, Object>> soBasis(final String netto, final String datum) {
		return Map.of("so", Map.of(
			"ArbetsloshetsersattningLista", Map.of(
				"Arbetsloshetsersattning", Map.of(
					"Utbetalningar", Map.of("NettoEfterSkatt", netto, "Utbetalningsdatum", datum)))));
	}

	private static PersonBasedCalculationProposalDTO proposalWithAkassa() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("A-kassa/Alfa"));
	}

	@Test
	void preparesApplicantIncomeRowsFromKontrollperiod() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposalWithAkassa());

		final var prepared = service.prepareCalculationIncomes(APPLICANT, soBasis("1200", "2026-04-10"), null, MAY_2026);

		assertThat(prepared.calculationIncomes()).singleElement().satisfies(row -> {
			assertThat(row.getId()).isEqualTo(30);                  // Arbetslöshetsersättning -> A-kassa/Alfa -> id 30
			assertThat(row.getApplicantAmount()).isEqualTo(1200.0);
			assertThat(row.getCoApplicantAmount()).isNull();
		});
		assertThat(prepared.unhandledIncomes()).isEmpty();
		assertThat(prepared.changeWarnings()).isEmpty();
		verify(lifecareFcIntegrationMock).getCalculationProposal(APPLICANT);
		verifyNoMoreInteractions(lifecareFcIntegrationMock);
	}

	@Test
	void mergesCoApplicantBasisIntoTheSameRow() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposalWithAkassa());

		final var prepared = service.prepareCalculationIncomes(
			APPLICANT, soBasis("1200", "2026-04-10"), soBasis("800", "2026-04-12"), MAY_2026);

		assertThat(prepared.calculationIncomes()).singleElement().satisfies(row -> {
			assertThat(row.getApplicantAmount()).isEqualTo(1200.0);
			assertThat(row.getCoApplicantAmount()).isEqualTo(800.0);
		});
	}

	@Test
	void flagsAChangeBetweenJamforelseAndKontroll() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposalWithAkassa());

		// March 1000 (jämförelse) vs April 1300 (kontroll) = +30% -> warning; both transfer-considered.
		final Map<String, Map<String, Object>> basis = Map.of("so", Map.of(
			"ArbetsloshetsersattningLista", Map.of(
				"Arbetsloshetsersattning", Map.of(
					"Utbetalningar", List.of(
						Map.of("NettoEfterSkatt", "1000", "Utbetalningsdatum", "2026-03-10"),
						Map.of("NettoEfterSkatt", "1300", "Utbetalningsdatum", "2026-04-10"))))));

		final var prepared = service.prepareCalculationIncomes(APPLICANT, basis, null, MAY_2026);

		assertThat(prepared.changeWarnings()).singleElement().satisfies(warning -> {
			assertThat(warning.forman()).isEqualTo("Arbetslöshetsersättning");
			assertThat(warning.changePercent()).isEqualByComparingTo("30.0");
		});
		// kontrollperiod income transfers (the April one)
		assertThat(prepared.calculationIncomes()).singleElement().satisfies(row -> assertThat(row.getApplicantAmount()).isEqualTo(1300.0));
	}

	@Test
	void reportsUnhandledWhenProposalLacksTheType() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(new PersonBasedCalculationProposalDTO());

		final var prepared = service.prepareCalculationIncomes(APPLICANT, soBasis("1200", "2026-04-10"), null, MAY_2026);

		assertThat(prepared.calculationIncomes()).isEmpty();
		assertThat(prepared.unhandledIncomes()).singleElement()
			.satisfies(unhandled -> assertThat(unhandled.forman()).isEqualTo("Arbetslöshetsersättning"));
	}

	@Test
	void emptyBasisProducesEmptyPreparation() {
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposalWithAkassa());

		final var prepared = service.prepareCalculationIncomes(APPLICANT, Map.of(), null, MAY_2026);

		assertThat(prepared.calculationIncomes()).isEmpty();
		assertThat(prepared.unhandledIncomes()).isEmpty();
		assertThat(prepared.changeWarnings()).isEmpty();
	}
}
