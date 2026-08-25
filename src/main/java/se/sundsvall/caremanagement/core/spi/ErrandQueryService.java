package se.sundsvall.caremanagement.core.spi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.service.mapper.ErrandMapper;

import static se.sundsvall.caremanagement.core.integration.db.specification.ErrandSpecification.selection;

/**
 * Core-owned, read-side query facade over the errand envelope, exposed via the {@code spi} named interface so other
 * modules can look an errand up or aggregate errand read-model fields without depending on core's persistence layer
 * (the {@code ErrandRepository} and {@code ErrandEntity} are core-internal). The JPA entity never crosses the boundary
 * —
 * lookups return the {@link Errand} API model and aggregations return {@link ErrandStatusView} projections. Mirrors the
 * {@code conversation.spi} query-service pattern.
 */
@Service
public class ErrandQueryService {

	private final ErrandRepository errandRepository;

	ErrandQueryService(final ErrandRepository errandRepository) {
		this.errandRepository = errandRepository;
	}

	/**
	 * Looks an errand up by id within the tenant, as the {@link Errand} API model. Empty when no such errand exists in
	 * the namespace/municipality — callers that must fail hard {@code .orElseThrow(...)}; async listeners simply skip, so
	 * a since-deleted errand never wedges a retry.
	 */
	@Transactional(readOnly = true)
	public Optional<Errand> findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.map(ErrandMapper::toErrand);
	}

	/**
	 * Existence check that takes a pessimistic write lock on the errand row. Intentionally has no {@code @Transactional}
	 * of its own so it joins the caller's transaction — the lock must be held for the duration of the caller's
	 * check-then-write, which is the whole point (concurrent callers for the same errand block here rather than both
	 * passing the check). Returns {@code false} when no such errand exists in the namespace/municipality.
	 */
	public boolean existsWithLock(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId);
	}

	/**
	 * The errands in a namespace (optionally narrowed by {@code typeSlug} and a {@code created} range), projected to just
	 * the fields the statistics aggregation needs. The selection spec is owned here so the statistics module never
	 * reaches into core's persistence layer.
	 */
	@Transactional(readOnly = true)
	public List<ErrandStatusView> findStatusViews(final String municipalityId, final String namespace, final String typeSlug,
		final OffsetDateTime from, final OffsetDateTime to) {
		return errandRepository.findAll(selection(namespace, municipalityId, typeSlug, from, to)).stream()
			.map(errand -> new ErrandStatusView(errand.getStatus(), errand.getAssignedUserId()))
			.toList();
	}
}
