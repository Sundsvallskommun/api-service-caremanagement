package se.sundsvall.caremanagement.attachments.api;

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
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.attachments.api.model.Attachment;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class AttachmentResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/attachments";

	@MockitoBean
	private AttachmentService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createAttachment() {
		when(serviceMock.createAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), any(MultipartFile.class))).thenReturn(ATTACHMENT_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("file", "hello".getBytes()).filename("hello.txt");
		final MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).createAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), any(MultipartFile.class));
	}

	@Test
	void createAttachmentWithCaseDataDocumentType() {
		when(serviceMock.createAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CASE_DATA"), any(MultipartFile.class))).thenReturn(ATTACHMENT_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("file", "hello".getBytes()).filename("arendeuppgifter.pdf");
		final MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = builder.build();

		webTestClient.post()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "CASE_DATA").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(body)
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).createAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CASE_DATA"), any(MultipartFile.class));
	}

	@Test
	void readAttachments() {
		when(serviceMock.readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull())).thenReturn(List.of(Attachment.create()));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Attachment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull());
	}

	@Test
	void readAttachmentsFilteredByDocumentType() {
		when(serviceMock.readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CONVERSATION"), isNull())).thenReturn(List.of(Attachment.create()));

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "CONVERSATION").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CONVERSATION"), isNull());
	}

	@Test
	void readAttachmentsFilteredByCaseDataDocumentType() {
		when(serviceMock.readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CASE_DATA"), isNull())).thenReturn(List.of(Attachment.create()));

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "CASE_DATA").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).readAttachments(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CASE_DATA"), isNull());
	}

	@Test
	void readAttachment() {
		when(serviceMock.readAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID)).thenReturn(Attachment.create().withId(ATTACHMENT_ID));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Attachment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID);
	}

	@Test
	void streamAttachmentFile() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}/file").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).streamAttachmentFile(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(ATTACHMENT_ID), any());
	}

	@Test
	void deleteAttachment() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).deleteAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, ATTACHMENT_ID);
	}
}
