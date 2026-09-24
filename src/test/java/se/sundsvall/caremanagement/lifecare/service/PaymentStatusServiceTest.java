package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
