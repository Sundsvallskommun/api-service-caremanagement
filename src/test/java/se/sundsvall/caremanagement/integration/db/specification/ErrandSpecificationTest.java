package se.sundsvall.caremanagement.integration.db.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErrandSpecificationTest {

	@Mock
	private Root<ErrandEntity> root;

	@Mock
	private CriteriaQuery<?> query;

	@Mock
	private CriteriaBuilder cb;

	@Mock
	private Predicate predicate;

	@Mock
	private Path<Object> path;

	@Test
	void withNamespaceAndMunicipalityIdAddsBothPredicates() {
		when(root.get("namespace")).thenReturn(path);
		when(root.get("municipalityId")).thenReturn(path);
		when(cb.equal(path, "ns")).thenReturn(predicate);
		when(cb.equal(path, "m1")).thenReturn(predicate);
		when(cb.and(predicate, predicate)).thenReturn(predicate);

		final var result = ErrandSpecification
			.withNamespaceAndMunicipalityId("ns", "m1")
			.toPredicate(root, query, cb);

		assertThat(result).isSameAs(predicate);
		verify(cb).and(predicate, predicate);
	}

	@Test
	void withStatusReturnsEqualPredicateWhenStatusGiven() {
		when(root.get("status")).thenReturn(path);
		when(cb.equal(path, "OPEN")).thenReturn(predicate);

		final var result = ErrandSpecification.withStatus("OPEN").toPredicate(root, query, cb);

		assertThat(result).isSameAs(predicate);
	}

	@Test
	void withStatusReturnsConjunctionWhenStatusIsNull() {
		when(cb.conjunction()).thenReturn(predicate);

		final var result = ErrandSpecification.withStatus(null).toPredicate(root, query, cb);

		assertThat(result).isSameAs(predicate);
		verify(cb).conjunction();
	}

	@Test
	void hasMatchingTagsReturnsConjunctionWhenTagsNull() {
		when(cb.conjunction()).thenReturn(predicate);

		final var result = ErrandSpecification.hasMatchingTags(null).toPredicate(root, query, cb);

		assertThat(result).isSameAs(predicate);
	}

	@Test
	void hasMatchingTagsReturnsConjunctionWhenTagsEmpty() {
		when(cb.conjunction()).thenReturn(predicate);

		final var result = ErrandSpecification.hasMatchingTags(List.of()).toPredicate(root, query, cb);

		assertThat(result).isSameAs(predicate);
	}

	@Test
	void hasMatchingTagsBuildsAndForEachTag() {
		final var tag = TagEmbeddable.create().withKey("k").withValue("v");
		when(root.join("externalTags")).thenReturn((jakarta.persistence.criteria.Join) org.mockito.Mockito.mock(jakarta.persistence.criteria.Join.class));
		final var join = root.join("externalTags");
		when(join.get("key")).thenReturn(path);
		when(join.get("value")).thenReturn(path);
		when(cb.equal(path, "k")).thenReturn(predicate);
		when(cb.equal(path, "v")).thenReturn(predicate);
		when(cb.and(predicate, predicate)).thenReturn(predicate);
		when(cb.and(new Predicate[] {
			predicate
		})).thenReturn(predicate);

		final var result = ErrandSpecification.hasMatchingTags(List.of(tag)).toPredicate(root, query, cb);

		assertThat(result).isNotNull();
	}
}
