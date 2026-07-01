package se.sundsvall.caremanagement.eventlog.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.decisions.service.event.DecisionRecorded;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DecisionRecordedEventListenerTest {
	private static final OffsetDateTime TS = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private ErrandEventService serviceMock;

	@InjectMocks
	private DecisionRecordedEventListener listener;

	private ErrandEventEntity capture() {
		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).recordDomainEvent(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsDecisionWithTypeAndOutcome() {
		listener.on(new DecisionRecorded("d-1", "errand-1", "2281", "EB", "PAYMENT", "APPROVED", "carola01winberg", TS));

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getSource()).isEqualTo("EVENT");
		assertThat(entity.getAction()).isEqualTo("CREATE");
		assertThat(entity.getTarget()).isEqualTo("decision");
		assertThat(entity.getDescription()).isEqualTo("Beslut registrerat: PAYMENT = APPROVED");
		assertThat(entity.getActor()).isEqualTo("carola01winberg");
		assertThat(entity.getCreated()).isEqualTo(TS);
	}

	@Test
	void omitsOutcomeWhenBlankAndDefaultsActorToSystem() {
		listener.on(new DecisionRecorded("d-1", "errand-1", "2281", "EB", "RECOMMENDATION", " ", null, TS));

		final var entity = capture();
		assertThat(entity.getDescription()).isEqualTo("Beslut registrerat: RECOMMENDATION");
		assertThat(entity.getActor()).isEqualTo("system");
	}
}
