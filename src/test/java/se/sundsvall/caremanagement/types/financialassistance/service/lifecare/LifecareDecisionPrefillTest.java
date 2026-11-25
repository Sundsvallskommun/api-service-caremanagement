package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareDecisionPrefillTest {

	private static final LocalDate FIRST = LocalDate.of(2026, Month.SEPTEMBER, 1);
	private static final LocalDate LAST = LocalDate.of(2026, Month.SEPTEMBER, 30);
	private static final LifecareDecisionPrefill PREFILL = new LifecareDecisionPrefill(25, new BigDecimal("2068"), FIRST, LAST);

	@Test
	void fillsInWhatTheCaseworkerLeftOut() {
		final var request = new LifecareDecisionSaveRequest(153, null, null, null, null, 3, "<p>Beslut</p>", true, null);

		assertThat(PREFILL.fillIn(request)).isEqualTo(new LifecareDecisionSaveRequest(153, null, FIRST, LAST, new BigDecimal("2068"), 3, "<p>Beslut</p>", true, null));
	}

	@Test
	void keepsWhatTheCaseworkerGave() {
		final var request = new LifecareDecisionSaveRequest(153, LAST, LocalDate.of(2026, Month.SEPTEMBER, 15), LAST, new BigDecimal("2000"), 3, null, false, null);

		assertThat(PREFILL.fillIn(request)).isEqualTo(request);
	}

	@Test
	void takesThePeriodOnlyWhenTheCaseworkerGaveNeitherEnd() {
		final var request = new LifecareDecisionSaveRequest(153, null, LocalDate.of(2026, Month.SEPTEMBER, 15), null, null, 3, null, null, null);

		final var filled = PREFILL.fillIn(request);

		assertThat(filled.periodFrom()).isEqualTo(LocalDate.of(2026, Month.SEPTEMBER, 15));
		assertThat(filled.periodTo()).isNull();
		assertThat(filled.amount()).isEqualByComparingTo("2068");
	}
}
