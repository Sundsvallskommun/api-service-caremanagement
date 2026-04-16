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
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.caremanagement.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CATEGORY;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CONTACT_REASON;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.ROLE;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.STATUS;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.TYPE;

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

	private void verifyCrud(final String segment, final LookupKind kind) {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), any(Lookup.class))).thenReturn(NAME);
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, kind)).thenReturn(List.of(Lookup.create()));
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, kind, NAME)).thenReturn(Lookup.create().withName(NAME));

		webTestClient.post()
			.uri(uri -> uri.path(PATH + segment).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("dn"))
			.exchange()
			.expectStatus().isCreated();

		final var list = webTestClient.get()
			.uri(uri -> uri.path(PATH + segment).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Lookup.class)
			.returnResult()
			.getResponseBody();
		assertThat(list).isNotNull();

		final var single = webTestClient.get()
			.uri(uri -> uri.path(PATH + segment + "/{name}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Lookup.class)
			.returnResult()
			.getResponseBody();
		assertThat(single).isNotNull();

		webTestClient.patch()
			.uri(uri -> uri.path(PATH + segment + "/{name}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("d2"))
			.exchange()
			.expectStatus().isNoContent();

		webTestClient.delete()
			.uri(uri -> uri.path(PATH + segment + "/{name}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), any(Lookup.class));
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, kind);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, kind, NAME);
		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(kind), eq(NAME), any(Lookup.class));
		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, kind, NAME);
	}

	@Test
	void categoriesCrud() {
		verifyCrud("/categories", CATEGORY);
	}

	@Test
	void statusesCrud() {
		verifyCrud("/statuses", STATUS);
	}

	@Test
	void typesCrud() {
		verifyCrud("/types", TYPE);
	}

	@Test
	void rolesCrud() {
		verifyCrud("/roles", ROLE);
	}

	@Test
	void contactReasonsCrud() {
		verifyCrud("/contact-reasons", CONTACT_REASON);
	}
}
