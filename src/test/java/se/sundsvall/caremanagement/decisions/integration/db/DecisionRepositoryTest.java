package se.sundsvall.caremanagement.decisions.integration.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class DecisionRepositoryTest {

	private static final String THIS_ERRAND = "11111111-1111-1111-1111-111111111111";
	private static final String OTHER_ERRAND = "22222222-2222-2222-2222-222222222222";

	@Autowired
	private DecisionRepository repository;

	@BeforeEach
	void setUp() {
		repository.saveAndFlush(decision(THIS_ERRAND, "ACTUALISATION", "5010"));
		repository.saveAndFlush(decision(OTHER_ERRAND, "ACTUALISATION", "5011"));
		repository.saveAndFlush(decision(OTHER_ERRAND, "RECOMMENDATION", "5012"));
	}

	@Test
	void findsAValueRecordedOnAnotherErrand() {
		assertThat(repository.existsByDecisionTypeAndValueAndErrandIdNot("ACTUALISATION", "5011", THIS_ERRAND)).isTrue();
	}

	@Test
	void doesNotCountTheErrandsOwnDecision() {
		assertThat(repository.existsByDecisionTypeAndValueAndErrandIdNot("ACTUALISATION", "5010", THIS_ERRAND)).isFalse();
	}

	@Test
	void doesNotCountAnotherTypeOrAnotherValue() {
		assertThat(repository.existsByDecisionTypeAndValueAndErrandIdNot("ACTUALISATION", "5012", THIS_ERRAND)).isFalse();
		assertThat(repository.existsByDecisionTypeAndValueAndErrandIdNot("ACTUALISATION", "5099", THIS_ERRAND)).isFalse();
	}

	private static DecisionEntity decision(final String errandId, final String decisionType, final String value) {
		return DecisionEntity.create().withErrandId(errandId).withDecisionType(decisionType).withValue(value);
	}
}
