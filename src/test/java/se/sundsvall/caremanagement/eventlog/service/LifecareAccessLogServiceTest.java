package se.sundsvall.caremanagement.eventlog.service;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.dept44.support.Identifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LifecareAccessLogServiceTest {

	@Mock
	private ErrandEventService errandEventService;

	@InjectMocks
	private LifecareAccessLogService service;

	@AfterEach
	void tearDown() {
		Identifier.remove();
	}

	@Test
	void recordsWithTheCaller() {
		final var identifier = Identifier.parse("joe01doe; type=adAccount");
		Identifier.set(identifier);

		final var entries = List.of(new LifecareAccessEntry("READ", "reminders", "Läste bevakningar", "7"));

		service.append("2281", "NS", "errand", entries);

		verify(errandEventService).recordLifecareAccesses("2281", "NS", "errand", identifier, entries);
	}

	@Test
	void nothingToRecord() {
		service.append("2281", "NS", "errand", List.of());

		verifyNoInteractions(errandEventService);
	}

	@Test
	void missingCaller() {
		assertThatThrownBy(() -> service.append("2281", "NS", "errand", List.of(new LifecareAccessEntry("READ", "x", null, null))))
			.hasMessageContaining("X-Sent-By");
		verify(errandEventService, never()).recordLifecareAccesses(any(), any(), any(), any(), any());
	}
}
