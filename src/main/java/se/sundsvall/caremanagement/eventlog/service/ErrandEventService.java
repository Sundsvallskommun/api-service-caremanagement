package se.sundsvall.caremanagement.eventlog.service;

import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventEntry;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

import static java.time.OffsetDateTime.now;

@Service
@Transactional
public class ErrandEventService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandEventService.class);

	private final ErrandEventRepository repository;

	ErrandEventService(final ErrandEventRepository repository) {
		this.repository = repository;
	}

	/**
	 * Records an HTTP-sourced event (from the interceptor): stamps the current time and persists. The single source of
	 * truth for the {@code created} timestamp, so the interceptor need not carry a clock.
	 */
	public void recordEvent(final ErrandEventEntity entity) {
		repository.save(entity.withCreated(now(ZoneId.systemDefault())));
	}

	/**
	 * Records a domain-event-sourced row (from the listener): persisted as-is, since {@code created} already carries the
	 * event's own timestamp.
	 */
	public void recordDomainEvent(final ErrandEventEntity entity) {
		repository.save(entity);
	}

	/**
	 * Disposes an errand's entire activity log when the errand itself is deleted (gallrat). The event log is a legal
	 * who/what/when record kept for the life of the errand, so it is removed only here — never on a time or size basis —
	 * matching how the other modules clean up their errand-scoped data on {@code ErrandDeleted}.
	 */
	public void deleteForErrand(final String municipalityId, final String namespace, final String errandId) {
		final var deleted = repository.deleteByErrandIdAndMunicipalityIdAndNamespace(errandId, municipalityId, namespace);
		LOG.info("Disposed {} event(s) for deleted errand {}", deleted, errandId);
	}

	/**
	 * Lists the activity for an errand, newest first, with optional filters. Scoped to the owning tenant
	 * ({@code municipalityId} + {@code namespace}): a foreign tenant/namespace yields an empty list rather than another
	 * errand's activity log — closing the cross-tenant read on the audit trail.
	 *
	 * @param municipalityId the owning municipality
	 * @param namespace      the owning namespace
	 * @param errandId       the errand to list activity for
	 * @param action         optional action filter (case-insensitive), e.g. {@code READ}; {@code null} for all
	 * @param actor          optional actor filter (case-insensitive), e.g. an AD account; {@code null} for all
	 * @param source         optional source filter (case-insensitive): {@code HTTP} or {@code EVENT}; {@code null} for all
	 * @param includeReads   when {@code false}, drops READ rows — a clean "what changed" timeline without the read noise
	 */
	@Transactional(readOnly = true)
	public List<ErrandEventEntry> listForErrand(final String municipalityId, final String namespace, final String errandId, final String action, final String actor, final String source, final boolean includeReads) {
		return repository.findFiltered(municipalityId, namespace, errandId, action, actor, source, includeReads).stream()
			.map(ErrandEventService::toEvent)
			.toList();
	}

	/**
	 * Counts the activity for an errand, honouring the same tenant scope and filters as {@link #listForErrand}. With the
	 * defaults ({@code includeReads=true}, no other filter) this is the total event count; {@code includeReads=false}
	 * yields the "what changed" count without the read noise. Counted DB-side, so it does not materialise the rows.
	 */
	@Transactional(readOnly = true)
	public long countForErrand(final String municipalityId, final String namespace, final String errandId, final String action, final String actor, final String source, final boolean includeReads) {
		return repository.countFiltered(municipalityId, namespace, errandId, action, actor, source, includeReads);
	}

	private static ErrandEventEntry toEvent(final ErrandEventEntity e) {
		return new ErrandEventEntry(
			e.getId(),
			e.getErrandId(),
			e.getMunicipalityId(),
			e.getNamespace(),
			e.getSource(),
			e.getAction(),
			e.getTarget(),
			e.getDescription(),
			e.getHttpMethod(),
			e.getRequestPath(),
			e.getActor(),
			e.getActorType(),
			e.getRequestId(),
			e.getStatusCode(),
			e.getCreated());
	}
}
