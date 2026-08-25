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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentStatusServiceTest {

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@InjectMocks
	private PaymentStatusService service;

	@Test
	void effectuatedWhenMatchingPaymentExists() {
		final var payment = new PersonBasedPaymentDTO().payDate("2026-05-27").concernedMonth("2026-06");
		when(lifecareFamilyCareIntegrationMock.getPayments(eq("199001011234"), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read("199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isTrue();
		assertThat(status.paymentDate()).isEqualTo("2026-05-27");
	}

	@Test
	void notEffectuatedWhenPaymentConcernsAnotherMonth() {
		final var payment = new PersonBasedPaymentDTO().payDate("2026-04-27").concernedMonth("2026-05");
		when(lifecareFamilyCareIntegrationMock.getPayments(any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read("199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
		assertThat(status.paymentDate()).isNull();
	}

	@Test
	void notEffectuatedWhenPaymentHasNoPayDate() {
		final var payment = new PersonBasedPaymentDTO().concernedMonth("2026-06");
		when(lifecareFamilyCareIntegrationMock.getPayments(any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of(payment)));

		final var status = service.read("199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
	}

	@Test
	void notEffectuatedWhenResponseIsNull() {
		when(lifecareFamilyCareIntegrationMock.getPayments(any(), any(), any())).thenReturn(null);

		final var status = service.read("199001011234", YearMonth.of(2026, JUNE));

		assertThat(status.effectuated()).isFalse();
		assertThat(status.paymentDate()).isNull();
	}

	@Test
	void queriesThePriorMonthThroughApplicationMonthWindow() {
		when(lifecareFamilyCareIntegrationMock.getPayments(any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedPaymentDTO().result(List.of()));

		service.read("199001011234", YearMonth.of(2026, JUNE));

		verify(lifecareFamilyCareIntegrationMock).getPayments("199001011234", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-06-30"));
	}
}
