package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistancePaymentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "a3c1f4de-2b6a-4c1e-9d3f-7e8a9b0c1d2e";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String PERSONAL_NUMBER = "199001011234";

	@Mock
	private PaymentStatusService paymentStatusServiceMock;

	@Mock
	private PaymentService paymentServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private FinancialAssistancePaymentService service;

	private static PaymentStatusRequest errandRequest() {
		return PaymentStatusRequest.create().withErrandId(ERRAND_ID).withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");
	}

	private static Payment decided(final String status, final String lifecareId, final LocalDate paymentDate) {
		return Payment.create().withSource("CASEWORKER").withStatus(status).withLifecareId(lifecareId).withPaymentDate(paymentDate);
	}

	@Test
	void checkPaymentStatusWithoutErrandReadsPersonAndMonth() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(paymentStatusServiceMock.read(MUNICIPALITY_ID, PERSONAL_NUMBER, YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(true, "2026-05-27"));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		assertThat(response.getDetail()).isNull();
		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void checkPaymentStatusWithoutErrandNotEffectuated() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(paymentStatusServiceMock.read(MUNICIPALITY_ID, PERSONAL_NUMBER, YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(false, null));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getPaymentDate()).isNull();
	}

	@Test
	void checkPaymentStatusEffectuatedWhenEveryDecidedPaymentIsPaidInLifecare() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			decided("REGISTERED", "101", LocalDate.of(2026, 5, 27)),
			decided("REGISTERED", "102", LocalDate.of(2026, 6, 10)),
			// A draft and a payment mirrored from Lifecare are not part of the decision.
			decided("DRAFT", null, null),
			Payment.create().withSource("LIFECARE").withStatus("REGISTERED").withLifecareId("999")));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(paymentStatusServiceMock.paidPaymentDates(MUNICIPALITY_ID, PERSONAL_NUMBER, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30)))
			.thenReturn(Map.of("101", "2026-05-27", "102", "2026-06-10"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-06-10");
		assertThat(response.getDetail()).isNull();
	}

	@Test
	void checkPaymentStatusNotEffectuatedByAnotherPaymentForTheSameMonth() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(decided("REGISTERED", "101", LocalDate.of(2026, 5, 27))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		// Lifecare holds a paid payment for the person and month, but not the one this errand registered.
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any())).thenReturn(Map.of("555", "2026-05-27"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getPaymentDate()).isNull();
		assertThat(response.getDetail()).isEqualTo("1 av 1 registrerade utbetalningar hittas inte som utbetalda i Lifecare");
	}

	@Test
	void checkPaymentStatusNotEffectuatedWhileAPaymentAwaitsRegistration() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			decided("REGISTERED", "101", LocalDate.of(2026, 5, 27)),
			decided("PENDING_REGISTRATION", null, LocalDate.of(2026, 5, 27)),
			decided("FAILED", null, LocalDate.of(2026, 5, 27))));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("2 av 3 beslutade utbetalningar är inte registrerade i Lifecare");
		verifyNoInteractions(paymentStatusServiceMock, citizenServiceMock);
	}

	@Test
	void checkPaymentStatusNotEffectuatedWhenRegisteredWithoutLifecareId() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(decided("REGISTERED", " ", LocalDate.of(2026, 5, 27))));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("1 av 1 beslutade utbetalningar är inte registrerade i Lifecare");
		verifyNoInteractions(paymentStatusServiceMock);
	}

	@Test
	void checkPaymentStatusNotEffectuatedWithoutDecidedPayments() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(decided("DRAFT", null, null)));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("Ärendet har inga beslutade utbetalningar");
		verifyNoInteractions(paymentStatusServiceMock, citizenServiceMock);
	}

	@Test
	void checkPaymentStatusWidensTheLifecareWindowToTheDecidedPaymentDates() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			decided("REGISTERED", "101", LocalDate.of(2026, 4, 20)),
			decided("REGISTERED", "102", LocalDate.of(2026, 7, 3)),
			decided("REGISTERED", "103", null)));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(paymentStatusServiceMock.paidPaymentDates(MUNICIPALITY_ID, PERSONAL_NUMBER, LocalDate.of(2026, 4, 20), LocalDate.of(2026, 7, 3)))
			.thenReturn(Map.of("101", "2026-04-20", "102", "2026-07-03", "103", "2026-05-27"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-07-03");
	}

	@Test
	void checkPaymentStatusUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No citizen found for partyId f47ac10b-58cc-4372-a567-0e02b2c3d479");

		verify(paymentStatusServiceMock, never()).read(eq(MUNICIPALITY_ID), any(), any());
	}
}
