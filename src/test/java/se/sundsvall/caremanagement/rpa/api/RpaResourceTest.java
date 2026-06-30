package se.sundsvall.caremanagement.rpa.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.rpa.api.model.RpaTaskRequest;
import se.sundsvall.caremanagement.rpa.service.RpaService;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class RpaResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/rpa-tasks";

	@MockitoBean
	private RpaService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void enqueue() {
		final var request = RpaTaskRequest.create().withAction("FETCH_SUPPLEMENTS").withParameters(Map.of("k", "v"));

		webTestClient.post()
			.uri(PATH, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isAccepted();

		verify(serviceMock).enqueue(eq(MUNICIPALITY_ID), eq(ERRAND_ID), eq("FETCH_SUPPLEMENTS"), eq(Map.of("k", "v")));
	}
}
