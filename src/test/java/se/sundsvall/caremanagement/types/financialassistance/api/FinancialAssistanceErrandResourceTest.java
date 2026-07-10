package se.sundsvall.caremanagement.types.financialassistance.api;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshotSection;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceErrandService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceErrandResourceTest {

	private static final String ERRAND_ID = randomUUID().toString();
	private static final String SLUG = "financial-assistance-new";
	private static final String CREATE_PATH = "/{municipalityId}/{namespace}/errands/" + SLUG;

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";
	@MockitoBean
	private FinancialAssistanceErrandService errandServiceMock;
	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createErrand() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Min application").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandToleratesRequestPartWithoutJsonContentType() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		// Reproduces an axios FormData client that appends the JSON 'request' part without a 'Content-Type:
		// application/json' (the part arrives as text/plain). The endpoint must still bind and create.
		final var requestJson = "{\"title\":\"Utan content-type\",\"data\":{}}";
		final var builder = new MultipartBodyBuilder();
		builder.part("request", requestJson, TEXT_PLAIN);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandAcceptsScalarWhereModelExpectsList() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		// Mirrors the Mina sidor client: normType is a List<String> in the model but is sent as a single scalar.
		final var requestJson = "{\"title\":\"Återansökan\",\"data\":{\"normType\":\"NATIONAL_NORM\"}}";
		final var builder = new MultipartBodyBuilder();
		builder.part("request", requestJson.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		final var captor = ArgumentCaptor.forClass(CreateFinancialAssistanceRequest.class);
		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), captor.capture(), any(), any(), any());
		assertThat(captor.getValue().getData().getNormType()).containsExactly("NATIONAL_NORM"); // scalar coerced to a single-element list
	}

	@Test
	void createErrandWithCaseDataSnapshot() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med ärendeuppgifter").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("caseData", "%PDF-1.4".getBytes()).filename("snapshot.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandWithAttachments() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("attachments", "hyreskontrakt".getBytes()).filename("hyreskontrakt.pdf");
		builder.part("attachments", "hyresavi".getBytes()).filename("hyresavi.png");

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandWithFormSnapshot() {
		when(errandServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med formulärsnapshot").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("formSnapshot", "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}", APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID);

		verify(errandServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void readErrand() {
		when(errandServiceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(FinancialAssistanceView.create().withId(ERRAND_ID));

		final var view = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinancialAssistanceView.class)
			.returnResult()
			.getResponseBody();

		assertThat(view).isNotNull();
		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		verify(errandServiceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readFormSnapshot() {
		when(errandServiceMock.readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(FormSnapshot.create().withSchemaVersion("form-snapshot/1").withTitle("Ansökan")
				.withSections(List.of(FormSnapshotSection.create().withId("household").withTitle("Hushåll"))));

		final var snapshot = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}/form-snapshot").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(FormSnapshot.class)
			.returnResult()
			.getResponseBody();

		assertThat(snapshot).isNotNull();
		assertThat(snapshot.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(snapshot.getSections()).hasSize(1);
		verify(errandServiceMock).readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateData() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{errandId}/data").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(FinancialAssistanceData.create().withMaritalStatus("SINGLE"))
			.exchange()
			.expectStatus().isNoContent();

		verify(errandServiceMock).updateData(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(FinancialAssistanceData.class));
	}
}
