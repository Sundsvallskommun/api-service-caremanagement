package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceApprovalService;
import se.sundsvall.dept44.support.Identifier;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceApprovalResourceTest {
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private FinancialAssistanceApprovalService approvalServiceMock;

	@Test
	void getSectionApprovals() {
		final var approvals = SectionApprovals.create()
			.withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true))
			.withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false))
			.withDecision(SectionApproval.create().withSection("DECISION").withApproved(false));
		when(approvalServiceMock.getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(approvals);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/sections/approvals").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApprovals.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculation().isApproved()).isTrue();
		assertThat(response.getPayment().isApproved()).isFalse();
		verify(approvalServiceMock).getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void setSectionApproval() {
		when(approvalServiceMock.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CALCULATION", true, "jane02doe"))
			.thenReturn(SectionApproval.create().withSection("CALCULATION").withApproved(true).withApprovedBy("jane02doe"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/sections/CALCULATION/approval").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.header(Identifier.HEADER_NAME, "jane02doe; type=adAccount") // approver comes from X-Sent-By, not the body
			.bodyValue(SectionApprovalRequest.create().withApproved(true))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApproval.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getSection()).isEqualTo("CALCULATION");
		assertThat(response.isApproved()).isTrue();
		assertThat(response.getApprovedBy()).isEqualTo("jane02doe");
		verify(approvalServiceMock).setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CALCULATION", true, "jane02doe");
	}

}
