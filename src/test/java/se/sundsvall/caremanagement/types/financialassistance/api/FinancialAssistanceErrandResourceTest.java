package se.sundsvall.caremanagement.types.financialassistance.api;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.MultipartBodyBuilder;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshotSection;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_PLAIN;

class FinancialAssistanceErrandResourceTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void createErrand() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Min application").withData(FinancialAssistanceData.create()), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandToleratesRequestPartWithoutJsonContentType() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		// Reproduces an axios FormData client that appends the JSON 'request' part without a 'Content-Type:
		// application/json' (the part arrives as text/plain). The endpoint must still bind and create.
		final var requestJson = "{\"title\":\"Utan content-type\",\"data\":{}}";
		final var builder = new MultipartBodyBuilder();
		builder.part("request", requestJson, TEXT_PLAIN);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandAcceptsScalarWhereModelExpectsList() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		// Mirrors the Mina sidor client: normType is a List<String> in the model but is sent as a single scalar.
		final var requestJson = "{\"title\":\"Återansökan\",\"data\":{\"normType\":\"NATIONAL_NORM\"}}";
		final var builder = new MultipartBodyBuilder();
		builder.part("request", requestJson.getBytes(StandardCharsets.UTF_8), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		final var captor = ArgumentCaptor.forClass(CreateFinancialAssistanceRequest.class);
		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), captor.capture(), any(), any(), any());
		assertThat(captor.getValue().getData().getNormType()).containsExactly("NATIONAL_NORM"); // scalar coerced to a single-element list
	}

	@Test
	void createErrandWithCaseDataSnapshot() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med ärendeuppgifter").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("caseData", "%PDF-1.4".getBytes()).filename("snapshot.pdf");

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandWithAttachments() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("attachments", "hyreskontrakt".getBytes()).filename("hyreskontrakt.pdf");
		builder.part("attachments", "hyresavi".getBytes()).filename("hyresavi.png");

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void createErrandWithFormSnapshot() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any())).thenReturn(ERRAND_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("request", CreateFinancialAssistanceRequest.create().withTitle("Med formulärsnapshot").withData(FinancialAssistanceData.create()), APPLICATION_JSON);
		builder.part("formSnapshot", "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}", APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(CREATE_PATH).build(base()))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(SLUG), any(CreateFinancialAssistanceRequest.class), any(), any(), any());
	}

	@Test
	void readErrand() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(FinancialAssistanceView.create().withId(ERRAND_ID));

		final var view = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{errandId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(FinancialAssistanceView.class)
			.returnResult()
			.getResponseBody();

		assertThat(view).isNotNull();
		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readFormSnapshot() {
		when(serviceMock.readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
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
		verify(serviceMock).readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateData() {
		webTestClient.put()
			.uri(uri -> uri.path(PATH + "/{errandId}/data").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(FinancialAssistanceData.create().withApplicationType("NEW"))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).updateData(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(FinancialAssistanceData.class));
	}
}
