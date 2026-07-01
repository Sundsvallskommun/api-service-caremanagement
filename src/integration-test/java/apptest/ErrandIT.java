package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageReadReceiptRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.document.integration.db.DocumentRepository;
import se.sundsvall.caremanagement.notes.integration.db.NoteRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/ErrandIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String EXISTING_ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String NOT_FOUND_ERRAND_ID = "33333333-3333-3333-3333-333333333333";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands";

	// Dedicated errand carrying child rows (message + attachment/blob/read-receipt, note, document), used to prove
	// ON DELETE CASCADE cleanup on errand deletion. Kept separate from EXISTING_ERRAND_ID so the seeded message
	// attachment does not leak into ErrandAttachmentIT (the attachment listing aggregates conversation attachments).
	private static final String CASCADE_ERRAND_ID = "44444444-4444-4444-4444-444444444444";
	private static final String MESSAGE_ID = "cccccccc-cccc-cccc-cccc-cccccccccc01";
	private static final String MESSAGE_ATTACHMENT_ID = "cccccccc-cccc-cccc-cccc-ccccccccca01";
	private static final int MESSAGE_ATTACHMENT_DATA_ID = 1;
	private static final String MESSAGE_READ_RECEIPT_ID = "cccccccc-cccc-cccc-cccc-cccccccccr01";
	private static final String NOTE_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbn01";
	private static final String DOCUMENT_ID = "ffffffff-ffff-ffff-ffff-ffffffffff01";

	@Autowired
	private ErrandRepository repository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private MessageAttachmentRepository messageAttachmentRepository;

	@Autowired
	private MessageAttachmentDataRepository messageAttachmentDataRepository;

	@Autowired
	private MessageReadReceiptRepository messageReadReceiptRepository;

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private DocumentRepository documentRepository;

	@Test
	void test01_createErrand() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/[a-f0-9-]+$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readErrand() {
		setupCall()
			.withServicePath(PATH + "/" + EXISTING_ERRAND_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_findErrands() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateErrand() {
		setupCall()
			.withServicePath(PATH + "/" + EXISTING_ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteErrand() {
		assertThat(repository.existsById(EXISTING_ERRAND_ID)).isTrue();

		setupCall()
			.withServicePath(PATH + "/" + EXISTING_ERRAND_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsById(EXISTING_ERRAND_ID)).isFalse();
	}

	@Test
	void test06_readErrandNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + NOT_FOUND_ERRAND_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_deleteErrandNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + NOT_FOUND_ERRAND_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_deleteErrandCascadesToChildren() {
		// Errand-child rows exist up front (seeded): message + its attachment/blob/read-receipt, note, document.
		assertThat(messageRepository.existsById(MESSAGE_ID)).isTrue();
		assertThat(messageAttachmentRepository.existsById(MESSAGE_ATTACHMENT_ID)).isTrue();
		assertThat(messageAttachmentDataRepository.existsById(MESSAGE_ATTACHMENT_DATA_ID)).isTrue();
		assertThat(messageReadReceiptRepository.existsById(MESSAGE_READ_RECEIPT_ID)).isTrue();
		assertThat(noteRepository.existsById(NOTE_ID)).isTrue();
		assertThat(documentRepository.existsById(DOCUMENT_ID)).isTrue();

		setupCall()
			.withServicePath(PATH + "/" + CASCADE_ERRAND_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.existsById(CASCADE_ERRAND_ID)).isFalse();
		// ON DELETE CASCADE removed every errand-child row — no orphaned message/attachment/note/document data.
		assertThat(messageRepository.existsById(MESSAGE_ID)).isFalse();
		assertThat(messageAttachmentRepository.existsById(MESSAGE_ATTACHMENT_ID)).isFalse();
		assertThat(messageAttachmentDataRepository.existsById(MESSAGE_ATTACHMENT_DATA_ID)).isFalse();
		assertThat(messageReadReceiptRepository.existsById(MESSAGE_READ_RECEIPT_ID)).isFalse();
		assertThat(noteRepository.existsById(NOTE_ID)).isFalse();
		assertThat(documentRepository.existsById(DOCUMENT_ID)).isFalse();
	}
}
