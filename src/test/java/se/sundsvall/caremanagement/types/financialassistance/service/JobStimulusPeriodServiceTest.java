package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaJobStimulusPeriodRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaJobStimulusPeriodEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStimulusPeriodServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FaJobStimulusPeriodRepository repositoryMock;

	@InjectMocks
	private JobStimulusPeriodService service;

	@Test
	void listMapsEntitiesInDateOrder() {
		when(repositoryMock.findByErrandIdOrderByFromDateAsc(ERRAND_ID)).thenReturn(List.of(
			FaJobStimulusPeriodEntity.create().withErrandId(ERRAND_ID).withRole("APPLICANT")
				.withFromDate(LocalDate.parse("2021-01-01")).withToDate(LocalDate.parse("2021-12-31")),
			FaJobStimulusPeriodEntity.create().withErrandId(ERRAND_ID).withRole("CO_APPLICANT")
				.withFromDate(LocalDate.parse("2022-01-01"))));

		final var periods = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(periods).containsExactly(
			new JobStimulusPeriod("APPLICANT", LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")),
			new JobStimulusPeriod("CO_APPLICANT", LocalDate.parse("2022-01-01"), null));
	}

	@Test
	void replaceAllDeletesThenStoresAndReturnsCount() {
		when(repositoryMock.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final var stored = service.replaceAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, List.of(
			new JobStimulusPeriod("APPLICANT", LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")),
			new JobStimulusPeriod("CO_APPLICANT", LocalDate.parse("2022-01-01"), null)));

		assertThat(stored).isEqualTo(2);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).deleteByErrandId(ERRAND_ID);

		final ArgumentCaptor<List<FaJobStimulusPeriodEntity>> captor = ArgumentCaptor.captor();
		verify(repositoryMock).saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(2);
		assertThat(captor.getValue().getFirst().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().getFirst().getRole()).isEqualTo("APPLICANT");
		assertThat(captor.getValue().getFirst().getFromDate()).isEqualTo(LocalDate.parse("2021-01-01"));
		assertThat(captor.getValue().getLast().getToDate()).isNull();
	}

	@Test
	void replaceAllWithEmptyListEmptiesTheSet() {
		when(repositoryMock.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final var stored = service.replaceAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, List.of());

		assertThat(stored).isZero();
		verify(repositoryMock).deleteByErrandId(ERRAND_ID);
	}
}
