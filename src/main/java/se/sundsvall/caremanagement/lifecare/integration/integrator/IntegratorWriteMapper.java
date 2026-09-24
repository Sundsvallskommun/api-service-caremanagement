package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecareintegrator.CreateActualisationRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static java.util.Optional.ofNullable;

/**
 * Translates the FamilyCare actualisation body careM assembles into the integrator's own request — the one write
 * careM still makes through this route. (careM no longer creates calculations: Draken's BFF owns the normberäkning.)
 *
 * <p>
 * FamilyCare's rendered date strings become {@link LocalDate}; nothing else is reinterpreted — a value careM did not
 * set stays unset. The applicant is the one field that cannot be copied across: FamilyCare's body identifies the
 * person by personal identity number while the integrator wants a {@code partyId}, so the caller resolves it and
 * passes it in.
 */
final class IntegratorWriteMapper {

	private IntegratorWriteMapper() {}

	/**
	 * @param applicantPartyId the applicant, already resolved from the personal identity number in
	 *                         {@code body.personId}.
	 */
	static CreateActualisationRequest toActualisation(final PostAktualiseringsBodyRequest body, final String applicantPartyId) {
		return new CreateActualisationRequest()
			.partyId(applicantPartyId)
			.date(toDate(body.getDate()))
			.typeId(body.getType())
			.fromWhoId(body.getFromWho())
			.reasonId(body.getReason())
			.organisationId(body.getOrganisationId())
			.organisationUnitId(body.getOrganisationUnitId())
			.caseworkerId(body.getCaseworkerId())
			.specifiesId(body.getSpecifies())
			.serviceId(body.getServiceId())
			.investigationId(body.getInvestigationId())
			.workingStatusId(body.getWorkingStatus());
	}

	// ---- Conversions -------------------------------------------------------------------------------------------------

	/**
	 * FamilyCare's own rendering is {@code yyyy-MM-dd'T'HH:mm:ss} (see {@code FamilyCareDates}), so the leading ten
	 * characters are the date. Taking them rather than parsing the whole string keeps a bare {@code yyyy-MM-dd} and any
	 * other suffix working; anything that is not a date in front becomes {@code null}, which the caller's
	 * required-field check then reports by name instead of the integrator rejecting an empty body.
	 */
	private static LocalDate toDate(final String rendered) {
		return ofNullable(rendered)
			.filter(value -> value.length() >= 10)
			.map(value -> {
				try {
					return LocalDate.parse(value.substring(0, 10));
				} catch (final DateTimeParseException e) {
					return null;
				}
			})
			.orElse(null);
	}
}
