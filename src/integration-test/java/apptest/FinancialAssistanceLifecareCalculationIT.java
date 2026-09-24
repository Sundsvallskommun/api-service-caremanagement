package apptest;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;

/**
 * The contract between Draken and careM around the Lifecare normberäkning: Draken saves the calculation in Lifecare and
 * PATCHes its id onto the errand as {@code lifecareCalculationId}; from then on the daily prepare leaves careM's draft
 * alone, and only then may a granting decision be finalized.
 *
 * <p>
 * One application type (återansökan) is covered: none of these paths — the data PATCH, the draft refresh or the
 * finalize guards — reads the application type, so a nyansökan takes exactly the same route.
 * </p>
 */
@WireMockAppTestSuite(files = "classpath:/FinancialAssistanceLifecareCalculationIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-lifecare-calculation-it.sql"
})
class FinancialAssistanceLifecareCalculationIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String ERRAND_ID = "77777777-7777-7777-7777-777777777777";
	private static final String APPLICANT_PARTY_ID = "aaaaaaaa-0000-4000-8000-000000000001";
	private static final String PATH = "/2281/MY_NAMESPACE/errands/financial-assistance";
	private static final String ERRAND_PATH = PATH + "/" + ERRAND_ID;
	private static final String SENT_BY = "X-Sent-By";
	private static final String CASEWORKER = "joe01doe; type=adAccount";
	private static final String PREPARE_REQUEST = """
		{
			"errandId": "%s",
			"applicant": "%s",
			"applicationMonth": "2026-10",
			"classifiedIncomes": "[]"
		}""".formatted(ERRAND_ID, APPLICANT_PARTY_ID);

	@Autowired
	private FinancialAssistanceRepository financialAssistanceRepository;

	@Autowired
	private FaWarningRepository warningRepository;

	@Autowired
	private DecisionRepository decisionRepository;

	@Autowired
	private FaPaymentRepository paymentRepository;

	@Test
	void test01_patchLifecareCalculationIdAndReadItBack() {
		setupCall()
			.withServicePath(ERRAND_PATH + "/data")
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();

		setupCall()
			.withServicePath(ERRAND_PATH)
			.withHttpMethod(GET)
			.withJsonAssertOptions(List.of(IGNORING_EXTRA_FIELDS, IGNORING_ARRAY_ORDER))
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		assertThat(financialAssistanceRepository.findByErrandId(ERRAND_ID)).hasValueSatisfying(entity -> assertThat(entity.getLifecareCalculationId()).isEqualTo(4711));
	}

	@Test
	void test02_prepareRefreshesTheDraftUntilTheCalculationIsSavedInLifecare() throws JacksonException {
		// Run 1, no lifecareCalculationId: the draft is built from the application — the applicant and the rent.
		prepare();
		assertThat(draft().getExpenses()).extracting(NormExpenseRow::getCostType).containsExactly("RENT");

		// The application gains an electricity cost; run 2 still refreshes the draft, which picks it up.
		patchData("""
			{"costs": [{"costType": "RENT", "appliedAmount": 6500}, {"costType": "ELECTRICITY", "appliedAmount": 400}]}""");
		prepare();
		final var refreshed = draft();
		assertThat(refreshed.getExpenses()).extracting(NormExpenseRow::getCostType).containsExactlyInAnyOrder("RENT", "ELECTRICITY");
		final var warningsBefore = warningSnapshot();

		// Draken saves the normberäkning in Lifecare and sets its id — and the application changes once more.
		patchData("""
			{"lifecareCalculationId": 4711, "costs": [{"costType": "RENT", "appliedAmount": 6500}, {"costType": "ELECTRICITY", "appliedAmount": 400}, {"costType": "HOME_INSURANCE", "appliedAmount": 150}]}""");
		prepare();

		// Run 3: the Lifecare calculation is the truth, so the draft and its warnings are exactly as run 2 left them.
		assertThat(draft()).usingRecursiveComparison().isEqualTo(refreshed);
		assertThat(warningSnapshot()).containsExactlyInAnyOrderElementsOf(warningsBefore);
	}

	@Test
	void test03_finalizeBifallWithoutLifecareCalculationIdIsRejected() {
		setupCall()
			.withServicePath(ERRAND_PATH + "/finalize")
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		// Nothing is recorded: no decision, no payment.
		assertThat(decisionRepository.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).isEmpty();
		assertThat(paymentRepository.findByErrandId(ERRAND_ID)).isEmpty();
	}

	@Test
	void test04_finalizeBifallWithLifecareCalculationId() {
		patchData("""
			{"lifecareCalculationId": 4711}""");

		// The OAuth token may already be cached by an earlier test in this context, so the stubs are not verified
		// one by one; processMessageCorrelated=true in the response is the engine stub answering.
		setupCall()
			.withServicePath(ERRAND_PATH + "/finalize")
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		assertThat(decisionRepository.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).extracting(DecisionEntity::getDecisionType, DecisionEntity::getValue)
			.containsExactly(tuple("PAYMENT", "BIFALL"));
		assertThat(paymentRepository.findByErrandId(ERRAND_ID)).hasSize(1);
	}

	@Test
	void test05_finalizeAvslagWithoutLifecareCalculationId() {
		// An avslag pays nothing, so it needs no normberäkning in Lifecare.
		setupCall()
			.withServicePath(ERRAND_PATH + "/finalize")
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		assertThat(decisionRepository.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).extracting(DecisionEntity::getDecisionType, DecisionEntity::getValue)
			.containsExactly(tuple("PAYMENT", "AVSLAG"));
		assertThat(paymentRepository.findByErrandId(ERRAND_ID)).isEmpty();
	}

	/**
	 * One daily prepare run. The rule tables in the engine are not stubbed — every DMN evaluation is best-effort and
	 * degrades to "no rule warning" — so the stubs are not verified here.
	 */
	private void prepare() {
		setupCall()
			.withServicePath(PATH + "/calculation/prepare")
			.withHttpMethod(POST)
			.withRequest(PREPARE_REQUEST)
			.withExpectedResponseStatus(OK)
			.sendRequest();
	}

	private void patchData(final String data) {
		setupCall()
			.withServicePath(ERRAND_PATH + "/data")
			.withHttpMethod(PATCH)
			.withRequest(data)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequest();
	}

	private CalculationDraft draft() throws JacksonException {
		return setupCall()
			.withServicePath(ERRAND_PATH + "/calculation/draft")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.sendRequest()
			.andReturnBody(new TypeReference<CalculationDraft>() {});
	}

	private record WarningSnapshot(String type, String sourceKey, String status, String message) {}

	private List<WarningSnapshot> warningSnapshot() {
		return warningRepository.findByErrandId(ERRAND_ID).stream()
			.map(warning -> new WarningSnapshot(warning.getType(), warning.getSourceKey(), warning.getStatus(), warning.getMessage()))
			.toList();
	}
}
