package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Monitoring;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringCount;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;

import static java.time.Month.JULY;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class MonitoringResourceTest extends AbstractFinancialAssistanceResourceTest {

	private static final String MONITORING_ID = randomUUID().toString();
	private static final String MONITORING_PATH = PATH + "/{errandId}/monitorings";

	private Map<String, ?> monitoringBase() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	private Map<String, ?> withMonitoring() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "monitoringId", MONITORING_ID);
	}

	private static MonitoringRequest request() {
		return MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, JULY, 1)).withEndDate(LocalDate.of(2026, JULY, 31));
	}

	@Test
	void createMonitoring() {
		when(monitoringServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(MonitoringRequest.class)))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(monitoringBase()))
			.bodyValue(request())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID + "/monitorings/" + MONITORING_ID)
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MONITORING_ID);
		verify(monitoringServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(MonitoringRequest.class));
	}

	@Test
	void listMonitorings() {
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(MONITORING_PATH).build(monitoringBase()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getTitle()).isEqualTo("Följ upp");
		verify(monitoringServiceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void countMonitorings() {
		when(monitoringServiceMock.count(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(2L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(MONITORING_PATH + "/count").build(monitoringBase()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(MonitoringCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.count()).isEqualTo(2L);
		verify(monitoringServiceMock).count(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getMonitoring() {
		when(monitoringServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(MONITORING_PATH + "/{monitoringId}").build(withMonitoring()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MONITORING_ID);
		verify(monitoringServiceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID);
	}

	@Test
	void updateMonitoring() {
		when(monitoringServiceMock.update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MONITORING_ID), any(MonitoringRequest.class)))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Ändrad"));

		final var response = webTestClient.put()
			.uri(uri -> uri.path(MONITORING_PATH + "/{monitoringId}").build(withMonitoring()))
			.bodyValue(request().withTitle("Ändrad"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Ändrad");
		verify(monitoringServiceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MONITORING_ID), any(MonitoringRequest.class));
	}

	@Test
	void deleteMonitoring() {
		webTestClient.delete()
			.uri(uri -> uri.path(MONITORING_PATH + "/{monitoringId}").build(withMonitoring()))
			.exchange()
			.expectStatus().isNoContent();

		verify(monitoringServiceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID);
	}
}
