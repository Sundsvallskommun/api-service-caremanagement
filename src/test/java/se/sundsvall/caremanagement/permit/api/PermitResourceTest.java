package se.sundsvall.caremanagement.permit.api;

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
import se.sundsvall.caremanagement.permit.api.model.Permit;
import se.sundsvall.caremanagement.permit.service.PermitService;

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
class PermitResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PERMIT_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/permits";

	@MockitoBean
	private PermitService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	@Test
	void issuePermit() {
		when(serviceMock.issue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Permit.class))).thenReturn(PERMIT_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(Permit.create().withPermitType("PARKING_PERMIT"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).issue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Permit.class));
	}

	@Test
	void readPermits() {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Permit.create().withId("p1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Permit.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readPermit() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PERMIT_ID)).thenReturn(Permit.create().withId(PERMIT_ID));

		final var permit = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{permitId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "permitId", PERMIT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Permit.class)
			.returnResult()
			.getResponseBody();

		assertThat(permit).isNotNull();
		assertThat(permit.getId()).isEqualTo(PERMIT_ID);
	}

	@Test
	void revokePermit() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/{permitId}/revoke").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "permitId", PERMIT_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).revoke(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PERMIT_ID);
	}

	@Test
	void revokeAllPermits() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/revoke").build(base()))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).revokeAllForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void deletePermit() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{permitId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "permitId", PERMIT_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PERMIT_ID);
	}
}
