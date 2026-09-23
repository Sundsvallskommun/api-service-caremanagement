package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaContext;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class LifecareServiceIdServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String APPLICANT = "19800101T001";

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private RpaContextService rpaContextServiceMock;

	@Mock
	private ActualisationService actualisationServiceMock;

	@InjectMocks
	private LifecareServiceIdService service;

	@Test
	void storedInsatsIsReturnedWithoutLookingItUp() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withLifecareServiceId(7700)));

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(7700);
		verifyNoInteractions(rpaContextServiceMock, actualisationServiceMock);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void missingInsatsIsLookedUpAndStored() {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(rpaContextServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(new RpaContext("EB-26090001", APPLICANT, null));
		when(actualisationServiceMock.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(7700));

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(7700);
		verify(repositoryMock).save(entity);
		assertThat(entity.getLifecareServiceId()).isEqualTo(7700);
	}

	@Test
	void noOpenInsatsStoresNothing() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()));
		when(rpaContextServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(new RpaContext("EB-26090001", APPLICANT, null));
		when(actualisationServiceMock.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.empty());

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isNull();
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void unresolvableApplicantLooksNothingUp() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()));
		when(rpaContextServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(new RpaContext("EB-26090001", null, null));

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isNull();
		verifyNoInteractions(actualisationServiceMock);
	}

	@Test
	void failedLookupAnswersNullSoTheErrandStillOpens() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()));
		when(rpaContextServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(new RpaContext("EB-26090001", APPLICANT, null));
		when(actualisationServiceMock.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isNull();
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void errandWithoutDataAnswersNull() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		assertThat(service.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isNull();
		verifyNoInteractions(rpaContextServiceMock, actualisationServiceMock);
	}
}
