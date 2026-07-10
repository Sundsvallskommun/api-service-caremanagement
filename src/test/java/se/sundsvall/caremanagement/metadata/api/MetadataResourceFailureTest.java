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
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MetadataResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String KIND = "STATUS";
	private static final String NAME = "NEW";
	private static final String BASE = "/{municipalityId}/{namespace}/metadata";
	private static final String INVALID_KIND_MESSAGE = "must be one of: [ROLE, STATUS, CATEGORY, JOURNAL_ENTRY_TYPE, CONTACT_REASON, DOCUMENT_TYPE, TYPE]";

	@MockitoBean
	private MetadataService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createLookupBadMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createLookup.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookupBadNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createLookup.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookupInvalidKind() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createLookup.kind", INVALID_KIND_MESSAGE)));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookupMissingKind() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(NAME).withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("Required parameter 'kind' is not present.");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookupBlankName() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withName(" ").withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createLookup.lookup.name", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createLookupMissingName() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createLookup.lookup.name", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupsBadMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookups.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupsBadNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookups.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupsInvalidKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookups.kind", INVALID_KIND_MESSAGE)));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupsMissingKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("Required parameter 'kind' is not present.");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupBadMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookup.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupBadNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookup.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readLookupInvalidKind() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readLookup.kind", INVALID_KIND_MESSAGE)));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookupBadMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateLookup.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookupBadNamespace() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateLookup.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateLookupInvalidKind() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.bodyValue(Lookup.create().withDisplayName("New case"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateLookup.kind", INVALID_KIND_MESSAGE)));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookupBadMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteLookup.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookupBadNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", KIND)
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteLookup.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteLookupInvalidKind() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{name}").queryParam("kind", "NOT_A_KIND")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "name", NAME)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteLookup.kind", INVALID_KIND_MESSAGE)));

		verifyNoInteractions(serviceMock);
	}
}
