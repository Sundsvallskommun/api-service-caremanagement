package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.WarningCount;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceWarningService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceWarningResourceTest {
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String WARNING_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private FinancialAssistanceWarningService warningServiceMock;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createWarning() {
		when(warningServiceMock.createWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateWarningRequest.class)))
			.thenReturn(Warning.create().withId("w1").withType("MISSING_SSBTEK").withStatus("OPEN"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/warnings").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateWarningRequest.create().withType("MISSING_SSBTEK").withMessage("Inkomst saknas"))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID + "/warnings/w1")
			.expectBody(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("w1");
		verify(warningServiceMock).createWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateWarningRequest.class));
	}

	@Test
	void listWarnings() {
		when(warningServiceMock.listWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Warning.create().withId("w1").withType("MISSING_SSBTEK").withStatus("OPEN")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/warnings").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getType()).isEqualTo("MISSING_SSBTEK");
		verify(warningServiceMock).listWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void countWarnings() {
		when(warningServiceMock.countActiveWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(3L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/warnings/count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(WarningCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.count()).isEqualTo(3L);
		verify(warningServiceMock).countActiveWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateWarning() {
		when(warningServiceMock.updateWarning(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, WARNING_ID, "CLOSED"))
			.thenReturn(Warning.create().withId(WARNING_ID).withStatus("CLOSED"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/warnings/" + WARNING_ID).queryParam("status", "CLOSED").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("CLOSED");
		verify(warningServiceMock).updateWarning(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, WARNING_ID, "CLOSED");
	}

}
