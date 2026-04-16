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
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.service.StakeholderParameterService;

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
class StakeholderParameterResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String STAKEHOLDER_ID = randomUUID().toString();
	private static final Long PARAMETER_ID = 42L;
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/stakeholders/{stakeholderId}/parameters";

	@MockitoBean
	private StakeholderParameterService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createParameter() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(STAKEHOLDER_ID), any(StakeholderParameter.class))).thenReturn(PARAMETER_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "stakeholderId", STAKEHOLDER_ID)))
			.bodyValue(StakeholderParameter.create().withKey("k"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(STAKEHOLDER_ID), any(StakeholderParameter.class));
	}

	@Test
	void readParameters() {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID)).thenReturn(List.of(StakeholderParameter.create()));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "stakeholderId", STAKEHOLDER_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(StakeholderParameter.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);
	}

	@Test
	void readParameter() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID)).thenReturn(StakeholderParameter.create().withId(PARAMETER_ID));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "stakeholderId", STAKEHOLDER_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(StakeholderParameter.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID);
	}

	@Test
	void updateParameter() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "stakeholderId", STAKEHOLDER_ID, "parameterId", PARAMETER_ID)))
			.bodyValue(StakeholderParameter.create().withKey("k"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(STAKEHOLDER_ID), eq(PARAMETER_ID), any(StakeholderParameter.class));
	}

	@Test
	void deleteParameter() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{parameterId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "stakeholderId", STAKEHOLDER_ID, "parameterId", PARAMETER_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID);
	}
}
