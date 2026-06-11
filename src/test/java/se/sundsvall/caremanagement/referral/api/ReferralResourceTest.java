package se.sundsvall.caremanagement.referral.api;

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
import se.sundsvall.caremanagement.referral.api.model.Referral;
import se.sundsvall.caremanagement.referral.api.model.ReferralResponseRequest;
import se.sundsvall.caremanagement.referral.service.ReferralService;

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
class ReferralResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String REFERRAL_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/referrals";

	@MockitoBean
	private ReferralService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	private Map<String, ?> withReferral() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "referralId", REFERRAL_ID);
	}

	@Test
	void createReferral() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Referral.class))).thenReturn(REFERRAL_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(base()))
			.bodyValue(Referral.create().withAuthority("ENVIRONMENTAL_OFFICE"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Referral.class));
	}

	@Test
	void readReferrals() {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Referral.create().withId("r1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Referral.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readReferral() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, REFERRAL_ID)).thenReturn(Referral.create().withId(REFERRAL_ID));

		final var referral = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{referralId}").build(withReferral()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Referral.class)
			.returnResult()
			.getResponseBody();

		assertThat(referral).isNotNull();
		assertThat(referral.getId()).isEqualTo(REFERRAL_ID);
	}

	@Test
	void registerResponse() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/{referralId}/response").build(withReferral()))
			.bodyValue(new ReferralResponseRequest("No objection"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).registerResponse(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, REFERRAL_ID, "No objection");
	}

	@Test
	void deleteReferral() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{referralId}").build(withReferral()))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, REFERRAL_ID);
	}
}
