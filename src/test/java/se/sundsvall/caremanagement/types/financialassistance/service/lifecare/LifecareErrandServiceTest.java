package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.HouseholdPartyService;
import se.sundsvall.caremanagement.types.financialassistance.service.LifecareServiceIdService;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 24, null, null, null, 2026, 9);

	@Mock
	private ErrandService errandService;
	@Mock
	private FinancialAssistanceRepository financialAssistanceRepository;
	@Mock
	private FinancialAssistanceErrandService financialAssistanceErrandService;
	@Mock
	private LifecareServiceIdService lifecareServiceIdService;
	@Mock
	private HouseholdPartyService householdPartyService;
	@Mock
	private CitizenService citizenService;

	@InjectMocks
	private LifecareErrandService service;

	@Captor
	private ArgumentCaptor<FinancialAssistanceData> dataCaptor;

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void load() {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecareCalculationId(3).withLifecareDecisionId(4)
			.withPeriodYear(2026).withPeriodMonth(9);
		entity.setLifecarePaymentIds(List.of("p1"));
		when(financialAssistanceRepository.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(lifecareServiceIdService.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(24);

		final var errand = service.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandService).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(errand).isEqualTo(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 24, 3, 4, List.of("p1"), 2026, 9));
	}

	@Test
	void loadWithoutData() {
		when(financialAssistanceRepository.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		when(lifecareServiceIdService.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(null);

		assertThat(service.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isEqualTo(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, List.of(), null, null));
	}

	@Test
	void applicantPersonalNumber() {
		when(householdPartyService.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.of("party"), false, Optional.empty(), Optional.empty()));
		when(citizenService.getPersonalNumber(MUNICIPALITY_ID, "party")).thenReturn(Optional.of("199001012385"));

		assertThat(service.applicantPersonalNumber(ERRAND)).isEqualTo("199001012385");
	}

	@Test
	void applicantPersonalNumberUnresolved() {
		when(householdPartyService.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.empty(), false, Optional.empty(), Optional.empty()));

		assertThatThrownBy(() -> service.applicantPersonalNumber(ERRAND)).hasMessageContaining("could not be resolved");
	}

	@Test
	void coApplicantPresent() {
		when(householdPartyService.coApplicantPresent(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(true);

		assertThat(service.coApplicantPresent(ERRAND)).isTrue();
	}

	@Test
	void linkCalculationAndDecision() {
		service.linkCalculation(ERRAND, 3);
		service.linkDecision(ERRAND, 4);

		verify(financialAssistanceErrandService, org.mockito.Mockito.times(2)).updateData(any(), any(), any(), dataCaptor.capture());
		assertThat(dataCaptor.getAllValues().get(0).getLifecareCalculationId()).isEqualTo(3);
		assertThat(dataCaptor.getAllValues().get(1).getLifecareDecisionId()).isEqualTo(4);
	}

	@Test
	void linkPaymentKeepsTheExistingOnes() {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		entity.setLifecarePaymentIds(List.of("p1"));
		when(financialAssistanceRepository.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));

		service.linkPayment(ERRAND, "p2");

		verify(financialAssistanceErrandService).updateData(any(), any(), any(), dataCaptor.capture());
		assertThat(dataCaptor.getValue().getLifecarePaymentIds()).containsExactly("p1", "p2");
	}

	@Test
	void linkPaymentAlreadyLinked() {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		entity.setLifecarePaymentIds(List.of("p1"));
		when(financialAssistanceRepository.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));

		service.linkPayment(ERRAND, "p1");

		verify(financialAssistanceErrandService, never()).updateData(any(), any(), any(), any());
	}

	@Test
	void caller() {
		Identifier.set(Identifier.parse("joe01doe; type=adAccount"));

		assertThat(service.caller()).isEqualTo("joe01doe");
	}

	@Test
	void noCaller() {
		assertThatThrownBy(service::caller).hasMessageContaining("X-Sent-By");
	}
}
