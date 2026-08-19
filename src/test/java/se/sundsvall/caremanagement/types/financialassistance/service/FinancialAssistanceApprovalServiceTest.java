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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceApprovalServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

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
}
