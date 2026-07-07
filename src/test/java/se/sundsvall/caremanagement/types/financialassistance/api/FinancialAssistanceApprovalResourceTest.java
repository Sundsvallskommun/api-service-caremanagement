package se.sundsvall.caremanagement.types.financialassistance.api;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialAssistanceApprovalResourceTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void getSectionApprovals() {
		final var approvals = SectionApprovals.create()
			.withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true))
			.withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false))
			.withDecision(SectionApproval.create().withSection("DECISION").withApproved(false));
		when(approvalServiceMock.getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, "errand-1")).thenReturn(approvals);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/approvals").build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApprovals.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getCalculation().isApproved()).isTrue();
		assertThat(response.getPayment().isApproved()).isFalse();
		verify(approvalServiceMock).getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, "errand-1");
	}

	@Test
	void setSectionApproval() {
		when(approvalServiceMock.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, "errand-1", "CALCULATION", true, "jane02doe"))
			.thenReturn(SectionApproval.create().withSection("CALCULATION").withApproved(true).withApprovedBy("jane02doe"));

		final var response = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(base()))
			.bodyValue(SectionApprovalRequest.create().withApproved(true).withApprovedBy("jane02doe"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(SectionApproval.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getSection()).isEqualTo("CALCULATION");
		assertThat(response.isApproved()).isTrue();
		assertThat(response.getApprovedBy()).isEqualTo("jane02doe");
		verify(approvalServiceMock).setSectionApproval(MUNICIPALITY_ID, NAMESPACE, "errand-1", "CALCULATION", true, "jane02doe");
	}

}
