package se.sundsvall.caremanagement.types.financialassistance.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class ProtectedIdentityGateTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String CHILD = "0b9d2f6e-3c1a-4e57-8d20-6a4f1c9e7b31";

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	private ProtectedIdentityGate gate() {
		return new ProtectedIdentityGate(citizenServiceMock, lifecareCaseServiceMock);
	}

	private boolean check(final String... partyIds) {
		return gate().protectedOrUnknown(MUNICIPALITY_ID, ERRAND_ID, Arrays.asList(partyIds));
	}

	@Test
	void notProtectedInEitherSourceIsNotProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);

		assertThat(check(APPLICANT)).isFalse();

		verify(citizenServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
		verify(lifecareCaseServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
	}

	@Test
	void personLifecareHoldsNoRecordOfIsNotProtectedWhenCitizenIsClear() {
		// A first-time applicant: Lifecare has no record of them (LifecareCaseService answers false for that), so the
		// population register alone decides — and it is clear. Must not freeze the errand.
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);

		final var events = capturingLogs(() -> check(APPLICANT));

		assertThat(events).isEmpty();
		verify(lifecareCaseServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
	}

	@Test
	void personLifecareHoldsNoRecordOfIsStillProtectedWhenCitizenSaysSo() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		assertThat(check(APPLICANT)).isTrue();
	}

	@Test
	void protectedInCitizenIsProtectedWithoutAskingLifecare() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		assertThat(check(APPLICANT)).isTrue();

		verifyNoInteractions(lifecareCaseServiceMock);
	}

	@Test
	void protectedInLifecareIsProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		assertThat(check(APPLICANT)).isTrue();
	}

	@Test
	void citizenFailureIsTreatedAsProtectedWithoutAskingLifecare() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenThrow(Problem.valueOf(BAD_GATEWAY, "citizen down"));

		assertThat(check(APPLICANT)).isTrue();

		verifyNoInteractions(lifecareCaseServiceMock);
	}

	@Test
	void citizenFailureThatIsNotAProblemIsTreatedAsProtected() {
		// A transport failure or an open circuit breaker is not a Problem — the gate must fail closed on those too.
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenThrow(new IllegalStateException("circuit open"));

		assertThat(check(APPLICANT)).isTrue();
	}

	@Test
	void lifecareFailureIsTreatedAsProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenThrow(Problem.valueOf(BAD_GATEWAY, "FamilyCare down"));

		assertThat(check(APPLICANT)).isTrue();
	}

	@Test
	void lifecareFailureThatIsNotAProblemIsTreatedAsProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenThrow(new IllegalStateException("circuit open"));

		assertThat(check(APPLICANT)).isTrue();
	}

	@Test
	void noPartiesIsNotProtectedAndReadsNothing() {
		assertThat(gate().protectedOrUnknown(MUNICIPALITY_ID, ERRAND_ID, List.of())).isFalse();

		verifyNoInteractions(citizenServiceMock, lifecareCaseServiceMock);
	}

	@Test
	void blankAndMissingPartyIdsAreIgnored() {
		assertThat(check(null, "", "   ")).isFalse();

		verifyNoInteractions(citizenServiceMock, lifecareCaseServiceMock);
	}

	@Test
	void blankPartyIdsAreSkippedButTheRealOneIsStillChecked() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		assertThat(check("", APPLICANT, null)).isTrue();

		verify(citizenServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
	}

	@Test
	void oneProtectedPartyAmongSeveralIsProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(false);
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CHILD)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CHILD)).thenReturn(true);

		assertThat(check(APPLICANT, CO_APPLICANT, CHILD)).isTrue();
	}

	@Test
	void everyPartyOfSeveralIsReadWhenNoneIsProtected() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(false);

		assertThat(check(APPLICANT, CO_APPLICANT)).isFalse();

		verify(citizenServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT);
		verify(lifecareCaseServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT);
	}

	@Test
	void stopsAtTheFirstProtectedParty() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		assertThat(check(APPLICANT, CO_APPLICANT)).isTrue();

		// The outcome is settled by the applicant; nobody else's identity is read for it.
		final InOrder order = inOrder(citizenServiceMock, lifecareCaseServiceMock);
		order.verify(citizenServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
		order.verify(lifecareCaseServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
		verify(citizenServiceMock, never()).hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT);
		verify(lifecareCaseServiceMock, never()).hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT);
	}

	@Test
	void aRepeatedPartyIsReadOnce() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);

		assertThat(check(APPLICANT, APPLICANT)).isFalse();

		verify(citizenServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
		verify(lifecareCaseServiceMock).hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT);
	}

	@Test
	void aFailedLookupIsLoggedOnceWithTheErrandOnly() {
		// The exception message carries the partyId, as the real integrations' problems do — it must never reach the log.
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT))
			.thenThrow(new IllegalStateException("No citizen found for partyId " + APPLICANT));

		final var events = capturingLogs(() -> check(APPLICANT));

		assertThat(events).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage())
				.contains(ERRAND_ID)
				.contains("citizen")
				.contains("IllegalStateException")
				.doesNotContain(APPLICANT);
			assertThat(event.getThrowableProxy()).isNull();
			assertThat(event.getArgumentArray()).doesNotContain(APPLICANT);
		});
	}

	@Test
	void aFailedLifecareLookupIsLoggedOnceAndNamesTheSource() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching person in Lifecare FamilyCare: " + APPLICANT));

		final var events = capturingLogs(() -> check(APPLICANT));

		assertThat(events).singleElement().satisfies(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage()).contains(ERRAND_ID).contains("Lifecare").doesNotContain(APPLICANT);
			assertThat(event.getThrowableProxy()).isNull();
		});
	}

	@Test
	void aProtectedPartyIsNotLoggedAtAll() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		final var events = capturingLogs(() -> check(APPLICANT));

		assertThat(events).isEmpty();
	}

	@Test
	void aClearedPartyIsNotLoggedAtAll() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(lifecareCaseServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);

		final var events = capturingLogs(() -> check(APPLICANT));

		assertThat(events).isEmpty();
	}

	/** Runs the action with an appender on the gate's logger (level pinned to DEBUG) and returns everything it logged. */
	private static List<ILoggingEvent> capturingLogs(final Supplier<Boolean> action) {
		final var logger = (Logger) LoggerFactory.getLogger(ProtectedIdentityGate.class);
		final var originalLevel = logger.getLevel();
		final var appender = new ListAppender<ILoggingEvent>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);

		try {
			action.get();
			return List.copyOf(appender.list);
		} finally {
			logger.setLevel(originalLevel);
			logger.detachAppender(appender);
		}
	}
}
