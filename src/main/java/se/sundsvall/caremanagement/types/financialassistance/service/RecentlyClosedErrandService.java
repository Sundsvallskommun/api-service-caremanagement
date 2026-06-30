package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUGS;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_CLOSED;

/**
 * Detects whether any of a set of parties has a <em>recently closed</em> EB errand — an errand of an EB type whose
 * current status is {@code CLOSED} and which was last touched within the configured window. The {@code touched}
 * timestamp is used as the close time, the same heuristic the EB message-archive job uses for "closed since" (a closed
 * errand is normally not touched again after closing). Shared by the common-entry-point eligibility routing (which
 * recommends a renewal and surfaces the closed errand for reopening) and by the errand-created freeze (which halts
 * auto-actualisation and forces manual review).
 *
 * <p>
 * The reopen itself is a human action in Lifecare (the caseworker opens the insats); caremanagement only flags the
 * situation. The window is a policy value — confirm the day count with legal — configured via
 * {@code financial-assistance.eligibility.recently-closed-window-days}.
 */
@Service
@Transactional(readOnly = true)
public class RecentlyClosedErrandService {

	/** A recently closed EB errand: its id and when it was closed. */
	public record RecentlyClosed(String errandId, OffsetDateTime closedAt) {
	}

	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final ErrandRepository errandRepository;
	private final int windowDays;

	RecentlyClosedErrandService(final FinancialAssistanceRepository financialAssistanceRepository,
		final ErrandRepository errandRepository,
		@Value("${financial-assistance.eligibility.recently-closed-window-days:30}") final int windowDays) {
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.errandRepository = errandRepository;
		this.windowDays = windowDays;
	}

	/**
	 * The most recently closed EB errand for any of the given parties whose close happened within the window, if any.
	 * Parties are partyIds (applicant + optional co-applicant); blank entries are ignored.
	 */
	public Optional<RecentlyClosed> findRecentlyClosed(final String municipalityId, final String namespace, final Collection<String> partyIds) {
		final var cutoff = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(windowDays);
		final var errandIds = partyIds.stream()
			.filter(StringUtils::hasText)
			.flatMap(partyId -> financialAssistanceRepository.findErrandIdsByPartyId(partyId).stream())
			.collect(toCollection(LinkedHashSet::new));

		return errandIds.stream()
			.map(id -> errandRepository.findByIdAndNamespaceAndMunicipalityId(id, namespace, municipalityId))
			.flatMap(Optional::stream)
			.filter(errand -> SLUGS.contains(errand.getTypeSlug()))
			.filter(errand -> STATUS_CLOSED.equals(errand.getStatus()))
			.flatMap(errand -> recentlyClosed(errand, cutoff).stream())
			.max(comparing(RecentlyClosed::closedAt));
	}

	private static Optional<RecentlyClosed> recentlyClosed(final ErrandEntity errand, final OffsetDateTime cutoff) {
		return closedAt(errand)
			.filter(closedAt -> !closedAt.isBefore(cutoff))
			.map(closedAt -> new RecentlyClosed(errand.getId(), closedAt));
	}

	/** The close time of a CLOSED errand — its last-touched timestamp, falling back to modified, then created. */
	private static Optional<OffsetDateTime> closedAt(final ErrandEntity errand) {
		return ofNullable(errand.getTouched())
			.or(() -> ofNullable(errand.getModified()))
			.or(() -> ofNullable(errand.getCreated()));
	}
}
