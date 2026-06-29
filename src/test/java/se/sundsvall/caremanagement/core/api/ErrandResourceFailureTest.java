package se.sundsvall.caremanagement.core.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ErrandResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String BASE = "/{municipalityId}/{namespace}/errands";

	@MockitoBean
	private ErrandService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Errand validErrand() {
		return Errand.create().withTypeSlug("case-type-slug").withTitle("A title");
	}

	// ---------------------------------------------------------------------
	// createErrand — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void createErrand_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.bodyValue(validErrand())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.bodyValue(validErrand())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// createErrand — body validation (OnCreate group)
	// ---------------------------------------------------------------------

	@Test
	void createErrand_blankTypeSlug() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Errand.create().withTypeSlug(" ").withTitle("A title"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_missingTypeSlug() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Errand.create().withTitle("A title"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_blankTitle() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Errand.create().withTypeSlug("case-type-slug").withTitle(" "))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_missingTitle() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(Errand.create().withTypeSlug("case-type-slug"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createErrand_readOnlyIdSupplied() {
		webTestClient.post()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(validErrand().withId(randomUUID().toString()))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// readErrand — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void readErrand_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readErrand_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readErrand_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// findErrands — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void findErrands_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void findErrands_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// countErrands — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void countErrands_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/count").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void countErrands_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(BASE + "/count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// updateErrand — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void updateErrand_badMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(PatchErrand.create().withTitle("Updated"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateErrand_badNamespace() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(PatchErrand.create().withTitle("Updated"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateErrand_badErrandIdUuid() {
		webTestClient.patch()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(PatchErrand.create().withTitle("Updated"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	// ---------------------------------------------------------------------
	// deleteErrand — path-variable validation
	// ---------------------------------------------------------------------

	@Test
	void deleteErrand_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteErrand_badNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteErrand_badErrandIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(BASE + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
