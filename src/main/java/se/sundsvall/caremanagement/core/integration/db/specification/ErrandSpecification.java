package se.sundsvall.caremanagement.core.integration.db.specification;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

public interface ErrandSpecification {

	public static Specification<ErrandEntity> withNamespaceAndMunicipalityId(final String namespace, final String municipalityId) {
		return (root, _, cb) -> cb.and(
			cb.equal(root.get("namespace"), namespace),
			cb.equal(root.get("municipalityId"), municipalityId));
	}

	public static Specification<ErrandEntity> withStatus(final String status) {
		return (root, _, cb) -> ofNullable(status)
			.<Predicate>map(value -> cb.equal(root.get("status"), value))
			.orElseGet(cb::conjunction);
	}

	public static Specification<ErrandEntity> withTypeSlug(final String typeSlug) {
		return (root, _, cb) -> ofNullable(typeSlug)
			.<Predicate>map(value -> cb.equal(root.get("typeSlug"), value))
			.orElseGet(cb::conjunction);
	}

	public static Specification<ErrandEntity> selection(final String namespace, final String municipalityId,
		final String typeSlug, final OffsetDateTime from, final OffsetDateTime to) {
		return (root, _, cb) -> {
			final var predicates = new ArrayList<Predicate>();
			predicates.add(cb.equal(root.get("namespace"), namespace));
			predicates.add(cb.equal(root.get("municipalityId"), municipalityId));
			if (hasText(typeSlug)) {
				predicates.add(cb.equal(root.get("typeSlug"), typeSlug));
			}
			ofNullable(from).ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("created"), value)));
			ofNullable(to).ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("created"), value)));
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
