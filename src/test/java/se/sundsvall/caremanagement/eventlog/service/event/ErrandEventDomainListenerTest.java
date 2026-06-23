package se.sundsvall.caremanagement.eventlog.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErrandEventDomainListenerTest {
	private static final OffsetDateTime TS = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandEventService serviceMock;

	@InjectMocks
	private ErrandEventDomainListener listener;

	private ErrandEventEntity capture() {
		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).recordDomainEvent(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsErrandCreatedWithReporterAsActor() {
		listener.on(new ErrandCreated(ERRAND_ID, "FINANCIAL_ASSISTANCE", "2281", "EB", "joe001doe", "edwmol", TS));

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getSource()).isEqualTo("EVENT");
		assertThat(entity.getAction()).isEqualTo("CREATE");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getActor()).isEqualTo("joe001doe");
		assertThat(entity.getCreated()).isEqualTo(TS);
	}

	@Test
	void recordsStatusChange() {
		listener.on(new ErrandStatusChanged(ERRAND_ID, "FINANCIAL_ASSISTANCE", "2281", "EB", "OPEN", "DECIDED", "edwmol", TS));

		final var entity = capture();
		assertThat(entity.getAction()).isEqualTo("UPDATE");
		assertThat(entity.getTarget()).isEqualTo("status");
		assertThat(entity.getDescription()).isEqualTo("Status OPEN -> DECIDED");
		assertThat(entity.getActor()).isEqualTo("edwmol");
	}

	@Test
	void recordsAssignment() {
		listener.on(new ErrandAssigned(ERRAND_ID, "FINANCIAL_ASSISTANCE", "2281", "EB", "joe001doe", "edwmol", "boss001", TS));

		final var entity = capture();
		assertThat(entity.getAction()).isEqualTo("UPDATE");
		assertThat(entity.getTarget()).isEqualTo("assignment");
		assertThat(entity.getDescription()).isEqualTo("Assigned joe001doe -> edwmol");
		assertThat(entity.getActor()).isEqualTo("boss001");
	}

	@Test
	void recordsDeletion() {
		listener.on(new ErrandDeleted(ERRAND_ID, "FINANCIAL_ASSISTANCE", "2281", "EB", "edwmol", TS));

		final var entity = capture();
		assertThat(entity.getAction()).isEqualTo("DELETE");
		assertThat(entity.getTarget()).isEqualTo("errand");
		assertThat(entity.getActor()).isEqualTo("edwmol");
	}

	@Test
	void defaultsActorToSystemWhenEventCarriesNone() {
		listener.on(new ErrandStatusChanged(ERRAND_ID, "FINANCIAL_ASSISTANCE", "2281", "EB", "OPEN", "DECIDED", null, TS));

		assertThat(capture().getActor()).isEqualTo("system");
	}
}
