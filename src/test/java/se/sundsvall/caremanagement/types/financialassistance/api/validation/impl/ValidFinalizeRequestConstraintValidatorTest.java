package se.sundsvall.caremanagement.types.financialassistance.api.validation.impl;

import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.ERROR_AMOUNT_REQUIRED;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.ERROR_PAYMENTS_FORBIDDEN;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.ERROR_PAYMENTS_REQUIRED;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.ERROR_PERIOD_ORDER;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.NODE_AMOUNT;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.NODE_PAYMENTS;
import static se.sundsvall.caremanagement.types.financialassistance.api.validation.impl.ValidFinalizeRequestConstraintValidator.NODE_PERIOD_TO;

@ExtendWith(MockitoExtension.class)
class ValidFinalizeRequestConstraintValidatorTest {

	private static final FinalizePayment PAYMENT = FinalizePayment.create().withAmount(new BigDecimal("7900")).withConcernedMonth("2026-06");

	@Mock(answer = RETURNS_DEEP_STUBS)
	private ConstraintValidatorContext contextMock;

	private final ValidFinalizeRequestConstraintValidator validator = new ValidFinalizeRequestConstraintValidator();

	@Test
	void grantingOutcomeWithAmountAndPaymentIsValid() {
		final var request = FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("BIFALL").withAmount(new BigDecimal("7900"))
				.withPeriodFrom(LocalDate.of(2026, 6, 1)).withPeriodTo(LocalDate.of(2026, 6, 30)))
			.withPayments(List.of(PAYMENT));

		assertThat(validator.isValid(request, contextMock)).isTrue();
		verifyNoInteractions(contextMock);
	}

	@Test
	void nonGrantingOutcomeWithoutPaymentsIsValid() {
		final var request = FinalizeRequest.create().withDecision(FinalizeDecision.create().withOutcome("AVSLAG"));

		assertThat(validator.isValid(request, contextMock)).isTrue();
		verifyNoInteractions(contextMock);
	}

	@Test
	void nullRequestOrMissingOutcomeIsLeftToTheFieldConstraints() {
		assertThat(validator.isValid(null, contextMock)).isTrue();
		assertThat(validator.isValid(FinalizeRequest.create(), contextMock)).isTrue();
		assertThat(validator.isValid(FinalizeRequest.create().withDecision(FinalizeDecision.create()), contextMock)).isTrue();
		// an unrecognised outcome is @OneOf's violation — no payments/amount rule is applied on top of it
		assertThat(validator.isValid(FinalizeRequest.create().withDecision(FinalizeDecision.create().withOutcome("BEVILJAD")).withPayments(List.of(PAYMENT)), contextMock)).isTrue();
		verifyNoInteractions(contextMock);
	}

	@Test
	void grantingOutcomeReportsMissingAmountAndPaymentsTogether() {
		final var request = FinalizeRequest.create().withDecision(FinalizeDecision.create().withOutcome("DELAVSLAG"));

		assertThat(validator.isValid(request, contextMock)).isFalse();

		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_AMOUNT_REQUIRED).addPropertyNode(NODE_AMOUNT)).addConstraintViolation();
		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_PAYMENTS_REQUIRED).addPropertyNode(NODE_PAYMENTS)).addConstraintViolation();
		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_PERIOD_ORDER).addPropertyNode(NODE_PERIOD_TO), never()).addConstraintViolation();
	}

	@Test
	void nonGrantingOutcomeRejectsPayments() {
		final var request = FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("AVSLAG"))
			.withPayments(List.of(PAYMENT));

		assertThat(validator.isValid(request, contextMock)).isFalse();

		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_PAYMENTS_FORBIDDEN).addPropertyNode(NODE_PAYMENTS)).addConstraintViolation();
		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_AMOUNT_REQUIRED).addPropertyNode(NODE_AMOUNT), never()).addConstraintViolation();
	}

	@Test
	void periodEndingBeforeStartIsRejected() {
		final var request = FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("BIFALL").withAmount(new BigDecimal("7900"))
				.withPeriodFrom(LocalDate.of(2026, 6, 30)).withPeriodTo(LocalDate.of(2026, 6, 1)))
			.withPayments(List.of(PAYMENT));

		assertThat(validator.isValid(request, contextMock)).isFalse();

		verify(contextMock).disableDefaultConstraintViolation();
		verify(contextMock.buildConstraintViolationWithTemplate(ERROR_PERIOD_ORDER).addPropertyNode(NODE_PERIOD_TO)).addConstraintViolation();
	}
}
