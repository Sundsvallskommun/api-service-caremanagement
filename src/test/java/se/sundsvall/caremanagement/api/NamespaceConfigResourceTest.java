package se.sundsvall.caremanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.service.NamespaceConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class NamespaceConfigResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/namespace-config";

	@MockitoBean
	private NamespaceConfigService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createNamespaceConfig() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NamespaceConfig.class))).thenReturn(1L);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withDisplayName("d").withShortCode("sc"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NamespaceConfig.class));
	}

	@Test
	void readNamespaceConfig() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE)).thenReturn(NamespaceConfig.create().withDisplayName("d"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NamespaceConfig.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE);
	}

	@Test
	void updateNamespaceConfig() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withDisplayName("d"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(NamespaceConfig.class));
	}

	@Test
	void deleteNamespaceConfig() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE);
	}
}
