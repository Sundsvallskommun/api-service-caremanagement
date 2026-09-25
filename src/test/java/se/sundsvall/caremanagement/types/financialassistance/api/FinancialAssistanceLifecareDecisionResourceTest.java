package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionView;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionService;
import se.sundsvall.dept44.support.Identifier;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareDecisionResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String BASE = "/{municipalityId}/{namespace}/errands/financial-assistance";
	private static final String DECISION_PATH = BASE + "/{errandId}/lifecare/decision";
	private static final Map<String, String> PATH_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	private static final LifecareDecisionView VIEW = new LifecareDecisionView(98, 153, "BIFALL", "2026-09-23", "2026-09-01", "2026-09-30",
		new BigDecimal("3000"), 19, "Arbetar deltid ofrivilligt, otillräcklig inkomst", "<p>Beslut</p>", false, "Test Handläggare");

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private LifecareDecisionService serviceMock;

	@Test
	void readDecision() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(VIEW));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareDecisionView.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(VIEW);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readDecisionBeforeOneIsSaved() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.empty());

		webTestClient.get()
			.uri(builder -> builder.path(DECISION_PATH).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void saveDecision() {
		final var request = new LifecareDecisionSaveRequest(153, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
			new BigDecimal("3000"), 19, "<p>Beslut</p>", true);
		when(serviceMock.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(VIEW);

		final var response = webTestClient.put()
			.uri(builder -> builder.path(DECISION_PATH).build(PATH_VARIABLES))
			.header(Identifier.HEADER_NAME, "test; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareDecisionView.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(VIEW);
		verify(serviceMock).save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}

	@Test
	void readDecisionTypes() {
		final var types = List.of(new LifecareDecisionType(153, "bifall", "BIFALL", true, true), new LifecareDecisionType(161, "EK Återkrav", null, false, false));
		when(serviceMock.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(types);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_PATH + "/types").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(new ParameterizedTypeReference<List<LifecareDecisionType>>() {})
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(types);
	}

	@Test
	void readDecisionReasons() {
		final var reasons = List.of(new LifecareDecisionReason(19, "Arbetar deltid ofrivilligt, otillräcklig inkomst", "Arbetar deltid, ofrivilligt"));
		when(serviceMock.reasons(153)).thenReturn(reasons);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(BASE + "/lifecare/decision-types/{decisionCode}/reasons")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "decisionCode", "153")))
			.exchange()
			.expectStatus().isOk()
			.expectBody(new ParameterizedTypeReference<List<LifecareDecisionReason>>() {})
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(reasons);
	}

	@Test
	void readDecisionPdf() {
		final var pdf = "%PDF-1.7 beslut".getBytes();
		when(serviceMock.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(pdf);

		final var response = webTestClient.get()
			.uri(builder -> builder.path(DECISION_PATH + "/pdf").build(PATH_VARIABLES))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_PDF)
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(pdf);
	}
}
