package se.sundsvall.caremanagement.eventlog.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Optional;
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

/**
 * Records one {@link ErrandEventEntity} per errand-scoped HTTP request — the "who/what/when" log.
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
	private static final String EVENTS_TARGET = "events";
	private static final String DEFAULT_TARGET = "errand";

	private final ErrandEventService service;

	ErrandEventInterceptor(final ErrandEventService service) {
		this.service = service;
	}

	@Override
	public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, @Nullable final Exception ex) {
		try {
			record(request, response);
		} catch (final Exception e) {
			// An audit write must never break the request the caller already completed.
			LOG.warn("Failed to record errand event for {} {}", request.getMethod(), request.getRequestURI(), e);
		}
	}

	private void record(final HttpServletRequest request, final HttpServletResponse response) {
		final var action = actionFor(request.getMethod());
		if (action == null) {
			return; // not a CRUD method (HEAD/OPTIONS/...) — nothing to log
		}

		// path: ["", municipalityId, namespace, "errands", ...]
		final var parts = request.getRequestURI().split("/");
		if (parts.length < 5 || !ERRANDS_SEGMENT.equals(parts[3])) {
			return; // not an errand route
		}

		final var errandIdIndex = indexOfFirstUuid(parts);
		if (errandIdIndex < 0) {
			return; // collection / eligibility route — no single errand in scope
		}

		final var target = deriveTarget(parts, errandIdIndex);
		if (EVENTS_TARGET.equals(target)) {
			return; // don't log reads of the event log itself
		}

		final var identifier = Optional.ofNullable(Identifier.get());

		service.record(ErrandEventEntity.create()
			.withErrandId(parts[errandIdIndex])
			.withMunicipalityId(parts[1])
			.withNamespace(parts[2])
			.withAction(action)
			.withTarget(target)
			.withDescription(action + " " + target)
			.withHttpMethod(request.getMethod())
			.withRequestPath(request.getRequestURI())
			.withActor(identifier.map(Identifier::getValue).orElse(null))
			.withActorType(identifier.map(Identifier::getTypeString).orElse(null))
			.withRequestId(RequestId.get())
			.withStatusCode(response.getStatus()));
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
		return segments.isEmpty() ? DEFAULT_TARGET : String.join("/", segments);
	}

	private static boolean isIdLike(final String segment) {
		return UUID_SEGMENT.matcher(segment).matches() || NUMERIC_SEGMENT.matcher(segment).matches();
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
