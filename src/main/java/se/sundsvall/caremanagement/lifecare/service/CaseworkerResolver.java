package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.User;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * Resolves the caseworker to put on a financial assistance intake: reads the applicant's <em>most recent</em> Lifecare
 * FamilyCare Service (support effort) over a lookback window, takes its {@code Caseworker} display name, and matches
 * that name against the FamilyCare user directory ({@code Users/GetUsers}) to recover the user's FamilyCare {@code Id}
 * (the actualisation {@code CaseworkerId}) and {@code NetworkUserId} (the careM errand {@code assignedUserId}). The
 * person-based Service read only carries the caseworker as a display name, so the directory match is the only way to
 * recover the ids the writes need.
 *
 * <p>
 * Resolution is intentionally lenient — any step that yields nothing (no Service, no caseworker name, no matching
 * user) returns {@link Optional#empty()} so the caller can create the intake without a caseworker rather than fail.
 * Names are matched case-insensitively and trimmed; disabled users are skipped. No partyId or name is logged here.
 */
@Service
public class CaseworkerResolver {

	private final LifecareFamilyCare lifecareFamilyCareIntegration;
	private final int lookbackMonths;
	private final int usersLimit;

	CaseworkerResolver(final LifecareFamilyCare lifecareFamilyCareIntegration,
		@Value("${integration.lifecare-familycare.caseworker-lookback-months:36}") final int lookbackMonths,
		@Value("${integration.lifecare-familycare.users-limit:1000}") final int usersLimit) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.lookbackMonths = lookbackMonths;
		this.usersLimit = usersLimit;
	}

	/**
	 * Resolve the caseworker for the applicant as of the intake date.
	 *
	 * @param  partyId       the applicant's partyId
	 * @param  referenceDate the intake date (bounds the Service lookback window)
	 * @return               the resolved caseworker, or empty when none can be determined
	 */
	public Optional<ResolvedCaseworker> resolve(final String municipalityId, final String partyId, final LocalDate referenceDate) {
		return mostRecentServiceCaseworker(municipalityId, partyId, referenceDate)
			.flatMap(name -> findUserByFullName(municipalityId, name))
			.map(CaseworkerResolver::toResolvedCaseworker);
	}

	/**
	 * The caseworker display name on the person's most recent <strong>open</strong> Service (by start date) in the
	 * lookback window.
	 * <p>
	 * Open, not merely recent: verksamheten decided on 2026-09-22 that an application with no open insats goes to the
	 * default handläggare even when the person is known to us. The question they were asked was exactly this — keep
	 * today's continuity with the last caseworker a returnee had, or follow the regelverk's wording — and they chose
	 * the wording. So a closed insats no longer places an errand; a returnee whose case ended last year now reaches
	 * the default assignee instead of their old caseworker.
	 */
	private Optional<String> mostRecentServiceCaseworker(final String municipalityId, final String partyId, final LocalDate referenceDate) {
		final var start = referenceDate.minusMonths(lookbackMonths);

		return ofNullable(lifecareFamilyCareIntegration.getServices(municipalityId, partyId, start, referenceDate))
			.map(ApiPaginationCompositePersonBasedServiceDTO::getResult)
			.orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(service -> isOpenOn(service, referenceDate))
			.filter(service -> StringUtils.hasText(service.getCaseworker()))
			.max(Comparator.comparing(CaseworkerResolver::startDateOf))
			.map(PersonBasedServiceDTO::getCaseworker)
			.map(String::trim);
	}

	/**
	 * Whether the Service is still running on the intake date: no end date at all, or one that has not passed.
	 * <p>
	 * An unparseable end date counts as open. The alternative — treating what we cannot read as closed — would move
	 * errands off a caseworker who is in fact still handling them, on the strength of a string we did not understand.
	 * A wrongly kept caseworker is visible to the handläggare; a wrongly reassigned one looks like a routing rule
	 * working as intended.
	 */
	private static boolean isOpenOn(final PersonBasedServiceDTO service, final LocalDate referenceDate) {
		return ofNullable(service.getEndDate())
			.filter(StringUtils::hasText)
			.flatMap(CaseworkerResolver::parseDate)
			.map(endDate -> !endDate.isBefore(referenceDate))
			.orElse(true);
	}

	/** The first enabled FamilyCare user whose full name matches the given caseworker name (case-insensitive, trimmed). */
	private Optional<User> findUserByFullName(final String municipalityId, final String caseworkerName) {
		return ofNullable(lifecareFamilyCareIntegration.getUsers(municipalityId, usersLimit, null, null, null))
			.orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(user -> !Boolean.TRUE.equals(user.getDisabled()))
			.filter(user -> caseworkerName.equalsIgnoreCase(ofNullable(user.getFullName()).map(String::trim).orElse(null)))
			.findFirst();
	}

	private static ResolvedCaseworker toResolvedCaseworker(final User user) {
		final var assignedUserId = ofNullable(user.getNetworkUserId()).filter(StringUtils::hasText).orElse(user.getId());
		return new ResolvedCaseworker(user.getId(), assignedUserId, user.getFullName());
	}

	/** Parse a Service's plain-string start date for ordering; a missing/garbled date sorts oldest (LocalDate.MIN). */
	private static LocalDate startDateOf(final PersonBasedServiceDTO service) {
		return ofNullable(service.getStartDate())
			.filter(StringUtils::hasText)
			.flatMap(CaseworkerResolver::parseDate)
			.orElse(LocalDate.MIN);
	}

	/**
	 * Parse one of FamilyCare's plain-string dates, or nothing when it cannot be read.
	 * <p>
	 * FamilyCare may return a datetime (e.g. {@code 2026-05-01T00:00:00}); the leading {@code yyyy-MM-dd} is taken so
	 * a time component does not make every date unreadable. Callers decide what an unreadable date means — oldest for
	 * ordering, still open for the end date — because the safe answer differs.
	 */
	private static Optional<LocalDate> parseDate(final String value) {
		try {
			final String datePart;
			if (value.length() >= 10) {
				datePart = value.substring(0, 10);
			} else {
				datePart = value;
			}
			return Optional.of(LocalDate.parse(datePart, ISO_LOCAL_DATE));
		} catch (final RuntimeException e) {
			return Optional.empty();
		}
	}
}
