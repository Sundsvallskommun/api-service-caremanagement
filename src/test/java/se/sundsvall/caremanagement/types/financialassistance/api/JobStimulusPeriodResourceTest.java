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
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.service.JobStimulusPeriodService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class JobStimulusPeriodResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/job-stimulus-periods";

	@MockitoBean
	private JobStimulusPeriodService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void listJobStimulusPeriods() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			new JobStimulusPeriod("APPLICANT", LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31"))));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(JobStimulusPeriod.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(new JobStimulusPeriod("APPLICANT", LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")));
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void invalidErrandIdIsBadRequest() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
