package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.User;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * Resolves the caseworker to put on a financial assistance intake: reads the applicant's <em>most recent</em> Lifecare
 * FC Service
 * (support effort) over a lookback window, takes its {@code Caseworker} display name, and matches that name against the
 * FC
 * user directory ({@code Users/GetUsers}) to recover the user's FC {@code Id} (the actualisation {@code CaseworkerId})
 * and {@code NetworkUserId} (the careM errand {@code assignedUserId}). The person-based Service read only carries the
 * caseworker as a display name, so the directory match is the only way to recover the ids the writes need.
 *
 * <p>
 * Resolution is intentionally lenient — any step that yields nothing (no Service, no caseworker name, no matching user)
 * returns {@link Optional#empty()} so the caller can create the intake without a caseworker rather than fail. Names are
 * matched case-insensitively and trimmed; disabled users are skipped. No personId or name is logged here.
 */
@Service
public class CaseworkerResolver {

	private final LifecareFcIntegration lifecareFcIntegration;
	private final int lookbackMonths;
	private final int usersLimit;

	CaseworkerResolver(final LifecareFcIntegration lifecareFcIntegration,
		@Value("${integration.lifecare-fc.caseworker-lookback-months:36}") final int lookbackMonths,
		@Value("${integration.lifecare-fc.users-limit:1000}") final int usersLimit) {
		this.lifecareFcIntegration = lifecareFcIntegration;
		this.lookbackMonths = lookbackMonths;
		this.usersLimit = usersLimit;
	}

	/**
	 * Resolve the caseworker for the applicant as of the intake date.
	 *
	 * @param  personId      the applicant's personal identity number
	 * @param  referenceDate the intake date (bounds the Service lookback window)
	 * @return               the resolved caseworker, or empty when none can be determined
	 */
	public Optional<ResolvedCaseworker> resolve(final String personId, final LocalDate referenceDate) {
		return mostRecentServiceCaseworker(personId, referenceDate)
			.flatMap(this::findUserByFullName)
			.map(CaseworkerResolver::toResolvedCaseworker);
	}

	/** The caseworker display name on the person's most recent Service (by start date) in the lookback window. */
	private Optional<String> mostRecentServiceCaseworker(final String personId, final LocalDate referenceDate) {
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		return ofNullable(lifecareFcIntegration.getServices(personId, start, end))
			.map(ApiPaginationCompositePersonBasedServiceDTO::getResult)
			.orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(service -> StringUtils.hasText(service.getCaseworker()))
			.max(Comparator.comparing(CaseworkerResolver::startDateOf))
			.map(PersonBasedServiceDTO::getCaseworker)
			.map(String::trim);
	}

	/** The first enabled FC user whose full name matches the given caseworker name (case-insensitive, trimmed). */
	private Optional<User> findUserByFullName(final String caseworkerName) {
		return ofNullable(lifecareFcIntegration.getUsers(usersLimit, null, null, null))
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
			.map(date -> {
				try {
					// FC may return a datetime (e.g. 2026-05-01T00:00:00); take the leading yyyy-MM-dd so a time
					// component doesn't push every service to LocalDate.MIN and scramble the caseworker ordering.
					final String datePart;
					if (date.length() >= 10) {
						datePart = date.substring(0, 10);
					} else {
						datePart = date;
					}
					return LocalDate.parse(datePart, ISO_LOCAL_DATE);
				} catch (final RuntimeException e) {
					return LocalDate.MIN;
				}
			})
			.orElse(LocalDate.MIN);
	}
}
