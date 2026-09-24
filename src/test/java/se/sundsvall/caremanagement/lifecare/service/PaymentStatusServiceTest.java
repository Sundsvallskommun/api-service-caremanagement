package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class PaymentStatusServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@InjectMocks
	private PaymentStatusService service;

	@Test
	void effectuatedWhenMatchingPaymentExists() {
		final var payment = new PersonBasedPaymentDTO().payDate("2026-05-27").concernedMonth("2026-06");
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), eq("199001011234"), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isTrue();
		assertThat(status.paymentDate()).isEqualTo("2026-05-27");
	}

	@Test
	void notEffectuatedWhenPaymentConcernsAnotherMonth() {
		final var payment = new PersonBasedPaymentDTO().payDate("2026-04-27").concernedMonth("2026-05");
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
		assertThat(status.paymentDate()).isNull();
	}

	@Test
	void notEffectuatedWhenPaymentHasNoPayDate() {
		final var payment = new PersonBasedPaymentDTO().concernedMonth("2026-06");
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
	}

	@Test
	void notEffectuatedWhenResponseIsNull() {
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any())).thenReturn(null);

		final var status = service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
		assertThat(status.paymentDate()).isNull();
	}

	@Test
	void queriesThePriorMonthThroughApplicationMonthWindow() {
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of()));

		service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE));

		verify(lifecareFamilyCareIntegrationMock).getPayments(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"));
	}

	@Test
	void paidPaymentDatesKeysPaidPaymentsOnTheirLifecareId() {
		when(lifecareFamilyCareIntegrationMock.getPayments(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-04-20"), LocalDate.parse("2026-07-03")))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(
				new PersonBasedPaymentDTO().id(101).payDate("2026-05-27"),
				new PersonBasedPaymentDTO().id(101).payDate("2026-05-28"),
				new PersonBasedPaymentDTO().id(102),
				new PersonBasedPaymentDTO().payDate("2026-05-27"))));

		final var paid = service.paidPaymentDates(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-04-20"), LocalDate.parse("2026-07-03"));

		assertThat(paid).containsExactly(entry("101", "2026-05-27"));
	}

	@Test
	void paidPaymentDatesIsEmptyWhenResponseIsNull() {
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any())).thenReturn(null);

		assertThat(service.paidPaymentDates(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"))).isEmpty();
	}

	@Test
	void registeredPaymentsCarryTheInsatsMonthAndPayDateOfEveryPaymentWithAnId() {
		when(lifecareFamilyCareIntegrationMock.getPayments(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(
				new PersonBasedPaymentDTO().id(101).serviceId(7700).concernedMonth("2026-06").payDate("2026-05-27"),
				new PersonBasedPaymentDTO().id(102).serviceId(7700).concernedMonth("2026-06"),
				new PersonBasedPaymentDTO().serviceId(7700).payDate("2026-05-27"))));

		final var payments = service.registeredPayments(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"));

		assertThat(payments).containsExactly(
			new LifecarePayment("101", 7700, "2026-06", "2026-05-27"),
			new LifecarePayment("102", 7700, "2026-06", null));
	}

	@Test
	void registeredPaymentsIsEmptyWhenResponseIsNull() {
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any())).thenReturn(null);

		assertThat(service.registeredPayments(MUNICIPALITY_ID, "199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"))).isEmpty();
	}

	private static Stream<Arguments> everyRead() {
		return Stream.of(
			Arguments.of("read", (Consumer<PaymentStatusService>) service -> service.read(MUNICIPALITY_ID, "199001011234", YearMonth.of(2026, JUNE))),
			Arguments.of("paidPaymentDates", (Consumer<PaymentStatusService>) service -> service.paidPaymentDates(MUNICIPALITY_ID, "199001011234",
				LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"))),
			Arguments.of("registeredPayments", (Consumer<PaymentStatusService>) service -> service.registeredPayments(MUNICIPALITY_ID, "199001011234",
				LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"))));
	}

	/**
	 * A failed Lifecare read must fail the call: answering "no payments" would count towards the escalation deadline, or
	 * leave the errand waiting, on data that was never read.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("everyRead")
	void aLifecareIntegratorFailureIsPropagatedNotReadAsNoPayments(final String name, final Consumer<PaymentStatusService> read) {
		when(lifecareFamilyCareIntegrationMock.getPayments(eq(MUNICIPALITY_ID), any(), any(), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching payments in Lifecare FamilyCare"));

		assertThatThrownBy(() -> read.accept(service))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}
}
