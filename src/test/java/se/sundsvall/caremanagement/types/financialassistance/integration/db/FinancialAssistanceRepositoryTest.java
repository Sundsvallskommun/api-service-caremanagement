package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * The actualisation marker has to be committed on its own: the step writes it before it calls Lifecare, and it has to
 * survive the rollback of the transaction the step runs in. That is a property of the repository method's propagation,
 * which a mocked repository cannot show — so the test runs without a transaction of its own, commits its setup, and
 * builds the surrounding transaction itself.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Transactional(propagation = NOT_SUPPORTED)
class FinancialAssistanceRepositoryTest {

	private static final String ERRAND_ID = "5a9e1f3c-1111-4c2b-9d5e-0a1b2c3d4e5f";
	private static final OffsetDateTime FIRST_ATTEMPT = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime RETRY = FIRST_ATTEMPT.plusMinutes(5);

	@Autowired
	private FinancialAssistanceRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		repository.saveAndFlush(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("RENEWAL"));
	}

	@AfterEach
	void tearDown() {
		repository.deleteById(ERRAND_ID);
	}

	@Test
	void theFirstAttemptSetsTheMarkerAndTheRetryFindsItAlreadySetAndLeavesItAlone() {
		assertThat(repository.markActualisationRequestedIfAbsent(ERRAND_ID, FIRST_ATTEMPT)).isOne();
		assertThat(repository.markActualisationRequestedIfAbsent(ERRAND_ID, RETRY)).isZero();

		assertMarkedAt(FIRST_ATTEMPT);
	}

	@Test
	void anErrandWithNoApplicationRowHasNothingToMark() {
		assertThat(repository.markActualisationRequestedIfAbsent("no-such-errand", FIRST_ATTEMPT)).isZero();
	}

	@Test
	void theMarkerSurvivesTheRollbackOfTheTransactionItWasWrittenFrom() {
		final var intake = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> intake.executeWithoutResult(status -> {
			// The intake has read the errand's application, as the step does, before it marks and calls Lifecare.
			assertThat(repository.findByErrandId(ERRAND_ID)).isPresent();
			assertThat(repository.markActualisationRequestedIfAbsent(ERRAND_ID, FIRST_ATTEMPT)).isOne();
			throw new IllegalStateException("the intake fails after Lifecare has created the actualisation");
		})).isInstanceOf(IllegalStateException.class);

		assertMarkedAt(FIRST_ATTEMPT);
	}

	@Test
	void theMarkerIsNotOverwrittenByTheIntakeThatLoadedTheRowBeforeIt() {
		final var intake = new TransactionTemplate(transactionManager);

		intake.executeWithoutResult(status -> {
			final var loadedBeforeTheMarker = repository.findByErrandId(ERRAND_ID).orElseThrow();
			assertThat(repository.markActualisationRequestedIfAbsent(ERRAND_ID, FIRST_ATTEMPT)).isOne();
			// Something later in the intake changes the loaded row: only the changed column is written back.
			loadedBeforeTheMarker.setPeriodMonth(9);
		});

		assertMarkedAt(FIRST_ATTEMPT);
		assertThat(repository.findByErrandId(ERRAND_ID)).get().extracting(FinancialAssistanceEntity::getPeriodMonth).isEqualTo(9);
	}

	/** The stored marker, compared as the instant it is: the database hands it back in its own offset. */
	private void assertMarkedAt(final OffsetDateTime expected) {
		assertThat(repository.findByErrandId(ERRAND_ID)).get()
			.extracting(FinancialAssistanceEntity::getActualisationRequestedAt)
			.isNotNull()
			.satisfies(marker -> assertThat(marker.toInstant()).isEqualTo(expected.toInstant()));
	}
}
