package se.sundsvall.caremanagement.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static java.util.Optional.ofNullable;

public interface ErrandSpecification {

	static Specification<ErrandEntity> withNamespaceAndMunicipalityId(final String namespace, final String municipalityId) {
		return (root, _, cb) -> cb.and(
			cb.equal(root.get("namespace"), namespace),
			cb.equal(root.get("municipalityId"), municipalityId));
	}

	static Specification<ErrandEntity> withStatus(final String status) {
		return (root, _, cb) -> ofNullable(status)
			.<Predicate>map(value -> cb.equal(root.get("status"), value))
			.orElseGet(cb::conjunction);
	}

	static Specification<ErrandEntity> hasMatchingTags(final List<TagEmbeddable> tags) {
		return (root, _, cb) -> ofNullable(tags)
			.filter(tagList -> !tagList.isEmpty())
			.<Predicate>map(tagList -> {
				final var predicates = new ArrayList<Predicate>();
				for (final TagEmbeddable tag : tagList) {
					predicates.add(cb.and(
						cb.equal(root.join("externalTags").get("key"), tag.getKey()),
						cb.equal(root.join("externalTags").get("value"), tag.getValue())));
				}
				return cb.and(predicates.toArray(new Predicate[0]));
			})
			.orElseGet(cb::conjunction);
	}
}
