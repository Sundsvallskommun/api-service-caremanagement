package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.PAYMENT_PROPOSAL_TYPES;

@ExtendWith(MockitoExtension.class)
class PaymentWarningServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private HouseholdPartyService householdPartyServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@InjectMocks
	private PaymentWarningService service;

	@Test
	void coApplicantRaisesTheSplitPaymentWarning() {
		when(householdPartyServiceMock.coApplicantPresent(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(true);
		final var reconciled = List.of(Warning.create().withType("CO_APPLICANT_SPLIT_PAYMENT"));
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, List.of(new WarningService.WarningInput("CO_APPLICANT_SPLIT_PAYMENT", "co-applicant",
			"Det finns medsökande i ärendet – kontrollera om det ska vara delad utbetalning")))).thenReturn(reconciled);

		assertThat(service.reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(reconciled);
	}

	@Test
	void noCoApplicantClosesTheWarning() {
		when(householdPartyServiceMock.coApplicantPresent(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(false);
		when(warningServiceMock.reconcileByTypes(ERRAND_ID, PAYMENT_PROPOSAL_TYPES, List.of())).thenReturn(List.of());

		assertThat(service.reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}
}
