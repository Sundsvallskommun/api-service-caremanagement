package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessLog;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LifecareAccessRecorderTest {

	private static final LifecareErrand ERRAND = new LifecareErrand("2281", "NS", "errand", 24, null, null, null, 2026, 9);

	@Mock
	private LifecareAccessLog accessLog;

	@InjectMocks
	private LifecareAccessRecorder recorder;

	@Test
	void read() {
		recorder.read(ERRAND, "reminders", "Läste bevakningar");

		verify(accessLog).record("2281", "NS", "errand", List.of(new LifecareAccessEntry("READ", "reminders", "Läste bevakningar", null)));
	}

	@Test
	void readThatCannotBeLoggedFails() {
		doThrow(new IllegalStateException()).when(accessLog).record(any(), any(), any(), any());

		assertThatThrownBy(() -> recorder.read(ERRAND, "reminders", "x", "1")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void writeThatCannotBeLoggedStands() {
		doThrow(new IllegalStateException()).when(accessLog).record(any(), any(), any(), any());

		assertThatNoException().isThrownBy(() -> recorder.written(ERRAND, "CREATE", "reminder", "Skapade bevakning", "9"));
	}
}
