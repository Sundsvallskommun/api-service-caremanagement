package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefilledChild;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.dept44.problem.ThrowableProblem;

/**
 * Builds an EB återansökan pre-fill from Lifecare — only the household children. Takes the applicant's partyId,
 * resolves it to a personnummer via {@link CitizenService} (the API never accepts personnummer directly), reads the
 * applicant's most recent normberäkning roster (and the co-applicant from the most recent beslut) via
 * {@link LifecareEbCaseService}, then keeps the members that are neither the sökande nor the medsökande and maps them
 * to {@link RenewalPrefill}. The sökande is the logged-in citizen and the medsökande comes from the portal, so neither
 * is pre-filled; the co-applicant is read only to exclude that adult from the children. Lifecare supplies personnummer
 * + name, so everything else (residence, school) is left for the citizen. Best-effort — an unresolved partyId or a
 * citizen/Lifecare failure yields an empty pre-fill with {@code lifecareChecked=false} rather than an error.
 */
@Service
@Transactional(readOnly = true)
public class RenewalPrefillService {

	private final CitizenService citizenService;
	private final LifecareEbCaseService lifecareEbCaseService;

	RenewalPrefillService(final CitizenService citizenService, final LifecareEbCaseService lifecareEbCaseService) {
		this.citizenService = citizenService;
		this.lifecareEbCaseService = lifecareEbCaseService;
	}

	public RenewalPrefill prefill(final String municipalityId, final String partyId) {
		try {
			return citizenService.getPersonalNumber(municipalityId, partyId)
				.map(personalNumber -> toPrefill(lifecareEbCaseService.latestRoster(personalNumber, LocalDate.now())))
				.orElseGet(RenewalPrefillService::empty);
		} catch (final ThrowableProblem e) {
			return empty();
		}
	}

	private static RenewalPrefill toPrefill(final LifecareEbRoster roster) {
		final var children = roster.members().stream()
			.filter(member -> !Objects.equals(member.personalNumber(), roster.applicant()))
			.filter(member -> !Objects.equals(member.personalNumber(), roster.coApplicant()))
			.map(member -> PrefilledChild.create().withPersonalNumber(member.personalNumber()).withName(member.name()))
			.toList();

		return RenewalPrefill.create()
			.withLifecareChecked(true)
			.withChildren(children);
	}

	private static RenewalPrefill empty() {
		return RenewalPrefill.create().withLifecareChecked(false).withChildren(List.of());
	}
}
