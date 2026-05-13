package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.notifications.integration.db.NotificationRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/NotificationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class NotificationIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String UNKNOWN_ERRAND_ID = "33333333-3333-3333-3333-333333333333";
	private static final String NOTIFICATION_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
	private static final String UNKNOWN_NOTIFICATION_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
	private static final String ERRAND_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/%s/notifications";
	private static final String OWNER_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/notifications";

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void test01_createNotification() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/11111111-1111-1111-1111-111111111111/notifications/[a-f0-9-]+$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readNotifications() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_readNotification() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID) + "/" + NOTIFICATION_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateNotification() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID) + "/" + NOTIFICATION_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(notificationRepository.findById(NOTIFICATION_ID))
			.get()
			.extracting("acknowledged").isEqualTo(true);
	}

	@Test
	void test05_deleteNotification() {
		assertThat(notificationRepository.existsById(NOTIFICATION_ID)).isTrue();

		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID) + "/" + NOTIFICATION_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(notificationRepository.existsById(NOTIFICATION_ID)).isFalse();
	}

	@Test
	void test06_acknowledgeAll() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID) + "/acknowledged")
			.withHttpMethod(PUT)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(notificationRepository.findAll()).allMatch(n -> n.isAcknowledged());
	}

	@Test
	void test07_readNotificationsByOwner() {
		setupCall()
			.withServicePath(OWNER_PATH + "?ownerId=assignee1")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_readNotificationNotFound() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(ERRAND_ID) + "/" + UNKNOWN_NOTIFICATION_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test09_readNotificationInUnknownErrand() {
		setupCall()
			.withServicePath(ERRAND_PATH.formatted(UNKNOWN_ERRAND_ID) + "/" + NOTIFICATION_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
