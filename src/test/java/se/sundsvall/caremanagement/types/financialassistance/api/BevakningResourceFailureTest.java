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
import se.sundsvall.caremanagement.types.financialassistance.api.model.BevakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.BevakningService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class BevakningResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String BEVAKNING_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/bevakningar";

	@MockitoBean
	private BevakningService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	@Test
	void createBevakning_blankTitle() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(BevakningRequest.create().withTitle(" ").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createBevakning_missingStartDate() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(BevakningRequest.create().withTitle("Följ upp"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createBevakning_invalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(BevakningRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getBevakning_invalidBevakningId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{bevakningId}").build(
				Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "bevakningId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createBevakning_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "abc", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(BevakningRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
