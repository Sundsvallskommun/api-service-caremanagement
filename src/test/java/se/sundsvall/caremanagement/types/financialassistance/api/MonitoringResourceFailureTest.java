package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.Map;
import org.assertj.core.groups.Tuple;
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
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.time.Month.JULY;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MonitoringResourceFailureTest {
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MONITORING_PATH = PATH + "/{errandId}/monitorings";

	@MockitoBean
	private MonitoringService monitoringServiceMock;
	@Autowired
	private WebTestClient webTestClient;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.isNotEmpty()
			.allSatisfy(violation -> assertThat(violation.field()).isNotBlank())
			.allSatisfy(violation -> assertThat(violation.message()).isNotBlank());
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	private Map<String, ?> monitoringBase() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	@Test
	void createMonitoringBlankTitle() {
		webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(monitoringBase()))
			.bodyValue(MonitoringRequest.create().withTitle(" ").withStartDate(LocalDate.of(2026, JULY, 1)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("title", "must not be blank")));

		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void createMonitoringMissingStartDate() {
		webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(monitoringBase()))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("startDate", "must not be null")));

		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void createMonitoringInvalidSource() {
		webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(monitoringBase()))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, JULY, 1)).withSource("SOMETHING_ELSE"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("source", "must be one of: [CASEWORKER, LIFECARE]")));

		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void createMonitoringInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, JULY, 1)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createMonitoring.errandId", "not a valid UUID")));

		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void getMonitoringInvalidMonitoringId() {
		webTestClient.get()
			.uri(uri -> uri.path(MONITORING_PATH + "/{monitoringId}").build(
				Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "monitoringId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("getMonitoring.monitoringId", "not a valid UUID")));

		verifyNoInteractions(monitoringServiceMock);
	}

	@Test
	void createMonitoringInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(MONITORING_PATH).build(Map.of("municipalityId", "abc", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(MonitoringRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, JULY, 1)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createMonitoring.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(monitoringServiceMock);
	}
}
