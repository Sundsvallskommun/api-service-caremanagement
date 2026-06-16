package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.NormberakningService;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_INKOMMEN;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private NormberakningService normberakningServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private FinancialAssistanceService service;

	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

	@Test
	void createBuildsEnvelopeAndSavesData() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		// Client sends a mismatched applicationType — the slug must win.
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Min ansökan")
			.withData(FinancialAssistanceData.create().withApplicationType("SUPPLEMENTARY"));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request);

		assertThat(result).isEqualTo(ERRAND_ID);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_NEW);
		assertThat(errandCaptor.getValue().getStatus()).isEqualTo(STATUS_INKOMMEN);
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Min ansökan");

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("NEW"); // derived from SLUG_NEW, not the client value
	}

	@Test
	void createDerivesApplicationTypeFromSlug() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Återansökan").withData(FinancialAssistanceData.create());

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_RENEWAL);

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");
	}

	@Test
	void createDefaultsTitleWhenMissing() {
		when(errandServiceMock.createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withData(FinancialAssistanceData.create().withApplicationType("NEW"));

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Ekonomiskt bistånd");
	}

	@Test
	void readAssemblesView() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("INKOMMEN"));
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("NEW")));

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNotNull();
		assertThat(view.getData().getApplicationType()).isEqualTo("NEW");
	}

	@Test
	void readWithoutDataReturnsViewWithNullData() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNull();
	}

	@Test
	void readPropagatesNotFound() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "x"));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void updateDataSavesWhenErrandExists() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));

		service.updateData(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			FinancialAssistanceData.create().withApplicationType("RENEWAL"));

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");
	}

	@Test
	void createNormberakningResolvesPartyIdsDelegatesAndMaps() {
		final var month = YearMonth.of(2026, 6);
		final var result = new NormberakningResult(4711, List.of(), List.of());
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CO_APPLICANT_PARTY_ID)).thenReturn(Optional.of("199202022345"));
		when(normberakningServiceMock.buildAndPost(MUNICIPALITY_ID, "199001011234", "199202022345", month)).thenReturn(result);

		final var request = NormberakningRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withCoApplicant(CO_APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06");

		final var response = service.createNormberakning(MUNICIPALITY_ID, request);

		assertThat(response.getCalculationId()).isEqualTo(4711);
		verify(normberakningServiceMock).buildAndPost(MUNICIPALITY_ID, "199001011234", "199202022345", month);
	}

	@Test
	void createNormberakningUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		final var request = NormberakningRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.createNormberakning(MUNICIPALITY_ID, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(normberakningServiceMock, never()).buildAndPost(any(), any(), any(), any());
	}
}
