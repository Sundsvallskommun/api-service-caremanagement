package se.sundsvall.caremanagement.cocaseworkers.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.cocaseworkers.api.model.AddCoCaseworker;
import se.sundsvall.caremanagement.cocaseworkers.api.model.CoCaseworker;
import se.sundsvall.caremanagement.cocaseworkers.service.CoCaseworkerService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class CoCaseworkerResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String USER_ID = "jane01doe";
	private static final String CO_CASEWORKER_ID = "c1";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/co-caseworkers";

	@MockitoBean
	private CoCaseworkerService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addCoCaseworker() {
		when(serviceMock.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AddCoCaseworker(USER_ID))).thenReturn(CO_CASEWORKER_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new AddCoCaseworker(USER_ID))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/co-caseworkers/" + USER_ID);

		verify(serviceMock).add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AddCoCaseworker(USER_ID));
	}

	@Test
	void readCoCaseworkers() {
		when(serviceMock.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Sort.unsorted()))
			.thenReturn(List.of(CoCaseworker.create().withId(CO_CASEWORKER_ID).withUserId(USER_ID)));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(CoCaseworker.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getUserId()).isEqualTo(USER_ID);
		verify(serviceMock).listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Sort.unsorted());
	}

	@Test
	void removeCoCaseworker() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{userId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "userId", USER_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, USER_ID);
	}
}
