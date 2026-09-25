package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareDecisionModelsTest {

	@Test
	void decisionView() {
		final var view = new LifecareDecisionView(98, 153, "BIFALL", "2026-09-23", "2026-09-01", "2026-09-30", BigDecimal.TEN, 19, "orsak", "<p>x</p>", true,
			"Test Handläggare");

		assertThat(view).hasNoNullFieldsOrProperties();
		assertThat(view.locked()).isTrue();
		assertThat(view).isEqualTo(new LifecareDecisionView(98, 153, "BIFALL", "2026-09-23", "2026-09-01", "2026-09-30", BigDecimal.TEN, 19, "orsak",
			"<p>x</p>", true, "Test Handläggare"));
	}

	@Test
	void saveRequest() {
		final var request = new LifecareDecisionSaveRequest(153, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
			BigDecimal.TEN, 19, "<p>x</p>", false);

		assertThat(request).hasNoNullFieldsOrProperties();
		assertThat(request.decisionCode()).isEqualTo(153);
		assertThat(request.writeProtect()).isFalse();
	}

	@Test
	void typeReasonStatusAndRegistration() {
		assertThat(new LifecareDecisionType(153, "bifall", "BIFALL", true, false)).hasNoNullFieldsOrProperties();
		assertThat(new LifecareDecisionReason(19, "orsak", "rubrik")).hasNoNullFieldsOrProperties();
		assertThat(new LifecareSectionStatus(true, false, true).paymentRegistered()).isTrue();
		assertThat(new LifecareDecisionRegistration("decision-1", "REGISTERED", "98", "detail")).hasNoNullFieldsOrProperties();
	}
}
