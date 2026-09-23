package se.sundsvall.caremanagement.eventlog.api.model;

import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One recorded activity on an errand — who did what, when, and with what outcome.
 *
 * @param id             the event id
 * @param errandId       the errand the activity targeted
 * @param municipalityId the municipality id
 * @param namespace      the namespace
 * @param source         where the row came from: {@code HTTP} (an inbound request), {@code EVENT} (a published domain
 *                       event) or {@code LIFECARE} (a read or write Draken's BFF made in Lifecare directly and
 *                       reported)
 * @param action         READ / CREATE / UPDATE / DELETE (derived from the HTTP method)
 * @param target         what was acted on, e.g. {@code errand}, {@code decisions},
 *                       {@code financial-assistance/calculation/draft/incomes}
 * @param description    human readable summary, e.g. {@code "UPDATE financial-assistance/calculation/draft/incomes"}
 * @param httpMethod     the HTTP method of the request
 * @param requestPath    the request path (no query string)
 * @param lifecareId     the Lifecare record a {@code LIFECARE} access was about, or {@code null}
 * @param actor          who acted — the {@code X-Sent-By} value (AD account / partyId / custom), or {@code null} if
 *                       absent
 * @param actorType      the actor type — {@code adAccount}, {@code partyId} or a custom type, or {@code null}
 * @param requestId      the dept44 request id, for correlation with logs
 * @param statusCode     the HTTP response status code
 * @param created        when the activity happened
 */
public record ErrandEventEntry(
	String id,
	String errandId,
	String municipalityId,
	String namespace,
	String source,
	String action,
	String target,
	String description,
	String httpMethod,
	String requestPath,
	String lifecareId,
	String actor,
	String actorType,
	String requestId,
	Integer statusCode,
	@DateTimeFormat(iso = DATE_TIME) OffsetDateTime created) {}
