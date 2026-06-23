package se.sundsvall.caremanagement.eventlog.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEvent;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

import static java.time.OffsetDateTime.now;

@Service
@Transactional
public class ErrandEventService {

	private final ErrandEventRepository repository;

	ErrandEventService(final ErrandEventRepository repository) {
		this.repository = repository;
	}

	/**
	 * Records an HTTP-sourced event (from the interceptor): stamps the current time and persists. The single source of
	 * truth for the {@code created} timestamp, so the interceptor need not carry a clock.
	 */
	public void record(final ErrandEventEntity entity) {
		repository.save(entity.withCreated(now()));
	}

	/**
	 * Records a domain-event-sourced row (from the listener): persisted as-is, since {@code created} already carries the
	 * event's own timestamp.
	 */
	public void recordDomainEvent(final ErrandEventEntity entity) {
		repository.save(entity);
	}

	/**
	 * Lists the activity for an errand, newest first, optionally filtered by action and/or actor.
	 *
	 * @param errandId the errand to list activity for
	 * @param action   optional action filter (case-insensitive), e.g. {@code READ}; {@code null} for all
	 * @param actor    optional actor filter (case-insensitive), e.g. an AD account; {@code null} for all
	 */
	@Transactional(readOnly = true)
	public List<ErrandEvent> listForErrand(final String errandId, final String action, final String actor) {
		return repository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.filter(e -> action == null || action.equalsIgnoreCase(e.getAction()))
			.filter(e -> actor == null || actor.equalsIgnoreCase(e.getActor()))
			.map(ErrandEventService::toEvent)
			.toList();
	}

	private static ErrandEvent toEvent(final ErrandEventEntity e) {
		return new ErrandEvent(
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
