package se.sundsvall.caremanagement.statistics.service;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.statistics.api.model.AssigneeCount;
import se.sundsvall.caremanagement.statistics.api.model.StatisticsResponse;
import se.sundsvall.caremanagement.statistics.api.model.StatusCount;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.springframework.util.StringUtils.hasText;

/**
 * Computes read-only errand statistics. Loads the errands for the namespace (optionally scoped by typeSlug and a
 * {@code created} date range) and aggregates them in memory — counts per status, counts per assigned user, and the
 * number of unassigned errands. Status values are namespace-defined free text, so the aggregation is intentionally
 * lifecycle-agnostic.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private static final String UNKNOWN_STATUS = "UNKNOWN";

	private final ErrandRepository errandRepository;

	StatisticsService(final ErrandRepository errandRepository) {
		this.errandRepository = errandRepository;
	}

	public StatisticsResponse compute(final String municipalityId, final String namespace, final String typeSlug,
		final OffsetDateTime from, final OffsetDateTime to) {

		final var errands = errandRepository.findAll(selection(municipalityId, namespace, typeSlug, from, to));

		final var byStatus = errands.stream()
			.collect(groupingBy(errand -> ofNullable(errand.getStatus()).orElse(UNKNOWN_STATUS), counting()))
			.entrySet().stream()
			.map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
			.sorted(comparing(StatusCount::status))
			.toList();

		final var byAssignee = errands.stream()
			.filter(errand -> hasText(errand.getAssignedUserId()))
			.collect(groupingBy(ErrandEntity::getAssignedUserId, counting()))
			.entrySet().stream()
			.map(entry -> new AssigneeCount(entry.getKey(), entry.getValue()))
			.sorted(comparing(AssigneeCount::assignedUserId))
			.toList();

		final var unassigned = errands.stream().filter(errand -> !hasText(errand.getAssignedUserId())).count();

		return new StatisticsResponse(errands.size(), byStatus, byAssignee, unassigned);
	}

	/**
	 * Selection spec built locally over the (exposed) {@code ErrandEntity} — namespace + municipality, optionally
	 * narrowed by typeSlug and a {@code created} range. Kept self-contained so the statistics module doesn't reach into
	 * core's internal specification package.
	 */
	private static Specification<ErrandEntity> selection(final String municipalityId, final String namespace,
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
