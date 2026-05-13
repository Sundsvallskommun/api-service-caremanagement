package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;

/**
 * Sealed root of all envelope-level errand events.
 *
 * Pattern-match in listeners — compiler enforces exhaustive handling
 * when {@code @ApplicationModuleListener} receives the base type:
 *
 * <pre>{@code
 * &#64;ApplicationModuleListener
 * void onErrandEvent(final ErrandEvent event) {
 *     switch (event) {
 *         case ErrandCreated created       -> ...;
 *         case ErrandStatusChanged changed -> ...;
 *         case ErrandAssigned assigned     -> ...;
 *         case ErrandDeleted deleted       -> ...;
 *     }
 * }
 * }</pre>
 *
 * Type-specific events live in the type module and are NOT permitted here.
 */
public sealed interface ErrandEvent permits ErrandCreated, ErrandStatusChanged, ErrandAssigned, ErrandDeleted {

	String errandId();

	String typeSlug();

	String municipalityId();

	String namespace();

	OffsetDateTime timestamp();
}
