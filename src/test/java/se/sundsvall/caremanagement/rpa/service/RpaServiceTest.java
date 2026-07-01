package se.sundsvall.caremanagement.rpa.service;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.rpa.integration.RpaClient;
import se.sundsvall.caremanagement.rpa.integration.configuration.RpaProperties;
import se.sundsvall.caremanagement.rpa.integration.model.AddQueueItemParameters;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class RpaServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String FOLDER_ID = "7";

	private static RpaProperties properties(final boolean enabled) {
		return new RpaProperties(enabled, "RakelEkonomisktBistand", Map.of(MUNICIPALITY_ID, FOLDER_ID), 5, 30);
	}

	@Test
	void enqueueAddsQueueItem() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, properties(true));

		service.enqueue(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, RpaAction.WRITE_NORMBERAKNING, Map.of("hint", "x"));

		final var captor = ArgumentCaptor.forClass(AddQueueItemParameters.class);
		verify(client).addQueueItem(eq(FOLDER_ID), captor.capture());

		final var data = captor.getValue().itemData();
		assertThat(data.name()).isEqualTo("RakelEkonomisktBistand");
		assertThat(data.reference()).isEqualTo(NAMESPACE + ":" + ERRAND_ID + ":" + RpaAction.WRITE_NORMBERAKNING);
		assertThat(data.priority()).isEqualTo("Normal");
		assertThat(data.specificContent())
			.containsEntry("action", RpaAction.WRITE_NORMBERAKNING)
			.containsEntry("errandId", ERRAND_ID)
			.containsEntry("municipalityId", MUNICIPALITY_ID)
			.containsEntry("namespace", NAMESPACE)
			.containsEntry("hint", "x");
	}

	@Test
	void enqueueWithoutNamespaceOmitsNamespaceAndKeepsErrandActionReference() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, properties(true));

		service.enqueue(MUNICIPALITY_ID, null, ERRAND_ID, RpaAction.WRITE_DECISION, Map.of());

		final var captor = ArgumentCaptor.forClass(AddQueueItemParameters.class);
		verify(client).addQueueItem(eq(FOLDER_ID), captor.capture());

		final var data = captor.getValue().itemData();
		assertThat(data.reference()).isEqualTo(ERRAND_ID + ":" + RpaAction.WRITE_DECISION);
		assertThat(data.specificContent()).doesNotContainKey("namespace");
	}

	@Test
	void differentActionsOnSameErrandGetDistinctReferences() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, properties(true));

		service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.FETCH_SUPPLEMENTS);
		service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.WRITE_DECISION);

		final var captor = ArgumentCaptor.forClass(AddQueueItemParameters.class);
		verify(client, times(2)).addQueueItem(eq(FOLDER_ID), captor.capture());

		final var references = captor.getAllValues().stream().map(p -> p.itemData().reference()).toList();
		assertThat(references).containsExactly(
			ERRAND_ID + ":" + RpaAction.FETCH_SUPPLEMENTS,
			ERRAND_ID + ":" + RpaAction.WRITE_DECISION);
	}

	@Test
	void enqueueWithoutSpecificContentUsesDefaults() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, properties(true));

		service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.FETCH_SUPPLEMENTS);

		verify(client).addQueueItem(eq(FOLDER_ID), any());
	}

	@Test
	void disabledIsNoOp() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, properties(false));

		service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.WRITE_DECISION);

		verifyNoInteractions(client);
	}

	@Test
	void duplicateConflictIsSwallowed() {
		final var client = mock(RpaClient.class);
		doThrow(Problem.valueOf(CONFLICT, "Queue item already exists, error code 1016"))
			.when(client).addQueueItem(any(), any());
		final var service = new RpaService(client, properties(true));

		// no exception
		service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.WRITE_DECISION);
	}

	@Test
	void otherProblemIsRethrown() {
		final var client = mock(RpaClient.class);
		doThrow(Problem.valueOf(INTERNAL_SERVER_ERROR, "boom"))
			.when(client).addQueueItem(any(), any());
		final var service = new RpaService(client, properties(true));

		assertThatThrownBy(() -> service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.WRITE_DECISION))
			.hasMessageContaining("boom");
	}

	@Test
	void missingFolderIdFails() {
		final var client = mock(RpaClient.class);
		final var service = new RpaService(client, new RpaProperties(true, "q", Map.of(), 5, 30));

		assertThatThrownBy(() -> service.enqueue(MUNICIPALITY_ID, ERRAND_ID, RpaAction.WRITE_DECISION))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No RPA folder id");
		verifyNoInteractions(client);
	}
}
