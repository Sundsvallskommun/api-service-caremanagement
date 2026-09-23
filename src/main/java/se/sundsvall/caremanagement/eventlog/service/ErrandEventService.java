package se.sundsvall.caremanagement.eventlog.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.api.model.ActorEventLog;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventEntry;
import se.sundsvall.caremanagement.eventlog.api.model.LifecareAccess;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.support.Identifier;

import static java.time.OffsetDateTime.now;

@Service
@Transactional
public class ErrandEventService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandEventService.class);

	/**
	 * The most rows one logguppföljning read returns. An actor's history spans every errand they have opened, so the
	 * unbounded answer is unbounded in the literal sense; the total is returned with the page so the cap is visible
	 * rather than silent.
	 */
	static final int ACTOR_EVENT_LIMIT = 1000;

	/** Source of a row reported by a caller that read or wrote Lifecare directly (Draken's BFF). */
	static final String SOURCE_LIFECARE = "LIFECARE";

	private final ErrandEventRepository errandEventRepository;
	private final ErrandAccessGuard errandAccessGuard;

	ErrandEventService(final ErrandEventRepository errandEventRepository, final ErrandAccessGuard errandAccessGuard) {
		this.errandEventRepository = errandEventRepository;
		this.errandAccessGuard = errandAccessGuard;
	}

	/**
	 * Records an HTTP-sourced event (from the interceptor): stamps the current time and persists. The single source of
	 * truth for the {@code created} timestamp, so the interceptor need not carry a clock.
	 */
	public void recordEvent(final ErrandEventEntity entity) {
		errandEventRepository.save(entity.withCreated(now(ZoneId.systemDefault())));
	}

	/**
	 * Records reads and writes a caller made in Lifecare directly on the errand's behalf — Draken's BFF, which reads
	 * journal, documents, reminders and jobbstimulans live from Lifecare and writes journal notes, documents and
	 * reminders straight into it. None of that passes this service's own request logging, so the caller reports it and
	 * it lands in the same who/what/when log, under source {@code LIFECARE}, attributed to the caller's
	 * {@code X-Sent-By} identity.
	 *
	 * @param caller   the reporting caller's identity, from the {@code X-Sent-By} header
	 * @param accesses what was accessed in Lifecare, one row each
	 */
	public void recordLifecareAccesses(final String municipalityId, final String namespace, final String errandId, final Identifier caller,
		final List<LifecareAccess> accesses) {
		errandAccessGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		final var created = now(ZoneId.systemDefault());
		final var requestId = RequestId.get();
		errandEventRepository.saveAll(accesses.stream()
			.map(access -> toLifecareEntity(municipalityId, namespace, errandId, caller, requestId, created, access))
			.toList());
	}

	/**
	 * Records a domain-event-sourced row (from the listener): persisted as-is, since {@code created} already carries the
	 * event's own timestamp.
	 */
	public void recordDomainEvent(final ErrandEventEntity entity) {
		errandEventRepository.save(entity);
	}

	/**
	 * Disposes an errand's entire activity log when the errand itself is deleted (gallrat). The event log is a legal
	 * who/what/when record kept for the life of the errand, so it is removed only here — never on a time or size basis —
	 * matching how the other modules clean up their errand-scoped data on {@code ErrandDeleted}.
	 */
	public void deleteForErrand(final String municipalityId, final String namespace, final String errandId) {
		final var deleted = errandEventRepository.deleteByErrandIdAndMunicipalityIdAndNamespace(errandId, municipalityId, namespace);
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
	 * @param includeReads   when {@code false}, drops READ rows — a clean “what changed” timeline without the read noise
	 */
	@Transactional(readOnly = true)
	public List<ErrandEventEntry> listForErrand(final String municipalityId, final String namespace, final String errandId, final String action, final String actor, final String source, final boolean includeReads) {
		return errandEventRepository.findFiltered(municipalityId, namespace, errandId, action, actor, source, includeReads).stream()
			.map(ErrandEventService::toEvent)
			.toList();
	}

	/**
	 * Lists one actor's activity across every errand in the tenant, newest first — the logguppföljning read behind
	 * verksamhetens <em>”måste kunna söka upp loggar på en användare, inte bara per ärende”</em>.
	 * <p>
	 * Capped at {@value #ACTOR_EVENT_LIMIT} rows. The total is counted separately and returned alongside, so a
	 * follow-up never mistakes a truncated page for the whole picture — an audit answer that silently stops short is
	 * worse than no answer. Narrow with {@code from}/{@code to} to see the rest.
	 *
	 * @param actor  the actor to follow up, matched case-insensitively against the {@code X-Sent-By} identity
	 * @param action optional action filter (case-insensitive), e.g. {@code READ}; {@code null} for all
	 * @param source optional source filter (case-insensitive): {@code HTTP} or {@code EVENT}; {@code null} for all
	 * @param from   optional inclusive lower bound on {@code created}; {@code null} for unbounded
	 * @param to     optional exclusive upper bound on {@code created}; {@code null} for unbounded
	 */
	@Transactional(readOnly = true)
	public ActorEventLog listForActor(final String municipalityId, final String namespace, final String actor, final String action, final String source,
		final OffsetDateTime from, final OffsetDateTime to) {
		final var events = errandEventRepository.findByActor(municipalityId, namespace, actor, action, source, from, to, PageRequest.of(0, ACTOR_EVENT_LIMIT)).stream()
			.map(ErrandEventService::toEvent)
			.toList();
		final var total = errandEventRepository.countByActor(municipalityId, namespace, actor, action, source, from, to);

		return new ActorEventLog(events, total);
	}

	/**
	 * Counts the activity for an errand, honouring the same tenant scope and filters as {@link #listForErrand}. With the
	 * defaults ({@code includeReads=true}, no other filter) this is the total event count; {@code includeReads=false}
	 * yields the “what changed” count without the read noise. Counted DB-side, so it does not materialise the rows.
	 */
	@Transactional(readOnly = true)
	public long countForErrand(final String municipalityId, final String namespace, final String errandId, final String action, final String actor, final String source, final boolean includeReads) {
		return errandEventRepository.countFiltered(municipalityId, namespace, errandId, action, actor, source, includeReads);
	}

	/**
	 * One reported Lifecare access as a log row. The description falls back to "{@code ACTION target}", the same shape
	 * the request log uses when it has nothing better, so the list never shows an empty line.
	 */
	private static ErrandEventEntity toLifecareEntity(final String municipalityId, final String namespace, final String errandId, final Identifier caller,
		final String requestId, final OffsetDateTime created, final LifecareAccess access) {
		return ErrandEventEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withSource(SOURCE_LIFECARE)
			.withAction(access.getAction())
			.withTarget(access.getTarget())
			.withDescription(Optional.ofNullable(access.getDescription())
				.filter(StringUtils::hasText)
				.orElseGet(() -> access.getAction() + " " + access.getTarget()))
			.withLifecareId(access.getLifecareId())
			.withActor(caller.getValue())
			.withActorType(caller.getTypeString())
			.withRequestId(requestId)
			.withCreated(created);
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
			e.getLifecareId(),
			e.getActor(),
			e.getActorType(),
			e.getRequestId(),
			e.getStatusCode(),
			e.getCreated());
	}
}
