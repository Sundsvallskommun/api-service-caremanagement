package apptest;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry.ProcessMessageRetryWorker;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

/**
 * The process messages careM sends the engine, end to end: withdrawing an errand correlates {@code ErrandWithdrawn}
 * once the status change has committed, finalize correlates {@code PaymentDecisionReceived}, and a message the engine
 * cannot take is queued and delivered by the scheduled retry. The retry worker is driven directly, since waiting for
 * the minute cron would make the test slow and timing-dependent.
 */
@WireMockAppTestSuite(files = "classpath:/ProcessMessageIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-finalize-it.sql",
	"/db/scripts/testdata-withdrawal-it.sql"
})
class ProcessMessageIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String FINALIZE_ERRAND_ID = "66666666-6666-6666-6666-666666666666";
	private static final String WITHDRAWAL_ERRAND_ID = "77777777-7777-7777-7777-777777777777";
	private static final String SENT_BY = "X-Sent-By";
	private static final String CASEWORKER = "joe01doe; type=adAccount";

	@Autowired
	private ProcessMessageRetryRepository retryRepository;

	@Autowired
	private ProcessMessageRetryWorker retryWorker;

	@Test
	void test01_withdrawingAnErrandEndsItsProcess() {
		setupCall()
			.withServicePath("/2281/MY_NAMESPACE/errands/" + WITHDRAWAL_ERRAND_ID)
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
			.withServicePath("/2281/MY_NAMESPACE/errands/" + WITHDRAWAL_ERRAND_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest()
			.andVerifyThat(() -> retryRepository.count() == 1)
			.verifyStubs();

		// The status change went through although the process was not reached, and the message waits for the retry.
		assertThat(retryRepository.findAll()).singleElement().satisfies(retry -> {
			assertThat(retry.getErrandId()).isEqualTo(WITHDRAWAL_ERRAND_ID);
			assertThat(retry.getMessageName()).isEqualTo("ErrandWithdrawn");
			assertThat(retry.getVariables()).isEqualTo("{}");
			assertThat(retry.getStatus()).isEqualTo("PENDING");
			assertThat(retry.getLastError()).isNotBlank();
			assertThat(retry.getNextAttempt()).isAfter(OffsetDateTime.now());
		});
	}

	@Test
	void test03_finalizeRetryDeliversTheQueuedMessage() {
		setupCall()
			.withServicePath("/2281/MY_NAMESPACE/errands/financial-assistance/" + FINALIZE_ERRAND_ID + "/finalize")
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.sendRequest();

		// Not due yet: the worker leaves it alone.
		assertThat(retryWorker.retryDue().attempted()).isZero();

		// Make it due, as the minute scheduler would find it a minute later. The engine now accepts.
		final var queued = retryRepository.findAll().getFirst();
		retryRepository.save(queued.withNextAttempt(OffsetDateTime.now().minusSeconds(1)));

		final var result = retryWorker.retryDue();

		assertThat(result.attempted()).isOne();
		assertThat(result.delivered()).isOne();
		assertThat(retryRepository.findAll()).isEmpty();
		// Both stubs hit: the refused first attempt from finalize and the delivered retry, with the same variables.
		verifyStubs();
	}
}
