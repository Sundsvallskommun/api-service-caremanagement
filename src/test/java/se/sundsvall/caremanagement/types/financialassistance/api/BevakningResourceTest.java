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
import se.sundsvall.caremanagement.types.financialassistance.api.model.Bevakning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.BevakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.BevakningService;

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
class BevakningResourceTest {

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

	private Map<String, ?> withBevakning() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "bevakningId", BEVAKNING_ID);
	}

	private static BevakningRequest request() {
		return BevakningRequest.create().withTitle("Följ upp").withStartDate(LocalDate.of(2026, 7, 1)).withEndDate(LocalDate.of(2026, 7, 31));
	}

	@Test
	void createBevakning() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(BevakningRequest.class)))
			.thenReturn(Bevakning.create().withId(BEVAKNING_ID).withTitle("Följ upp"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(request())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().exists("Location")
			.expectBody(Bevakning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(BEVAKNING_ID);
		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(BevakningRequest.class));
	}

	@Test
	void listBevakningar() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Bevakning.create().withId(BEVAKNING_ID).withTitle("Följ upp")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Bevakning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getTitle()).isEqualTo("Följ upp");
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getBevakning() {
		when(serviceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BEVAKNING_ID))
			.thenReturn(Bevakning.create().withId(BEVAKNING_ID).withTitle("Följ upp"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{bevakningId}").build(withBevakning()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Bevakning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(BEVAKNING_ID);
		verify(serviceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BEVAKNING_ID);
	}

	@Test
	void updateBevakning() {
		when(serviceMock.update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(BEVAKNING_ID), any(BevakningRequest.class)))
			.thenReturn(Bevakning.create().withId(BEVAKNING_ID).withTitle("Ändrad"));

		final var response = webTestClient.put()
			.uri(uri -> uri.path(PATH + "/{bevakningId}").build(withBevakning()))
			.bodyValue(request().withTitle("Ändrad"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Bevakning.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Ändrad");
		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(BEVAKNING_ID), any(BevakningRequest.class));
	}

	@Test
	void deleteBevakning() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{bevakningId}").build(withBevakning()))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BEVAKNING_ID);
	}
}
