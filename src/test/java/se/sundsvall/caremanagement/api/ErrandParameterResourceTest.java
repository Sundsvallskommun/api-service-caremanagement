package se.sundsvall.caremanagement.api;

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
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.service.ErrandParameterService;

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
class ErrandParameterResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PARAMETER_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/parameters";

	@MockitoBean
	private ErrandParameterService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createParameter() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Parameter.class))).thenReturn(PARAMETER_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Parameter.create().withKey("k"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Parameter.class));
	}

	@Test
	void readParameters() {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Parameter.create()));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Parameter.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readParameter() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID)).thenReturn(Parameter.create().withId(PARAMETER_ID));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Parameter.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID);
	}

	@Test
	void updateParameter() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.bodyValue(Parameter.create().withKey("k"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PARAMETER_ID), any(Parameter.class));
	}

	@Test
	void deleteParameter() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID);
	}
}
