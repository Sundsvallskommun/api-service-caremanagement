package se.sundsvall.caremanagement.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

public interface ErrandSpecification {

	static Specification<ErrandEntity> withNamespaceAndMunicipalityId(final String namespace, final String municipalityId) {
		return (root, _, cb) -> cb.and(
			cb.equal(root.get("namespace"), namespace),
			cb.equal(root.get("municipalityId"), municipalityId));
	}

	static Specification<ErrandEntity> withStatus(final String status) {
		return (root, _, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
	}

	static Specification<ErrandEntity> hasMatchingTags(final List<TagEmbeddable> tags) {
		return (root, _, cb) -> {
			if ((tags == null) || tags.isEmpty()) {
				return cb.conjunction();
			}

			final var predicates = new ArrayList<Predicate>();
			for (final TagEmbeddable tag : tags) {
				predicates.add(cb.and(
					cb.equal(root.join("externalTags").get("key"), tag.getKey()),
					cb.equal(root.join("externalTags").get("value"), tag.getValue())));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
