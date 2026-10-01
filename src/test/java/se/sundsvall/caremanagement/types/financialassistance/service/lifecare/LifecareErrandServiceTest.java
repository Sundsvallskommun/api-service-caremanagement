package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
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

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void load() {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecareCalculationId(3).withLifecareDecisionId(4)
			.withPeriodYear(2026).withPeriodMonth(9);
		entity.setLifecarePaymentIds(List.of("p1"));
		when(financialAssistanceRepository.findWithLifecarePaymentIdsByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(lifecareServiceIdService.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(24);

		final var errand = service.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandService).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(errand).isEqualTo(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 24, 3, 4, List.of("p1"), 2026, 9));
	}

	@Test
	void loadWithoutData() {
		when(financialAssistanceRepository.findWithLifecarePaymentIdsByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
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
	void applicantPartyId() {
		when(householdPartyService.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.of("party"), false, Optional.empty(), Optional.empty()));

		assertThat(service.applicantPartyId(ERRAND)).isEqualTo("party");
	}

	@Test
	void applicantPartyIdMissing() {
		when(householdPartyService.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.empty(), false, Optional.empty(), Optional.empty()));

		assertThatThrownBy(() -> service.applicantPartyId(ERRAND)).hasMessageContaining("no applicant");
	}

	@Test
	void linkCalculationAndDecision() {
		service.linkCalculation(ERRAND, 3);
		service.linkDecision(ERRAND, 4);

		// Through the dedicated write-once links, never through the client-facing data update.
		verify(financialAssistanceErrandService).linkCalculation(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 3);
		verify(financialAssistanceErrandService).linkDecision(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 4);
		verify(financialAssistanceErrandService, never()).updateData(any(), any(), any(), any());
	}

	@Test
	void lockTakesTheErrandsRowLock() {
		service.lock(ERRAND);

		verify(financialAssistanceRepository).findByErrandIdForUpdate(ERRAND_ID);
	}

	@Test
	void linkPaymentInsertsTheIdAtomicallyInsteadOfReadModifyWrite() {
		service.linkPayment(ERRAND, "p2");

		verify(financialAssistanceRepository).linkPaymentIfAbsent(ERRAND_ID, "p2");
		verify(financialAssistanceErrandService, never()).updateData(any(), any(), any(), any());
	}

	@Test
	void linkPaymentAlreadyLinkedIsANoOp() {
		// The underlying insert is INSERT IGNORE against the (errand_id, lifecare_payment_id) primary key, so a payment
		// already linked is silently a no-op — nothing here needs to read the existing list first.
		service.linkPayment(ERRAND, "p1");

		verify(financialAssistanceRepository).linkPaymentIfAbsent(ERRAND_ID, "p1");
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
