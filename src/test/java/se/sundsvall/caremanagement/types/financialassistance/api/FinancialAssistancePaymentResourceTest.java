package se.sundsvall.caremanagement.types.financialassistance.api;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialAssistancePaymentResourceTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void checkPaymentStatus() {
		when(serviceMock.checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class)))
			.thenReturn(PaymentStatusResponse.create().withEffectuated(true).withPaymentDate("2026-05-27"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payment-status").build(base()))
			.bodyValue(PaymentStatusRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PaymentStatusResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		verify(serviceMock).checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class));
	}

}
