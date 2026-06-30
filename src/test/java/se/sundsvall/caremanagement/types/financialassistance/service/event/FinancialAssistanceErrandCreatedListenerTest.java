package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.jpa.JpaSystemException;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedProcessor.Outcome;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandCreatedListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final int ER_CHECKREAD = 1020;
	private static final int MAX_ATTEMPTS = 4;

	@Mock
	private FinancialAssistanceErrandCreatedProcessor processorMock;

	@InjectMocks
	private FinancialAssistanceErrandCreatedListener listener;

	private static ErrandCreated event() {
		return new ErrandCreated(ERRAND_ID, SLUG_RENEWAL, MUNICIPALITY_ID, NAMESPACE, "reporter", null,
			OffsetDateTime.parse("2026-06-05T12:00:00Z"));
	}

	/** A Spring DataAccessException carrying the MariaDB snapshot-isolation conflict (errorCode 1020) as a nested cause. */
	private static JpaSystemException snapshotConflict() {
		return new JpaSystemException(new RuntimeException(new SQLException("Record has changed since last read", "HY000", ER_CHECKREAD)));
	}

	@Test
	void startsProcessWhenClassificationProceeds() {
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.PROCEED);

		listener.on(event());

		verify(processorMock).assignAndClassify(any());
		verify(processorMock).startProcess(any());
	}

	@Test
	void doesNotStartProcessWhenFrozen() {
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.FROZEN);

		listener.on(event());

		verify(processorMock, never()).startProcess(any());
	}

	@Test
	void doesNotStartProcessForNonEbErrand() {
		when(processorMock.assignAndClassify(any())).thenReturn(Outcome.NOT_EB);

		listener.on(event());

		verify(processorMock, never()).startProcess(any());
	}

	@Test
	void retriesClassificationOnSnapshotConflictThenStartsProcess() {
		when(processorMock.assignAndClassify(any()))
			.thenThrow(snapshotConflict())
			.thenThrow(snapshotConflict())
			.thenReturn(Outcome.PROCEED);

		listener.on(event());

		verify(processorMock, times(3)).assignAndClassify(any());
		verify(processorMock).startProcess(any());
	}

	@Test
	void givesUpAfterMaxAttemptsAndRethrows() {
		when(processorMock.assignAndClassify(any())).thenThrow(snapshotConflict());

		assertThatExceptionOfType(JpaSystemException.class).isThrownBy(() -> listener.on(event()));

		verify(processorMock, times(MAX_ATTEMPTS)).assignAndClassify(any());
		verify(processorMock, never()).startProcess(any());
	}

	@Test
	void doesNotRetryNonSnapshotDataAccessException() {
		final var unrelated = new JpaSystemException(new RuntimeException(new SQLException("deadlock", "40001", 1213)));
		when(processorMock.assignAndClassify(any())).thenThrow(unrelated);

		assertThatExceptionOfType(JpaSystemException.class).isThrownBy(() -> listener.on(event()));

		verify(processorMock, times(1)).assignAndClassify(any());
		verify(processorMock, never()).startProcess(any());
	}
}
