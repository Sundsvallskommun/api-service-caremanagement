package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class FinancialAssistancePaymentResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void checkPaymentStatus_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payment-status").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PaymentStatusRequest.create())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(paymentServiceMock);
	}
}
