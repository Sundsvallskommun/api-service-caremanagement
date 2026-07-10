package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Actualisation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceActualisationService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceIntakeResourceTest {
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	private RenewalPrefillService prefillServiceMock;

	@MockitoBean
	private EligibilityService eligibilityServiceMock;

	@MockitoBean
	private FinancialAssistanceActualisationService actualisationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getMetadata() {
		final var metadata = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/metadata").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinancialAssistanceMetadata.class)
			.returnResult()
			.getResponseBody();

		assertThat(metadata).isNotNull();
		assertThat(metadata.getIncomeTypes()).hasSize(33);
		assertThat(metadata.getCostTypes()).hasSize(16);
		// a citizen Mina-sidor type — external label + group
		assertThat(metadata.getCostTypes()).anySatisfy(option -> {
			assertThat(option.getCode()).isEqualTo("RENT");
			assertThat(option.getExternalDisplayName()).isEqualTo("Hyra (inte parkering/garage)");
			assertThat(option.getInternalDisplayName()).isEqualTo("Boendekostnad");
			assertThat(option.getGroup()).isEqualTo("HOUSING");
			assertThat(option.isCitizenReportable()).isTrue();
		});
		// a handläggare-only Lifecare type — internal label only, no external, not citizen-reportable
		assertThat(metadata.getCostTypes()).anySatisfy(option -> {
			assertThat(option.getCode()).isEqualTo("GLASSES");
			assertThat(option.getExternalDisplayName()).isNull();
			assertThat(option.getInternalDisplayName()).isEqualTo("Glasögon");
			assertThat(option.isCitizenReportable()).isFalse();
		});
	}

	@Test
	void checkEligibility() {
		when(eligibilityServiceMock.evaluate(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(EligibilityRequest.class)))
			.thenReturn(EligibilityResponse.create().withReasonCode("EXISTING_CASE"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/eligibility").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(EligibilityRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(EligibilityResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getReasonCode()).isEqualTo("EXISTING_CASE");
		verify(eligibilityServiceMock).evaluate(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(EligibilityRequest.class));
	}

	@Test
	void createActualisation() {
		when(actualisationServiceMock.createActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(ActualisationRequest.class)))
			.thenReturn(ActualisationResponse.create().withActualisationId(5012));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisation").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(ActualisationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ActualisationResponse.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).createActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(ActualisationRequest.class));
	}

	@Test
	void listActualisations() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(actualisationServiceMock.listActualisations(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull()))
			.thenReturn(List.of(Actualisation.create().withId(5012).withName("Ekonomiskt bistånd")));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Actualisation.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).singleElement().satisfies(actualisation -> assertThat(actualisation.getId()).isEqualTo(5012));
		verify(actualisationServiceMock).listActualisations(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull());
	}

	@Test
	void listActualisationsWithExplicitPeriod() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(actualisationServiceMock.listActualisations(MUNICIPALITY_ID, partyId, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30)))
			.thenReturn(List.of());

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/actualisations").queryParam("partyId", partyId).queryParam("from", "2026-01-01").queryParam("to", "2026-06-30").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk();

		verify(actualisationServiceMock).listActualisations(MUNICIPALITY_ID, partyId, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void archiveToActualisation() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");
		builder.part("request", ArchiveActualisationRequest.create().withTitle("Tilläggsansökan"), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isNoContent();

		verify(actualisationServiceMock).archiveToActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(partyId), eq(5012), any(MultipartFile.class), any(ArchiveActualisationRequest.class));
	}

	@Test
	void archiveToActualisationWithoutMetadata() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "%PDF-1.4".getBytes()).filename("tillaggsansokan.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/actualisations/{actualisationId}/archive").queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "actualisationId", 5012)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isNoContent();

		verify(actualisationServiceMock).archiveToActualisation(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(partyId), eq(5012), any(MultipartFile.class), isNull());
	}

	@Test
	void prefill() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(prefillServiceMock.prefill(MUNICIPALITY_ID, partyId)).thenReturn(RenewalPrefill.create().withLifecareChecked(true));

		final var prefill = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/prefill").queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(RenewalPrefill.class)
			.returnResult()
			.getResponseBody();

		assertThat(prefill).isNotNull();
		assertThat(prefill.isLifecareChecked()).isTrue();
		verify(prefillServiceMock).prefill(MUNICIPALITY_ID, partyId);
	}

}
