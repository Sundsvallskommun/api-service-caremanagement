package se.sundsvall.caremanagement.statistics.service;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.core.spi.ErrandStatusView;
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
 * {@code created} date range) as {@link ErrandStatusView} projections from the core query facade and aggregates them in
 * memory — counts per status, counts per assigned user, and the number of unassigned errands. Status values are
 * namespace-defined free text, so the aggregation is intentionally lifecycle-agnostic.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private static final String UNKNOWN_STATUS = "UNKNOWN";

	private final ErrandQueryService errandQueryService;

	StatisticsService(final ErrandQueryService errandQueryService) {
		this.errandQueryService = errandQueryService;
	}

	public StatisticsResponse compute(final String municipalityId, final String namespace, final String typeSlug,
		final OffsetDateTime from, final OffsetDateTime to) {

		final var errands = errandQueryService.findStatusViews(municipalityId, namespace, typeSlug, from, to);

		final var byStatus = errands.stream()
			.collect(groupingBy(errand -> ofNullable(errand.status()).orElse(UNKNOWN_STATUS), counting()))
			.entrySet().stream()
			.map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
			.sorted(comparing(StatusCount::status))
			.toList();

		final var byAssignee = errands.stream()
			.filter(errand -> hasText(errand.assignedUserId()))
			.collect(groupingBy(ErrandStatusView::assignedUserId, counting()))
			.entrySet().stream()
			.map(entry -> new AssigneeCount(entry.getKey(), entry.getValue()))
			.sorted(comparing(AssigneeCount::assignedUserId))
			.toList();

		final var unassigned = errands.stream().filter(errand -> !hasText(errand.assignedUserId())).count();

		return new StatisticsResponse(errands.size(), byStatus, byAssignee, unassigned);
	}
}
