package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceApprovalService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceDraftRowService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceWarningService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;

import static java.util.UUID.randomUUID;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Shared harness for the financial assistance resource tests. Declaring the whole mocked-service set here — so every
 * concrete {Resource}Test / {Resource}FailureTest inherits the identical {@code @MockitoBean} configuration — means
 * they
 * all resolve to a single cached Spring context instead of one heavy full-app context per distinct mock set. The mocks
 * are {@code protected} (not {@code private}) so the shared, per-resource unused ones are not flagged as dead fields.
 */
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
abstract class AbstractFinancialAssistanceResourceTest {

	protected static final String MUNICIPALITY_ID = "2281";
	protected static final String NAMESPACE = "my-namespace";
	protected static final String ERRAND_ID = randomUUID().toString();
	protected static final String SLUG = "financial-assistance-new";
	protected static final String CREATE_PATH = "/{municipalityId}/{namespace}/errands/" + SLUG;
	protected static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@MockitoBean
	protected FinancialAssistanceService serviceMock;

	@MockitoBean
	protected EligibilityService eligibilityServiceMock;

	@MockitoBean
	protected RenewalPrefillService prefillServiceMock;

	@MockitoBean
	protected FinancialAssistanceWarningService warningServiceMock;

	@MockitoBean
	protected FinancialAssistanceApprovalService approvalServiceMock;

	@MockitoBean
	protected FinancialAssistanceDraftRowService draftRowServiceMock;

	@Autowired
	protected WebTestClient webTestClient;

	protected Map<String, ?> base() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE);
	}
}
