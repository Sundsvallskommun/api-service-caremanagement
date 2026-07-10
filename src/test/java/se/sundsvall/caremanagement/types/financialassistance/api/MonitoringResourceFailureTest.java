package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.MonitoringRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.time.Month.JULY;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class MonitoringResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	private static final String MONITORING_PATH = PATH + "/{errandId}/monitorings";

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
