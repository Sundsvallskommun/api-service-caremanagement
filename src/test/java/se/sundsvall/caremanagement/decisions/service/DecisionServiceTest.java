package se.sundsvall.caremanagement.decisions.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String DECISION_ID = "22222222-2222-2222-2222-222222222222";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private DecisionRepository decisionRepositoryMock;

	@Mock
	private ApplicationEventPublisher eventPublisherMock;

	@InjectMocks
	private DecisionService service;

	@Test
	void createPublishesNotificationsAndReturnsId() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withReporterUserId("reporter").withAssignedUserId("assignee");
		final var saved = DecisionEntity.create().withId(DECISION_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(decisionRepositoryMock.save(any(DecisionEntity.class))).thenReturn(saved);

		final var decision = Decision.create().withDecisionType("PAYMENT").withValue("APPROVED").withCreatedBy("operaton");
		final var id = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, decision);

		assertThat(id).isEqualTo(DECISION_ID);

		final ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
		verify(eventPublisherMock, times(2)).publishEvent(captor.capture());
		assertThat(captor.getAllValues()).extracting("ownerId").containsExactlyInAnyOrder("reporter", "assignee");
		assertThat(captor.getAllValues()).allSatisfy(req -> {
			assertThat(req.type()).isEqualTo("CREATE");
			assertThat(req.subType()).isEqualTo("DECISION");
			assertThat(req.description()).contains("PAYMENT").contains("APPROVED");
		});
	}

	@Test
	void createWithoutAnyRecipientsSkipsNotifications() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(decisionRepositoryMock.save(any(DecisionEntity.class)))
			.thenReturn(DecisionEntity.create().withId(DECISION_ID));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Decision.create().withDecisionType("X").withValue("Y"));

		verify(eventPublisherMock, never()).publishEvent(any());
	}

	@Test
	void createWhenReporterEqualsAssigneeDedupesRecipients() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withReporterUserId("u").withAssignedUserId("u");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(decisionRepositoryMock.save(any(DecisionEntity.class)))
			.thenReturn(DecisionEntity.create().withId(DECISION_ID));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Decision.create().withDecisionType("X").withValue("Y"));

		verify(eventPublisherMock, times(1)).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createWhenErrandMissingThrowsNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Decision.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(decisionRepositoryMock, eventPublisherMock);
	}

	@Test
	void readReturnsMappedDecision() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(decisionRepositoryMock.findByErrandIdAndId(ERRAND_ID, DECISION_ID))
			.thenReturn(Optional.of(DecisionEntity.create().withId(DECISION_ID).withDecisionType("PAYMENT").withValue("APPROVED")));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);

		assertThat(result.getId()).isEqualTo(DECISION_ID);
		assertThat(result.getDecisionType()).isEqualTo("PAYMENT");
		assertThat(result.getValue()).isEqualTo("APPROVED");
	}

	@Test
	void readWhenDecisionMissingThrowsNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(decisionRepositoryMock.findByErrandIdAndId(ERRAND_ID, DECISION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAllReturnsList() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(decisionRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID))
			.thenReturn(List.of(DecisionEntity.create().withId(DECISION_ID)));

		assertThat(service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.hasSize(1)
			.first()
			.hasFieldOrPropertyWithValue("id", DECISION_ID);
	}

	@Test
	void deleteRemovesEntity() {
		final var entity = DecisionEntity.create().withId(DECISION_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(decisionRepositoryMock.findByErrandIdAndId(ERRAND_ID, DECISION_ID)).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);

		verify(decisionRepositoryMock).delete(entity);
	}
}
