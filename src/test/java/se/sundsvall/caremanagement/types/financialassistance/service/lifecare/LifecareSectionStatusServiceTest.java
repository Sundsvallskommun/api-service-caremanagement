package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareSectionStatus;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapperTest.tree;

/**
 * Ported from the Draken BFF's errand-lifecare-section-status.service.test.ts, with the payment reading of
 * errand-lifecare-payments.service.ts (paymentStatus) inlined.
 */
@ExtendWith(MockitoExtension.class)
class LifecareSectionStatusServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";

	private static final Map<String, String> LIST_PARAMS = Map.of("businessType", "8", "businessId", "1", "investigationId", "0", "serviceId", "1",
		"onlylatest", "true");
	private static final Map<String, String> PAYMENT_PARAMS = Map.of("businessType", "8", "businessId", "1");

	private static final String CALCULATIONS = """
		[{"calculationId": 31, "date": "2026-09-24", "startDate": "2026-09-01", "endDate": "2026-09-30", "isFinalized": true},
		 {"calculationId": 29, "date": "2026-09-23", "startDate": "2026-12-01", "endDate": "2026-12-31", "isFinalized": false}]""";

	private static final String PAYMENTS = """
		[{"paymentId": 1, "amount": 3000, "payDate": "2026-09-25", "concernedMonth": "202609", "accountNumber": "1234", "cancellationDate": ""},
		 {"paymentId": 2, "amount": 3000, "payDate": "2026-08-25", "concernedMonth": "202608", "accountNumber": "1234", "cancellationDate": ""}]""";

	@Mock
	private LifecareErrandService errandServiceMock;

	@Mock
	private ProfessionalWebClient clientMock;

	@Mock
	private LifecareAccessRecorder accessRecorderMock;

	@InjectMocks
	private LifecareSectionStatusService service;

	private LifecareErrand errand(final Integer serviceId, final Integer calculationId, final Integer decisionId, final Integer year, final Integer month) {
		final var errand = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, serviceId, calculationId, decisionId, null, year, month);
		when(errandServiceMock.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);
		return errand;
	}

	@Test
	void checksEachSectionByItsStateInLifecare() {
		final var errand = errand(1, 31, 98, 2026, 9);
		when(clientMock.get("api2/Calculation/ListCalculations", LIST_PARAMS)).thenReturn(tree(CALCULATIONS));
		when(clientMock.get("api2/Payment/GetLatestPayments", PAYMENT_PARAMS)).thenReturn(tree(PAYMENTS));

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecareSectionStatus(true, true, true));
		verify(accessRecorderMock).read(errand, "CALCULATION", "Läste insatsens normberäkningar i Lifecare");
		verify(accessRecorderMock).read(errand, "PAYMENTS", "Läste utbetalningar i Lifecare");
	}

	@Test
	void leavesABerakningStillBeingWorkedOnABeslutNotYetSavedAndAnotherMonthUnchecked() {
		errand(1, 29, null, 2026, 10);
		when(clientMock.get("api2/Calculation/ListCalculations", LIST_PARAMS)).thenReturn(tree(CALCULATIONS));
		when(clientMock.get("api2/Payment/GetLatestPayments", PAYMENT_PARAMS)).thenReturn(tree(PAYMENTS));

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecareSectionStatus(false, false, false));
	}

	@Test
	void doesNotCountAMakuleradUtbetalning() {
		errand(1, null, null, 2026, 9);
		when(clientMock.get("api2/Payment/GetLatestPayments", PAYMENT_PARAMS)).thenReturn(tree("""
			[{"paymentId": 1, "payDate": "2026-09-25", "concernedMonth": "202609", "cancellationDate": "2026-09-26"},
			 {"paymentId": 2, "payDate": "2026-09-25", "concernedMonth": "202609", "cancellationDate": null},
			 {"paymentId": 3, "payDate": "2026-09-25", "concernedMonth": null, "cancellationDate": ""}]"""));

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).paymentRegistered()).isFalse();
	}

	@Test
	void leavesACheckOffWhenLifecareCannotBeReadWithoutFailingTheOthers() {
		errand(1, 31, 98, 2026, 9);
		when(clientMock.get("api2/Calculation/ListCalculations", LIST_PARAMS)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));
		when(clientMock.get("api2/Payment/GetLatestPayments", PAYMENT_PARAMS)).thenReturn(tree(PAYMENTS));

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecareSectionStatus(false, true, true));
	}

	@Test
	void leavesTheUtbetalningOffWhenItsReadCannotBeLogged() {
		final var errand = errand(1, null, null, 2026, 9);
		when(clientMock.get("api2/Payment/GetLatestPayments", PAYMENT_PARAMS)).thenReturn(tree(PAYMENTS));
		doThrow(new IllegalStateException("log down")).when(accessRecorderMock).read(errand, "PAYMENTS", "Läste utbetalningar i Lifecare");

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecareSectionStatus(false, false, false));
	}

	@Test
	void readsNothingFromLifecareWithoutInsatsOrMonth() {
		errand(null, 31, 98, null, null);

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecareSectionStatus(false, true, false));
		verifyNoInteractions(clientMock, accessRecorderMock);
	}

	@Test
	void readsNoUtbetalningarWithoutAMonth() {
		errand(1, null, null, 2026, null);

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).paymentRegistered()).isFalse();
		verify(clientMock, never()).get(anyString(), any());
	}
}
