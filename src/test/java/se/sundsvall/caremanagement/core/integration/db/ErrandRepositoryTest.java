package se.sundsvall.caremanagement.core.integration.db;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.selection;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withNamespaceAndMunicipalityId;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withStatus;
import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.withTypeSlug;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
@Transactional
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
