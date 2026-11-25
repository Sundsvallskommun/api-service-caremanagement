package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Withdrawing a financial assistance errand ends its process, end to end: the status change publishes
 * {@code ErrandStatusChanged}, the listener correlates {@code ErrandWithdrawn} once the change has committed, and a
 * message the engine cannot take is queued for the scheduled retry. The unit tests cover each half with the other
 * mocked.
 */
@WireMockAppTestSuite(files = "classpath:/WithdrawalProcessMessageIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-withdrawal-it.sql"
})
class WithdrawalProcessMessageIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String ERRAND_ID = "77777777-7777-7777-7777-777777777777";
	private static final String PATH = "/2281/MY_NAMESPACE/errands/" + ERRAND_ID;

	@Autowired
	private ProcessMessageRetryRepository retryRepository;

	@Test
	void test01_withdrawingAnErrandEndsItsProcess() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		// The engine took the message at once, so nothing waits for a retry.
		assertThat(retryRepository.findAll()).isEmpty();
	}

	@Test
	void test02_anEngineThatIsDownQueuesTheWithdrawal() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest()
			.andVerifyThat(() -> retryRepository.count() == 1)
			.verifyStubs();

		// The status change went through although the process was not reached, and the message waits for the retry.
		assertThat(retryRepository.findAll()).singleElement().satisfies(retry -> {
			assertThat(retry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(retry.getMessageName()).isEqualTo("ErrandWithdrawn");
			assertThat(retry.getVariables()).isEqualTo("{}");
			assertThat(retry.getStatus()).isEqualTo("PENDING");
			assertThat(retry.getLastError()).isNotBlank();
			assertThat(retry.getNextAttempt()).isAfter(OffsetDateTime.now());
		});
	}
}
