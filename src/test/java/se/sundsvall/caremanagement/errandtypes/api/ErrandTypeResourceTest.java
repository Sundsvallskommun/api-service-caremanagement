package se.sundsvall.caremanagement.errandtypes.api;

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
import se.sundsvall.caremanagement.errandtypes.api.model.ErrandTypeSchema;
import se.sundsvall.caremanagement.errandtypes.service.ErrandTypeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ErrandTypeResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String SLUG = "financial-assistance-renewal";
	private static final String PATH = "/{municipalityId}/{namespace}/errand-types";

	@MockitoBean
	private ErrandTypeService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void findErrandTypes() {
		when(serviceMock.findAll()).thenReturn(List.of(ErrandTypeSchema.create().withTypeSlug(SLUG)));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandTypeSchema.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().hasSize(1);
		assertThat(response.getFirst().getTypeSlug()).isEqualTo(SLUG);
		verify(serviceMock).findAll();
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void readErrandType() {
		when(serviceMock.findBySlug(SLUG)).thenReturn(ErrandTypeSchema.create().withTypeSlug(SLUG).withApplicationType("RENEWAL"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{typeSlug}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "typeSlug", SLUG)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ErrandTypeSchema.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getTypeSlug()).isEqualTo(SLUG);
		assertThat(response.getApplicationType()).isEqualTo("RENEWAL");
		verify(serviceMock).findBySlug(SLUG);
		verifyNoMoreInteractions(serviceMock);
	}
}
