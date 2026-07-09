package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.WarningCount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class FinancialAssistanceWarningResourceTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void createWarning() {
		when(warningServiceMock.createWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(CreateWarningRequest.class)))
			.thenReturn(Warning.create().withId("w1").withType("MISSING_SSBTEK").withStatus("OPEN"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(base()))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateWarningRequest.create().withType("MISSING_SSBTEK").withMessage("Inkomst saknas"))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/errand-1/warnings/w1")
			.expectBody(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo("w1");
		verify(warningServiceMock).createWarning(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("errand-1"), any(CreateWarningRequest.class));
	}

	@Test
	void listWarnings() {
		when(warningServiceMock.listWarnings(MUNICIPALITY_ID, NAMESPACE, "errand-1"))
			.thenReturn(List.of(Warning.create().withId("w1").withType("MISSING_SSBTEK").withStatus("OPEN")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getType()).isEqualTo("MISSING_SSBTEK");
		verify(warningServiceMock).listWarnings(MUNICIPALITY_ID, NAMESPACE, "errand-1");
	}

	@Test
	void countWarnings() {
		when(warningServiceMock.countActiveWarnings(MUNICIPALITY_ID, NAMESPACE, "errand-1")).thenReturn(3L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings/count").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(WarningCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.count()).isEqualTo(3L);
		verify(warningServiceMock).countActiveWarnings(MUNICIPALITY_ID, NAMESPACE, "errand-1");
	}

	@Test
	void updateWarning() {
		when(warningServiceMock.updateWarning(MUNICIPALITY_ID, NAMESPACE, "errand-1", "w1", "CLOSED"))
			.thenReturn(Warning.create().withId("w1").withStatus("CLOSED"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings/w1").queryParam("status", "CLOSED").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Warning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("CLOSED");
		verify(warningServiceMock).updateWarning(MUNICIPALITY_ID, NAMESPACE, "errand-1", "w1", "CLOSED");
	}

}
