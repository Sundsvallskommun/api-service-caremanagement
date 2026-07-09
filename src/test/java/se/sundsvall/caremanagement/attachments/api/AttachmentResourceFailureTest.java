package se.sundsvall.caremanagement.attachments.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

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

	private static MultiValueMap<String, HttpEntity<?>> multipartBody() {
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
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createAttachment.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createAttachment.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createAttachment_invalidDocumentType() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "NOT_A_TYPE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(multipartBody())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createAttachment.documentType", "must be one of: [ERRAND, CASE_DATA, DECISION]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachments.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachments.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachments.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_invalidDocumentType() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("documentType", "NOT_A_TYPE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachments.documentType", "must be one of: [APPLICATION, CONVERSATION, GENERATED, ERRAND, CASE_DATA, DECISION, MESSAGE_HISTORY]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachments_invalidSenderRole() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("senderRole", "NOT_A_ROLE").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachments.senderRole", "must be one of: [CLIENT, CASEWORKER]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachment.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachment.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readAttachment_badAttachmentIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readAttachment.attachmentId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void streamAttachmentFile_badAttachmentIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}/file").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("streamAttachmentFile.attachmentId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void streamAttachmentFile_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{attachmentId}/file").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("streamAttachmentFile.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteAttachment.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badNamespace() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteAttachment.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badErrandIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteAttachment.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteAttachment_badAttachmentIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{attachmentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "attachmentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteAttachment.attachmentId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}
}
