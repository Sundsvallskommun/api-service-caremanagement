package se.sundsvall.caremanagement.eventlog.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One actor's activity across every errand in the tenant — the logguppföljning answer.
 *
 * <p>
 * Carries the total alongside the page on purpose. The listing is capped, and an audit answer that quietly stops
 * short reads exactly like a complete one: a follow-up that shows 1000 rows and does not say there are 4000 is worse
 * than showing nothing, because the reader draws a conclusion from it.
 * </p>
 */
@Schema(description = "One actor's activity across every errand in the namespace, newest first. The listing is capped; compare 'events' against 'total' to see whether it was truncated, and narrow with from/to to see the rest.")
public record ActorEventLog(

	@Schema(description = "The matching events, newest first, up to the listing cap") List<ErrandEventEntry> events,

	@Schema(description = "How many events match the filters in total, ignoring the cap", examples = "4213") long total) {
}
