package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
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

	@InjectMocks
	private FinancialAssistanceService service;

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
}
