package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.Monitoring;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.MonitoringService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MonitoringResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MONITORING_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/monitorings";

	@MockitoBean
	private MonitoringService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	private Map<String, ?> withMonitoring() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "monitoringId", MONITORING_ID);
	}

	private static MonitoringRequest request() {
		return MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)).withEndDate(LocalDate.of(2026, 7, 31));
	}

	@Test
	void createMonitoring() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(MonitoringRequest.class)))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(request())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().exists("Location")
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MONITORING_ID);
		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(MonitoringRequest.class));
	}

	@Test
	void listMonitorings() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getTitle()).isEqualTo("Följ upp");
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getMonitoring() {
		when(serviceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Följ upp"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{monitoringId}").build(withMonitoring()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(MONITORING_ID);
		verify(serviceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID);
	}

	@Test
	void updateMonitoring() {
		when(serviceMock.update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MONITORING_ID), any(MonitoringRequest.class)))
			.thenReturn(Monitoring.create().withId(MONITORING_ID).withTitle("Ändrad"));

		final var response = webTestClient.put()
			.uri(uri -> uri.path(PATH + "/{monitoringId}").build(withMonitoring()))
			.bodyValue(request().withTitle("Ändrad"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Monitoring.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Ändrad");
		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MONITORING_ID), any(MonitoringRequest.class));
	}

	@Test
	void deleteMonitoring() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{monitoringId}").build(withMonitoring()))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MONITORING_ID);
	}
}
