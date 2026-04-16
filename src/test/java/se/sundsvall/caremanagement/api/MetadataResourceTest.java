package se.sundsvall.caremanagement.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.caremanagement.service.MetadataService;

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
class MetadataResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String NAME = "some-name";
	private static final String PATH = "/{municipalityId}/{namespace}/metadata";

	@MockitoBean
	private MetadataService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@ParameterizedTest
	@EnumSource(LookupKind.class)
	void createLookup(final LookupKind kind) {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), any(Lookup.class))).thenReturn(NAME);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).queryParam("kind", kind).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("dn"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), any(Lookup.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@EnumSource(LookupKind.class)
	void readLookups(final LookupKind kind) {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, kind)).thenReturn(List.of(Lookup.create().withName(NAME)));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("kind", kind).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Lookup.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().hasSize(1);
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, kind);
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@EnumSource(LookupKind.class)
	void readLookup(final LookupKind kind) {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, kind, NAME)).thenReturn(Lookup.create().withName(NAME));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{name}").queryParam("kind", kind).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Lookup.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getName()).isEqualTo(NAME);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, kind, NAME);
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@EnumSource(LookupKind.class)
	void updateLookup(final LookupKind kind) {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{name}").queryParam("kind", kind).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("d2"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), eq(NAME), any(Lookup.class));
		verifyNoMoreInteractions(serviceMock);
	}

	@ParameterizedTest
	@EnumSource(LookupKind.class)
	void deleteLookup(final LookupKind kind) {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{name}").queryParam("kind", kind).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, kind, NAME);
		verifyNoMoreInteractions(serviceMock);
	}
}
