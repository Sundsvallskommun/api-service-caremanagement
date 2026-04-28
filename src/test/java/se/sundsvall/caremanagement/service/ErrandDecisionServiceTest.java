package se.sundsvall.caremanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.Decision;
import se.sundsvall.caremanagement.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandDecisionServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String DECISION_ID = "did";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Captor
	private ArgumentCaptor<DecisionEntity> decisionCaptor;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	@InjectMocks
	private ErrandDecisionService service;

	@Test
	void create() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(decisionRepositoryMock.save(any(DecisionEntity.class)))
			.thenAnswer(inv -> ((DecisionEntity) inv.getArgument(0)).withId(DECISION_ID));

		final var decision = Decision.create().withDecisionType("PAYMENT").withValue("APPROVED").withCreatedBy("jane01doe");
		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, decision);

		assertThat(result).isEqualTo(DECISION_ID);
		verify(decisionRepositoryMock).save(decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getErrandEntity()).isSameAs(errand);
		assertThat(decisionCaptor.getValue().getDecisionType()).isEqualTo("PAYMENT");
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("APPROVED");
		assertThat(decisionCaptor.getValue().getCreatedBy()).isEqualTo("jane01doe");
	}

	@Test
	void create_errandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Decision.create().withDecisionType("X").withValue("Y")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(decisionRepositoryMock);
	}

	@Test
	void readAll() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withDecisions(new ArrayList<>(List.of(
			DecisionEntity.create().withId("d1").withDecisionType("RECOMMENDATION").withValue("Beslutsförslag"),
			DecisionEntity.create().withId("d2").withDecisionType("PAYMENT").withValue("APPROVED"))));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo("d1");
		assertThat(result.get(1).getId()).isEqualTo("d2");
	}

	@Test
	void readAll_errandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void read() {
		final var entity = DecisionEntity.create().withId(DECISION_ID).withDecisionType("PAYMENT").withValue("APPROVED");
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withDecisions(new ArrayList<>(List.of(entity)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);

		assertThat(result.getId()).isEqualTo(DECISION_ID);
		assertThat(result.getValue()).isEqualTo("APPROVED");
	}

	@Test
	void read_decisionNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withDecisions(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void delete() {
		final var entity = DecisionEntity.create().withId(DECISION_ID);
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withDecisions(new ArrayList<>(List.of(entity)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);

		verify(errandRepositoryMock).save(errandCaptor.capture());
		assertThat(errandCaptor.getValue().getDecisions()).isEmpty();
	}

	@Test
	void delete_decisionNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withDecisions(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
