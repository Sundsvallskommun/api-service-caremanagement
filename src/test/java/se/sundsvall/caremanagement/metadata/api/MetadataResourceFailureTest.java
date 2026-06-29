package se.sundsvall.caremanagement.metadata.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MetadataResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String KIND = "STATUS";
	private static final String NAME = "NEW";
	private static final String BASE = "/{municipalityId}/{namespace}/metadata";

	@MockitoBean
	private MetadataService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createLookup_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookup_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookup_invalidKind() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookup_missingKind() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookup_blankName() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(" ").withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookup_missingName() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookups_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookups_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookups_invalidKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookups_missingKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookup_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookup_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookup_invalidKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookup_badMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookup_badNamespace() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookup_invalidKind() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookup_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookup_badNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookup_invalidKind() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
