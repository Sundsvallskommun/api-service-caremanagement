package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
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
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.CalculationDraftMapper;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * With {@code spring.jpa.open-in-view} off, nothing keeps a session open once the transaction that loaded an entity has
 * ended, and a lazy collection that is still unread then cannot be read at all. The errand's application data and the
 * calculation draft keep their repeating groups in such collections, so what a service hands out must either have read
 * them already or be a copy. Needs a real database and no transaction of the test's own, which a mocked repository or a
 * test method rolled back in a transaction would hide.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Transactional(propagation = NOT_SUPPORTED)
class LazyCollectionsOutsideTransactionTest {

	private static final String ERRAND_ID = "6b1d2e4f-2222-4d3c-8e6f-1b2c3d4e5f60";

	@Autowired
	private FinancialAssistanceRepository repository;

	@Autowired
	private FaCalculationDraftRepository draftRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		repository.saveAndFlush(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("RENEWAL")
			.withNormType(List.of("NATIONAL_NORM")).withLifecarePaymentIds(List.of("p1", "p2")));
		draftRepository.saveAndFlush(FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM")));
	}

	@AfterEach
	void tearDown() {
		repository.deleteById(ERRAND_ID);
		draftRepository.deleteById(ERRAND_ID);
	}

	@Test
	void aPlainLookupLeavesTheCollectionsUnreadableOnceItsOwnTransactionHasEnded() {
		final var paymentIds = repository.findByErrandId(ERRAND_ID).orElseThrow().getLifecarePaymentIds();

		assertThat(Hibernate.isInitialized(paymentIds)).isFalse();
		assertThatThrownBy(paymentIds::size).isInstanceOf(LazyInitializationException.class);
	}

	@Test
	void theLookupWithTheGraphFetchesTheLinkedPaymentIdsWithTheRow() {
		final var entity = repository.findWithLifecarePaymentIdsByErrandId(ERRAND_ID).orElseThrow();

		assertThat(entity.getLifecarePaymentIds()).containsExactlyInAnyOrder("p1", "p2");
	}

	@Test
	void theApplicationDataOutlivesTheTransactionThatReadIt() {
		final var data = new TransactionTemplate(transactionManager)
			.execute(status -> FinancialAssistanceMapper.toData(repository.findByErrandId(ERRAND_ID).orElseThrow()));

		assertThat(requireNonNull(data).getNormType()).containsExactly("NATIONAL_NORM");
		assertThat(data.getLifecarePaymentIds()).containsExactlyInAnyOrder("p1", "p2");
	}

	@Test
	void theCalculationDraftOutlivesTheTransactionThatReadIt() {
		final var draft = new TransactionTemplate(transactionManager)
			.execute(status -> CalculationDraftMapper.toCalculationDraft(draftRepository.findById(ERRAND_ID).orElseThrow(), List.of(), List.of(), List.of()));

		assertThat(requireNonNull(draft).getNormType()).containsExactly("NATIONAL_NORM");
	}
}
