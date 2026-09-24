package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

/**
 * "Besluta och utbetala" against an engine that refuses the decision message, end to end: finalize records the decision
 * and queues PaymentDecisionReceived in the same transaction, and the scheduled retry delivers it once the engine takes
 * it. The unit tests cover each half with the other mocked; this runs both against a real database and the engine's
 * HTTP contract.
 *
 * <p>
 * Lives in the retry worker's package rather than {@code apptest} so it can drive {@link ProcessMessageRetryWorker}
 * directly: waiting for the minute cron would make the test slow and timing-dependent.
 * </p>
 */
@WireMockAppTestSuite(files = "classpath:/FinalizeProcessMessageRetryIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-finalize-it.sql"
})
class FinalizeProcessMessageRetryIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String ERRAND_ID = "66666666-6666-6666-6666-666666666666";
	private static final String PATH = "/2281/MY_NAMESPACE/errands/financial-assistance/" + ERRAND_ID + "/finalize";
	private static final String SENT_BY = "X-Sent-By";
	private static final String CASEWORKER = "joe01doe; type=adAccount";

	@Autowired
	private ProcessMessageRetryRepository retryRepository;

	@Autowired
	private FaPaymentRepository paymentRepository;

	@Autowired
	private ProcessMessageRetryWorker retryWorker;

	@Test
	void test01_finalizeQueuesTheMessageTheEngineRefused() {
		setupCall()
			.withServicePath(PATH)
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// The decision's payment is saved although the process was not reached ...
		assertThat(paymentRepository.findByErrandId(ERRAND_ID)).singleElement()
			.satisfies(payment -> assertThat(payment.getStatus()).isEqualTo("PENDING_REGISTRATION"));
		// ... and so is the message, waiting for the scheduled retry.
		assertThat(retryRepository.findAll()).singleElement().satisfies(retry -> {
			assertThat(retry.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(retry.getMessageName()).isEqualTo("PaymentDecisionReceived");
			assertThat(retry.getVariables()).contains("\"paymentDecision\"").contains("\"APPROVED\"");
			assertThat(retry.getStatus()).isEqualTo("PENDING");
			assertThat(retry.getAttempts()).isOne(); // finalize's own attempt counts
			assertThat(retry.getLastError()).isNotBlank();
			assertThat(retry.getNextAttempt()).isAfter(OffsetDateTime.now());
		});
	}

	@Test
	void test02_retryDeliversTheQueuedMessage() {
		setupCall()
			.withServicePath(PATH)
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
		// Both stubs hit: the refused first attempt from finalize and the delivered retry, with the same variables. The
		// OAuth token was fetched and cached by test01, so this test has no token stub.
		verifyStubs();
	}
}
