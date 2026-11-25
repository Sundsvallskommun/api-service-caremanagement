package se.sundsvall.caremanagement.core.integration.db;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.selection;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withNamespaceAndMunicipalityId;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withStatus;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withTypeSlug;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class ErrandRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String OTHER_MUNICIPALITY_ID = "2280";
	private static final String NAMESPACE = "my-namespace";
	private static final String OTHER_NAMESPACE = "other-namespace";
	private static final OffsetDateTime MAY = OffsetDateTime.parse("2026-05-15T00:00:00Z");
	private static final OffsetDateTime JUNE = OffsetDateTime.parse("2026-06-15T00:00:00Z");
	private static final OffsetDateTime JULY = OffsetDateTime.parse("2026-07-15T00:00:00Z");

	@Autowired
	private ErrandRepository repository;

	@BeforeEach
	void setUp() {
		final var errands = repository.saveAll(List.of(
			errand(MUNICIPALITY_ID, NAMESPACE, "OPEN", "financial-assistance-new", "ERRAND-1"),
			errand(MUNICIPALITY_ID, NAMESPACE, "CLOSED", "financial-assistance-renewal", "ERRAND-2"),
			errand(MUNICIPALITY_ID, OTHER_NAMESPACE, "OPEN", "financial-assistance-new", "ERRAND-3"),
			errand(OTHER_MUNICIPALITY_ID, NAMESPACE, "OPEN", "financial-assistance-new", "ERRAND-4")));

		// AuditableListener@PrePersist stamps created=now during save(); override to fixed dates before flush INSERTs.
		errands.get(0).setCreated(MAY);
		errands.get(1).setCreated(JUNE);
		errands.get(2).setCreated(JULY);
		errands.get(3).setCreated(JULY);
		repository.flush();
	}

	@Test
	void withNamespaceAndMunicipalityIdReturnsTenantRows() {
		final var result = repository.findAll(withNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrder("ERRAND-1", "ERRAND-2");
	}

	@Test
	void withStatusFiltersWhenPresent() {
		final var result = repository.findAll(withNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)
			.and(withStatus("OPEN")));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactly("ERRAND-1");
	}

	@Test
	void withStatusNullDoesNotFilter() {
		final var result = repository.findAll(withNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)
			.and(withStatus(null)));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrder("ERRAND-1", "ERRAND-2");
	}

	@Test
	void withTypeSlugFiltersWhenPresent() {
		final var result = repository.findAll(withNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)
			.and(withTypeSlug("financial-assistance-renewal")));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactly("ERRAND-2");
	}

	@Test
	void withTypeSlugNullDoesNotFilter() {
		final var result = repository.findAll(withNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)
			.and(withTypeSlug(null)));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrder("ERRAND-1", "ERRAND-2");
	}

	@Test
	void selectionFiltersByTenantTypeSlugAndCreatedRange() {
		final var result = repository.findAll(selection(NAMESPACE, MUNICIPALITY_ID, "financial-assistance-renewal",
			OffsetDateTime.parse("2026-06-01T00:00:00Z"), OffsetDateTime.parse("2026-06-30T23:59:59Z")));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactly("ERRAND-2");
	}

	@Test
	void selectionSkipsOptionalFiltersWhenNull() {
		final var result = repository.findAll(selection(NAMESPACE, MUNICIPALITY_ID, null, null, null));

		assertThat(result).extracting(ErrandEntity::getErrandNumber)
			.containsExactlyInAnyOrder("ERRAND-1", "ERRAND-2");
	}

	@Test
	void findWithoutProcessInstanceSelectsUnstartedReceivedErrandsOfTheTypesInTheWindow() {
		final var now = OffsetDateTime.now();
		save(errand("MATCH-NEW", "RECEIVED", "financial-assistance-new", now.minusHours(1)),
			errand("MATCH-RENEWAL", "RECEIVED", "financial-assistance-renewal", now.minusHours(2)),
			// the other side of every condition, one at a time
			errand("STARTED", "RECEIVED", "financial-assistance-new", now.minusHours(1)).withProcessInstanceId("instance-1"),
			errand("FROZEN", "NEEDS_MANUAL_REVIEW", "financial-assistance-new", now.minusHours(1)),
			errand("UNDER-REVIEW", "UNDER_REVIEW", "financial-assistance-new", now.minusHours(1)),
			errand("OTHER-TYPE", "RECEIVED", "another-type", now.minusHours(1)),
			errand("TOO-YOUNG", "RECEIVED", "financial-assistance-new", now.minusMinutes(1)),
			errand("TOO-OLD", "RECEIVED", "financial-assistance-new", now.minusDays(30)),
			errand("OTHER-NAMESPACE", "RECEIVED", "financial-assistance-new", now.minusHours(1)).withNamespace(OTHER_NAMESPACE),
			errand("OTHER-MUNICIPALITY", "RECEIVED", "financial-assistance-new", now.minusHours(1)).withMunicipalityId(OTHER_MUNICIPALITY_ID));

		final var result = repository.findWithoutProcessInstance(MUNICIPALITY_ID, NAMESPACE,
			List.of("financial-assistance-new", "financial-assistance-renewal"), "RECEIVED", now.minusDays(7), now.minusMinutes(10));

		// oldest first
		assertThat(result).extracting(ErrandEntity::getErrandNumber).containsExactly("MATCH-RENEWAL", "MATCH-NEW");
	}

	@Test
	void findWithoutProcessInstanceIncludesTheBoundsOfTheWindow() {
		save(errand("AT-FROM", "RECEIVED", "financial-assistance-new", MAY),
			errand("AT-TO", "RECEIVED", "financial-assistance-new", JUNE),
			errand("BEFORE", "RECEIVED", "financial-assistance-new", MAY.minusSeconds(1)),
			errand("AFTER", "RECEIVED", "financial-assistance-new", JUNE.plusSeconds(1)));

		final var result = repository.findWithoutProcessInstance(MUNICIPALITY_ID, NAMESPACE, List.of("financial-assistance-new"), "RECEIVED", MAY, JUNE);

		assertThat(result).extracting(ErrandEntity::getErrandNumber).containsExactly("AT-FROM", "AT-TO");
	}

	@Test
	void findWithoutProcessInstanceIsEmptyForNoTypes() {
		save(errand("E", "RECEIVED", "financial-assistance-new", OffsetDateTime.now().minusHours(1)));

		assertThat(repository.findWithoutProcessInstance(MUNICIPALITY_ID, NAMESPACE, List.of(), "RECEIVED", OffsetDateTime.now().minusDays(7), OffsetDateTime.now())).isEmpty();
	}

	/**
	 * Saves the errands with the {@code created} each was given: the auditing listener stamps now on insert, so it is put
	 * back before the flush.
	 */
	private void save(final ErrandEntity... errands) {
		final var created = new ArrayList<OffsetDateTime>();
		for (final var errand : errands) {
			created.add(errand.getCreated());
		}
		final var saved = repository.saveAll(List.of(errands));
		for (var i = 0; i < saved.size(); i++) {
			saved.get(i).setCreated(created.get(i));
		}
		repository.flush();
	}

	private static ErrandEntity errand(final String errandNumber, final String status, final String typeSlug, final OffsetDateTime created) {
		return errand(MUNICIPALITY_ID, NAMESPACE, status, typeSlug, errandNumber).withCreated(created);
	}

	private static ErrandEntity errand(final String municipalityId, final String namespace, final String status, final String typeSlug,
		final String errandNumber) {
		return ErrandEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withStatus(status)
			.withTypeSlug(typeSlug)
			.withTitle("Errand " + errandNumber)
			.withErrandNumber(errandNumber);
	}
}
