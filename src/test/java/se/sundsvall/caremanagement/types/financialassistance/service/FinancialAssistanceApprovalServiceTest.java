package se.sundsvall.caremanagement.types.financialassistance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceApprovalServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

	@Mock
	private DecisionProposalService decisionProposalServiceMock;

	@Mock
	private PaymentProposalService paymentProposalServiceMock;

	@InjectMocks
	private FinancialAssistanceApprovalService service;

	@Test
	void getSectionApprovalsScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var approvals = SectionApprovals.create().withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals);

		assertThat(service.getSectionApprovals(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(approvals);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void setSectionApprovalScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var approval = SectionApproval.create().withSection("DECISION").withApproved(true).withApprovedBy("jane02doe");
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "DECISION", true, "jane02doe")).thenReturn(approval);

		assertThat(service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "DECISION", true, "jane02doe")).isSameAs(approval);
		verify(sectionApprovalServiceMock).setApproval(ERRAND_ID, "DECISION", true, "jane02doe");
	}

	@Test
	void approvingCalculationRecomputesTheDecisionProposal() {
		final var approval = SectionApproval.create().withSection("CALCULATION").withApproved(true);
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "CALCULATION", true, "jane02doe")).thenReturn(approval);

		assertThat(service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CALCULATION", true, "jane02doe")).isSameAs(approval);
		verify(decisionProposalServiceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(paymentProposalServiceMock);
	}

	@Test
	void approvingDecisionRecomputesThePaymentProposalBestEffort() {
		final var approval = SectionApproval.create().withSection("DECISION").withApproved(true);
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "DECISION", true, "jane02doe")).thenReturn(approval);
		when(paymentProposalServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));

		assertThat(service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "DECISION", true, "jane02doe")).isSameAs(approval); // never fails the approval
		verifyNoInteractions(decisionProposalServiceMock);
	}

	@Test
	void withdrawingOrApprovingPaymentRecomputesNothing() {
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "CALCULATION", false, null)).thenReturn(SectionApproval.create());
		when(sectionApprovalServiceMock.setApproval(ERRAND_ID, "PAYMENT", true, "jane02doe")).thenReturn(SectionApproval.create());

		service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CALCULATION", false, null);
		service.setSectionApproval(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "PAYMENT", true, "jane02doe");

		verifyNoInteractions(decisionProposalServiceMock, paymentProposalServiceMock);
	}
}
