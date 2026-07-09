package se.sundsvall.caremanagement.namespaceconfig.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.namespaceconfig.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.namespaceconfig.service.NamespaceConfigService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class NamespaceConfigResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/namespace-config";

	@MockitoBean
	private NamespaceConfigService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createNamespaceConfig_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withDisplayName("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNamespaceConfig.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNamespaceConfig_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.bodyValue(NamespaceConfig.create().withDisplayName("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNamespaceConfig_blankDisplayName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withDisplayName(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNamespaceConfig.config.displayName", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNamespaceConfig_missingDisplayName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withShortCode("sc"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNamespaceConfig.config.displayName", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNamespaceConfig_nonNullId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withId(1L).withDisplayName("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNamespaceConfig.config.id", "must be null")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readNamespaceConfig_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readNamespaceConfig.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readNamespaceConfig_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateNamespaceConfig_badMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.bodyValue(NamespaceConfig.create().withDisplayName("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateNamespaceConfig.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateNamespaceConfig_badNamespace() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.bodyValue(NamespaceConfig.create().withDisplayName("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteNamespaceConfig_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteNamespaceConfig.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteNamespaceConfig_badNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteNamespaceConfig.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}
}
