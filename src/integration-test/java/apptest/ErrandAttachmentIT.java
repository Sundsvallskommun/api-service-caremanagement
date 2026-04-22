package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.integration.db.AttachmentRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/ErrandAttachmentIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandAttachmentIT extends AbstractAppTest {

	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String UNKNOWN_ERRAND_ID = "33333333-3333-3333-3333-333333333333";
	private static final String ATTACHMENT_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";
	private static final String UNKNOWN_ATTACHMENT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
	private static final String BASE_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/attachments";

	@Autowired
	private AttachmentRepository attachmentRepository;

	@Test
	void test01_readAttachments() {
		setupCall()
			.withServicePath(BASE_PATH)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readAttachment() {
		setupCall()
			.withServicePath(BASE_PATH + "/" + ATTACHMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_streamAttachmentFile() throws Exception {
		setupCall()
			.withServicePath(BASE_PATH + "/" + ATTACHMENT_ID + "/file")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse("hello.txt")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteAttachment() {
		assertThat(attachmentRepository.existsById(ATTACHMENT_ID)).isTrue();

		setupCall()
			.withServicePath(BASE_PATH + "/" + ATTACHMENT_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(attachmentRepository.existsById(ATTACHMENT_ID)).isFalse();
	}

	@Test
	void test05_readAttachmentNotFound() {
		setupCall()
			.withServicePath(BASE_PATH + "/" + UNKNOWN_ATTACHMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_readAttachmentInUnknownErrand() {
		setupCall()
			.withServicePath("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + UNKNOWN_ERRAND_ID + "/attachments/" + ATTACHMENT_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
