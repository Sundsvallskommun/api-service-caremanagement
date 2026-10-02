package se.sundsvall.caremanagement.eventlog.integration.db;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * The actor filter compares the column as-is so MariaDB can use the (actor, created) index; the case-insensitive match
 * the logguppföljning relies on comes from the column's utf8mb4_general_ci collation. This proves it against MariaDB.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class ErrandEventRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final OffsetDateTime CREATED = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, ZoneOffset.UTC);

	@Autowired
	private ErrandEventRepository repository;

	@BeforeEach
	void setUp() {
		repository.save(event("11111111-1111-1111-1111-111111111111", "Joe001Doe"));
		repository.save(event("22222222-2222-2222-2222-222222222222", "someone01else"));
	}

	@Test
	void findsAndCountsAnActorWhateverTheCase() {
		final var found = repository.findByActor(MUNICIPALITY_ID, NAMESPACE, "joe001doe", null, null, null, null, Pageable.ofSize(10));

		assertThat(found).extracting(ErrandEventEntity::getActor).containsExactly("Joe001Doe");
		assertThat(repository.countByActor(MUNICIPALITY_ID, NAMESPACE, "JOE001DOE", null, null, null, null)).isOne();
	}

	@Test
	void doesNotMatchAnotherActor() {
		assertThat(repository.countByActor(MUNICIPALITY_ID, NAMESPACE, "joe001", null, null, null, null)).isZero();
	}

	private static ErrandEventEntity event(final String errandId, final String actor) {
		return ErrandEventEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withSource("HTTP")
			.withAction("READ")
			.withTarget("errand")
			.withActor(actor)
			.withActorType("adAccount")
			.withCreated(CREATED);
	}
}
