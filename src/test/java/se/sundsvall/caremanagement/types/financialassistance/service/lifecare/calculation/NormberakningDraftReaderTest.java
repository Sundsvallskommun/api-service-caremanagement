package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NormberakningDraftReaderTest {

	private static final LifecareErrand ERRAND = new LifecareErrand("2281", "FINANCIAL_ASSISTANCE", "e1", 1, null, null, null, 2026, 9);

	@Mock
	private FinancialAssistanceCalculationService calculationService;
	@Mock
	private CitizenService citizenService;

	@InjectMocks
	private NormberakningDraftReader reader;

	@Test
	void readsTheDraftWithThePersonnummerItCanResolve() {
		final var draft = CalculationDraft.create().withPersons(List.of(
			NormPersonRow.create().withPartyId("p1"), NormPersonRow.create().withPartyId("p2"), NormPersonRow.create().withPartyId("p3"), NormPersonRow.create()));
		when(calculationService.getDraft("2281", "FINANCIAL_ASSISTANCE", "e1")).thenReturn(draft);
		when(citizenService.getPersonalNumber("2281", "p1")).thenReturn(Optional.of("19880209T050"));
		when(citizenService.getPersonalNumber("2281", "p2")).thenReturn(Optional.empty());
		when(citizenService.getPersonalNumber("2281", "p3")).thenThrow(new IllegalStateException("citizen down"));

		final var read = reader.read(ERRAND);

		assertThat(read.draft()).isSameAs(draft);
		assertThat(read.personalNumbers()).containsExactly(entry("p1", "19880209T050"));
		assertThat(read.persons()).extracting(CalculationDraftFill.DraftPerson::personalNumber).containsExactly("19880209T050", null, null, null);
	}

	@Test
	void readsADraftWithoutPersons() {
		when(calculationService.getDraft("2281", "FINANCIAL_ASSISTANCE", "e1")).thenReturn(CalculationDraft.create().withPersons(null));

		final var read = reader.read(ERRAND);

		assertThat(read.personalNumbers()).isEmpty();
		assertThat(read.persons()).isEmpty();
	}

	@Test
	void readsTheDraftsPeriodStartBestEffort() {
		when(calculationService.getDraft("2281", "FINANCIAL_ASSISTANCE", "e1"))
			.thenReturn(CalculationDraft.create().withCalculationFromDate(LocalDate.of(2026, 10, 1)))
			.thenReturn(CalculationDraft.create())
			.thenThrow(Problem.valueOf(NOT_FOUND, "No draft"));

		assertThat(reader.periodStart(ERRAND)).contains("2026-10-01");
		assertThat(reader.periodStart(ERRAND)).isEmpty();
		assertThat(reader.periodStart(ERRAND)).isEmpty();
	}
}
