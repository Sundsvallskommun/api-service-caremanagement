package se.sundsvall.caremanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.FindErrandsResponse;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.service.ErrandService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ErrandResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands";

	@MockitoBean
	private ErrandService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createErrand() {
		when(serviceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Errand.create().withTitle("title"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void readErrand() {
		when(serviceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Errand.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void findErrands() {
		when(serviceMock.findErrands(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(), any())).thenReturn(FindErrandsResponse.create().withErrands(java.util.List.of(Errand.create())));

		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).findErrands(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Specification.class), any());
	}

	@Test
	void updateErrand() {
		webTestClient.patch()
			.uri(builder -> builder.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(PatchErrand.create().withTitle("t"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PatchErrand.class));
	}

	@Test
	void deleteErrand() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}
}
