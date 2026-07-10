package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistancePaymentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistancePaymentResourceTest {
	private static final String NAMESPACE = "my-namespace";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";
	@MockitoBean
	private FinancialAssistancePaymentService paymentServiceMock;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	void checkPaymentStatus() {
		when(paymentServiceMock.checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class)))
			.thenReturn(PaymentStatusResponse.create().withEffectuated(true).withPaymentDate("2026-05-27"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payment-status").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(PaymentStatusRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PaymentStatusResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		verify(paymentServiceMock).checkPaymentStatus(eq(MUNICIPALITY_ID), any(PaymentStatusRequest.class));
	}

}
