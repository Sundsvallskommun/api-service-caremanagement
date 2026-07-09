package se.sundsvall.caremanagement.journal.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.metadata.service.MetadataService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class JournalEntryMetadataResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/journal-entries/metadata";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void metadataWithInvalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("metadata.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(metadataServiceMock);
	}

	@Test
	void metadataWithInvalidNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("metadata.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(metadataServiceMock);
	}
}
