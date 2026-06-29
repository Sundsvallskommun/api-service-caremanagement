package se.sundsvall.caremanagement.core.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

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

	static Specification<ErrandEntity> withTypeSlug(final String typeSlug) {
		return (root, _, cb) -> ofNullable(typeSlug)
			.<Predicate>map(value -> cb.equal(root.get("typeSlug"), value))
			.orElseGet(cb::conjunction);
	}
}
