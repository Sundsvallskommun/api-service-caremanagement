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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecareJobStimulusService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareJobStimulusResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/job-stimulus-periods";
	private static final Map<String, String> VARS = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	private static final List<LifecareJobStimulusPeriod> PERIODS = List.of(new LifecareJobStimulusPeriod(101, "APPLICANT", "2026-01-01", "2027-12-31"),
		new LifecareJobStimulusPeriod(201, "CO_APPLICANT", "2026-03-01", null));

	@MockitoBean
	private ErrandLifecareJobStimulusService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void readJobStimulusPeriods() {
		when(serviceMock.periods(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(PERIODS);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(VARS))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(LifecareJobStimulusPeriod.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(PERIODS);
		verify(serviceMock).periods(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void addJobStimulusPeriod() {
		final var request = new LifecareJobStimulusPeriodRequest("2028-01-15", null);
		when(serviceMock.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(PERIODS);

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(VARS))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectBodyList(LifecareJobStimulusPeriod.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(PERIODS);
		verify(serviceMock).addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
		verifyNoMoreInteractions(serviceMock);
	}
}
