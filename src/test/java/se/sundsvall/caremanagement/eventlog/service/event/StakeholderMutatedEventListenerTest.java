package se.sundsvall.caremanagement.eventlog.service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StakeholderMutatedEventListenerTest {

	@Mock
	private ErrandEventService serviceMock;

	@InjectMocks
	private StakeholderMutatedEventListener listener;

	@Test
	void recordsStakeholderMutatedAsCoarseChangeRow() {
		listener.recordStakeholderChange(new StakeholderMutated("2281", "EB", "errand-1"));

		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).recordDomainEvent(captor.capture());
		final var entity = captor.getValue();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getSource()).isEqualTo("EVENT");
		assertThat(entity.getAction()).isEqualTo("UPDATE");
		assertThat(entity.getTarget()).isEqualTo("stakeholder");
		assertThat(entity.getDescription()).isEqualTo("Intressent ändrad");
		assertThat(entity.getActor()).isEqualTo("system");
		assertThat(entity.getCreated()).isNotNull();
	}
}
