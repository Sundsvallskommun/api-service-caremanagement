package se.sundsvall.caremanagement.types.financialassistance.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceDraftRowServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private DraftService draftServiceMock;

	@InjectMocks
	private FinancialAssistanceDraftRowService service;

	@Test
	void incomeRowEditsScopeCheckThenDelegate() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var row = NormIncomeRow.create().withId("r1");
		final var input = new NormIncomeInput().withTypeId(20);
		when(draftServiceMock.addIncome(eq(ERRAND_ID), any())).thenReturn(row);
		when(draftServiceMock.patchIncome(eq(ERRAND_ID), eq("r1"), any())).thenReturn(row);
		when(draftServiceMock.setIncomeDeleted(ERRAND_ID, "r1", true)).thenReturn(row);
		when(draftServiceMock.setIncomeDeleted(ERRAND_ID, "r1", false)).thenReturn(row);

		assertThat(service.addDraftIncome(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input)).isSameAs(row);
		assertThat(service.patchDraftIncome(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", input)).isSameAs(row);
		assertThat(service.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", true)).isSameAs(row);
		assertThat(service.setDraftIncomeDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "r1", false)).isSameAs(row);
		verify(errandServiceMock, times(4)).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void expenseRowEditsScopeCheckThenDelegate() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var row = NormExpenseRow.create().withId("e1");
		final var input = new NormExpenseInput();
		when(draftServiceMock.addExpense(eq(ERRAND_ID), any())).thenReturn(row);
		when(draftServiceMock.patchExpense(eq(ERRAND_ID), eq("e1"), any())).thenReturn(row);
		when(draftServiceMock.setExpenseDeleted(ERRAND_ID, "e1", true)).thenReturn(row);
		when(draftServiceMock.setExpenseDeleted(ERRAND_ID, "e1", false)).thenReturn(row);

		assertThat(service.addDraftExpense(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input)).isSameAs(row);
		assertThat(service.patchDraftExpense(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "e1", input)).isSameAs(row);
		assertThat(service.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "e1", true)).isSameAs(row);
		assertThat(service.setDraftExpenseDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "e1", false)).isSameAs(row);
		verify(errandServiceMock, times(4)).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void personRowEditsScopeCheckThenDelegate() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var row = NormPersonRow.create().withId("p1");
		final var input = new NormPersonInput();
		when(draftServiceMock.addPerson(eq(ERRAND_ID), any())).thenReturn(row);
		when(draftServiceMock.patchPerson(eq(ERRAND_ID), eq("p1"), any())).thenReturn(row);
		when(draftServiceMock.setPersonDeleted(ERRAND_ID, "p1", true)).thenReturn(row);
		when(draftServiceMock.setPersonDeleted(ERRAND_ID, "p1", false)).thenReturn(row);

		assertThat(service.addDraftPerson(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input)).isSameAs(row);
		assertThat(service.patchDraftPerson(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "p1", input)).isSameAs(row);
		assertThat(service.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "p1", true)).isSameAs(row);
		assertThat(service.setDraftPersonDeleted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "p1", false)).isSameAs(row);
		verify(errandServiceMock, times(4)).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}
}
