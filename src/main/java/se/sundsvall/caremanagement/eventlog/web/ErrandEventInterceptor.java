package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.support.Identifier;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Records one {@link ErrandEventEntity} per errand-scoped HTTP request — the “who/what/when” log.
 *
 * Runs in {@code afterCompletion}, on the request thread, where the dept44 {@link Identifier} (the {@code X-Sent-By}
 * actor) and {@link RequestId} are still bound — unlike a Modulith {@code @ApplicationModuleListener}, which runs on a
 * different thread after commit and would have lost them. Recording never breaks the request: any failure is swallowed
 * with a warning, since an audit write must not fail a read or a write the caller already completed.
 *
 * <p>
 * Two errand path shapes are recognised, both matched by locating the errand id as the first UUID segment after the
 * {@code errands} segment:
 * <ul>
 * <li>{@code /{municipalityId}/{namespace}/errands/{errandId}/...}</li>
 * <li>{@code /{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/...}</li>
 * </ul>
 * Routes with no errand id (collection endpoints, the {@code .../errands/financial-assistance/eligibility} routing) are
 * skipped, as are reads of the event log itself.
 */
@Component
class ErrandEventInterceptor implements HandlerInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandEventInterceptor.class);

	private static final Pattern UUID_SEGMENT = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	private static final Pattern NUMERIC_SEGMENT = Pattern.compile("^\\d+$");

	private static final String ERRANDS_SEGMENT = "errands";
	private static final String DEFAULT_TARGET = "errand";
	/** Target on the rows a search leaves behind, so a follow-up can tell a list sighting from an opened case. */
	private static final String SEARCH_TARGET = "errands/search";

	/**
	 * Targets that must never produce an access-log row: reads of the event log itself, the report of accesses made in
	 * Lifecare (those rows <em>are</em> the log entry — logging the report as well would double every one), and the
	 * conversation read-state
	 * machinery (the polled unread-count and the mark-as-read call) — recording those would drown the who/what/when log
	 * in noise and is explicitly not wanted. Any {@code .../count} target (the per-resource badge counts) is likewise
	 * skipped via {@link #isNonAudited(String)}.
	 */
	private static final Set<String> NON_AUDITED_TARGETS = Set.of("events", "events/lifecare", "messages/unread-count", "messages/read");

	private static boolean isNonAudited(final String target) {
		return NON_AUDITED_TARGETS.contains(target) || target.endsWith("/count");
	}

	private final ErrandEventService service;

	ErrandEventInterceptor(final ErrandEventService service) {
		this.service = service;
	}

	@Override
	public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, @Nullable final Exception ex) {
		try {
			recordEvent(request, response);
		} catch (final Exception e) {
			// An audit write must never break the request the caller already completed.
			LOG.warn("Failed to record errand event for {} {}", sanitizeForLogging(request.getMethod()), sanitizeForLogging(request.getRequestURI()), e);
		}
	}

	private void recordEvent(final HttpServletRequest request, final HttpServletResponse response) {
		final var action = actionFor(request.getMethod());
		if (action == null) {
			return; // not a CRUD method (HEAD/OPTIONS/...) — nothing to log
		}

		// path: ["", municipalityId, namespace, "errands", ...]
		final var parts = request.getRequestURI().split("/");
		if (parts.length < 4 || !ERRANDS_SEGMENT.equals(parts[3])) {
			return; // not an errand route
		}

		final var errandIdIndex = indexOfFirstUuid(parts);
		if (errandIdIndex < 0) {
			// Collection / eligibility route — no single errand in scope. A search is the one case that still has to
			// leave a trace, and it leaves one per hit; everything else here is genuinely errand-less.
			recordSearchHits(request, response, parts, action);
			return;
		}

		final var target = deriveTarget(parts, errandIdIndex);
		if (isNonAudited(target)) {
			return; // event-log reads, conversation read-state calls, and badge counts are never audited
		}

		final var identifier = Optional.ofNullable(Identifier.get());
		final var description = ErrandEventDescriber.describe(request.getMethod(), tailSegments(parts, errandIdIndex), hasItemId(parts, errandIdIndex));

		service.recordEvent(ErrandEventEntity.create()
			.withErrandId(parts[errandIdIndex])
			.withMunicipalityId(parts[1])
			.withNamespace(parts[2])
			.withSource("HTTP")
			.withAction(action)
			.withTarget(target)
			.withDescription(description)
			.withHttpMethod(request.getMethod())
			.withRequestPath(request.getRequestURI())
			.withActor(identifier.map(Identifier::getValue).orElse(null))
			.withActorType(identifier.map(Identifier::getTypeString).orElse(null))
			.withRequestId(RequestId.get())
			.withStatusCode(response.getStatus()));
	}

	/**
	 * Records one row per errand a search returned — verksamhetens regelverk (revision 2026-09-22): <em>”Det måste
	 * loggas när man gör ett stort 'sök' på alla träffar man har sett, alltså första sidan om man inte har bläddrat,
	 * loggas på ärendet men även på användare.”</em>
	 * <p>
	 * One row per hit rather than one row for the search, because that is what the sentence asks for on both counts:
	 * the rows land on the errands (so a case shows who has seen it, even via a list) and they carry the actor (so the
	 * per-actor follow-up finds them). It also means the existing {@code errand_id NOT NULL} shape needs no sentinel.
	 * <p>
	 * “Träffar man har sett” is the returned page, captured by {@link SearchHitsCollector} before the body was
	 * written. A caseworker who never pages sees one page; one who pages generates a fresh request, and a fresh set of
	 * rows.
	 * <p>
	 * Silent when there are no stashed hits, which covers every other errand-less route (eligibility, counts,
	 * metadata) as well as a search that matched nothing — there is no disclosure to record when nothing was shown.
	 */
	private void recordSearchHits(final HttpServletRequest request, final HttpServletResponse response, final String[] parts, final String action) {
		if (!(request.getAttribute(SearchHitsCollector.SEARCH_HITS_ATTRIBUTE) instanceof final List<?> hits) || hits.isEmpty()) {
			return;
		}

		final var identifier = Optional.ofNullable(Identifier.get());
		final var description = "Såg ärendet i en sökträfflista (%d träffar)".formatted(hits.size());

		hits.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.forEach(errandId -> service.recordEvent(ErrandEventEntity.create()
				.withErrandId(errandId)
				.withMunicipalityId(parts[1])
				.withNamespace(parts[2])
				.withSource("HTTP")
				.withAction(action)
				.withTarget(SEARCH_TARGET)
				.withDescription(description)
				.withHttpMethod(request.getMethod())
				.withRequestPath(request.getRequestURI())
				.withActor(identifier.map(Identifier::getValue).orElse(null))
				.withActorType(identifier.map(Identifier::getTypeString).orElse(null))
				.withRequestId(RequestId.get())
				.withStatusCode(response.getStatus())));
	}

	private static int indexOfFirstUuid(final String[] parts) {
		for (var i = 4; i < parts.length; i++) {
			if (UUID_SEGMENT.matcher(parts[i]).matches()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Builds a human/machine-friendly target from the path: any non-id type prefix between {@code errands} and the
	 * errand id (e.g. {@code financial-assistance}) joined with the non-id segments after the errand id (e.g.
	 * {@code calculation/draft/incomes}). Falls back to {@code errand} when the errand id is the last meaningful segment.
	 */
	private static String deriveTarget(final String[] parts, final int errandIdIndex) {
		final var segments = new ArrayList<String>();
		for (var i = 4; i < parts.length; i++) {
			if (i == errandIdIndex) {
				continue;
			}
			final var segment = parts[i];
			if (!segment.isBlank() && !isIdLike(segment)) {
				segments.add(segment);
			}
		}
		if (segments.isEmpty()) {
			return DEFAULT_TARGET;
		}
		return String.join("/", segments);
	}

	private static boolean isIdLike(final String segment) {
		return UUID_SEGMENT.matcher(segment).matches() || NUMERIC_SEGMENT.matcher(segment).matches();
	}

	/** The non-id, non-blank path segments after the errand id — the sub-resource the request acted on. */
	private static List<String> tailSegments(final String[] parts, final int errandIdIndex) {
		final var tail = new ArrayList<String>();
		for (var i = errandIdIndex + 1; i < parts.length; i++) {
			if (!parts[i].isBlank() && !isIdLike(parts[i])) {
				tail.add(parts[i]);
			}
		}
		return tail;
	}

	/** True when the request targeted a specific sub-item (an id segment appeared after the errand id). */
	private static boolean hasItemId(final String[] parts, final int errandIdIndex) {
		for (var i = errandIdIndex + 1; i < parts.length; i++) {
			if (isIdLike(parts[i])) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private static String actionFor(final String method) {
		return switch (method) {
			case "GET" -> "READ";
			case "POST" -> "CREATE";
			case "PUT", "PATCH" -> "UPDATE";
			case "DELETE" -> "DELETE";
			default -> null;
		};
	}
}
