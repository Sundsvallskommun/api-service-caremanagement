package se.sundsvall.caremanagement.eventlog.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.api.model.LifecareAccess;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LifecareAccessLogServiceTest {

	@Mock
	private ErrandEventService errandEventService;

	@InjectMocks
	private LifecareAccessLogService service;

	@Captor
	private ArgumentCaptor<List<LifecareAccess>> accessesCaptor;

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void recordsWithTheCaller() {
		final var identifier = Identifier.parse("joe01doe; type=adAccount");
		Identifier.set(identifier);

		service.record("2281", "NS", "errand", List.of(new LifecareAccessEntry("READ", "reminders", "Läste bevakningar", "7")));

		verify(errandEventService).recordLifecareAccesses(eq("2281"), eq("NS"), eq("errand"), eq(identifier), accessesCaptor.capture());
		assertThat(accessesCaptor.getValue()).singleElement().satisfies(access -> {
			assertThat(access.getAction()).isEqualTo("READ");
			assertThat(access.getTarget()).isEqualTo("reminders");
			assertThat(access.getDescription()).isEqualTo("Läste bevakningar");
			assertThat(access.getLifecareId()).isEqualTo("7");
		});
	}

	@Test
	void nothingToRecord() {
		service.record("2281", "NS", "errand", List.of());

		verifyNoInteractions(errandEventService);
	}

	@Test
	void missingCaller() {
		assertThatThrownBy(() -> service.record("2281", "NS", "errand", List.of(new LifecareAccessEntry("READ", "x", null, null))))
			.hasMessageContaining("X-Sent-By");
		verify(errandEventService, org.mockito.Mockito.never()).recordLifecareAccesses(any(), any(), any(), any(), any());
	}
}
