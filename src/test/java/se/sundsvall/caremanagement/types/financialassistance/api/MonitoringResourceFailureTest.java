package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.MonitoringService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MonitoringResourceFailureTest {

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

	@Test
	void createMonitoring_blankTitle() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(MonitoringRequest.create().withTitle(" ").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createMonitoring_missingStartDate() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createMonitoring_invalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getMonitoring_invalidMonitoringId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{monitoringId}").build(
				Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "monitoringId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createMonitoring_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "abc", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
