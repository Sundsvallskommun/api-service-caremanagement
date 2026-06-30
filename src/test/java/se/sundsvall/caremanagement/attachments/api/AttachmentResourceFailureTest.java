package se.sundsvall.caremanagement.attachments.api;

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
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class AttachmentResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/attachments";

	@MockitoBean
	private AttachmentService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartBody() {
		final var builder = new MultipartBodyBuilder();
		builder.part("file", "hello".getBytes()).filename("hello.txt");
		return builder.build();
	}

	@Test
	void createAttachment_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_invalidDocumentType() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "NOT_A_TYPE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_invalidDocumentType() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "NOT_A_TYPE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_invalidSenderRole() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("senderRole", "NOT_A_ROLE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badAttachmentIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void streamAttachmentFile_badAttachmentIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}/file").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void streamAttachmentFile_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}/file").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badErrandIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badAttachmentIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
