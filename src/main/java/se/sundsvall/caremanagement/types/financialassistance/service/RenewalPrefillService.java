package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefilledChild;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.dept44.problem.ThrowableProblem;

/**
 * Builds a financial assistance renewal pre-fill from Lifecare — only the household children. Takes the applicant's
 * partyId,
 * resolves it to a personnummer via {@link CitizenService} (the API never accepts personnummer directly), reads the
 * applicant's most recent calculation roster (and the co-applicant from the most recent decision) via
 * {@link LifecareCaseService}, then keeps the members that are neither the applicant nor the co-applicant and maps
 * them
 * to {@link RenewalPrefill}. The applicant is the logged-in citizen and the co-applicant comes from the portal, so
 * neither
 * is pre-filled; the co-applicant is read only to exclude that adult from the children. The roster already identifies
 * everyone by partyId, so nothing here handles a personnummer beyond the one it hands Lifecare. Lifecare supplies the
 * identity and the name, so everything else (residence, school) is left for the citizen. Best-effort — an unresolved
 * partyId or a citizen/Lifecare failure yields an empty pre-fill with {@code lifecareChecked=false} rather than an
 * error.
 */
@Service
@Transactional(readOnly = true)
public class RenewalPrefillService {

	private final CitizenService citizenService;
	private final LifecareCaseService lifecareCaseService;

	RenewalPrefillService(final CitizenService citizenService, final LifecareCaseService lifecareCaseService) {
		this.citizenService = citizenService;
		this.lifecareCaseService = lifecareCaseService;
	}

	public RenewalPrefill prefill(final String municipalityId, final String partyId) {
		try {
			return citizenService.getPersonalNumber(municipalityId, partyId)
				.map(personalNumber -> toPrefill(lifecareCaseService.latestRoster(municipalityId, personalNumber, LocalDate.now(ZoneId.systemDefault()))))
				.orElseGet(RenewalPrefillService::empty);
		} catch (final ThrowableProblem e) {
			return empty();
		}
	}

	/**
	 * The household minus the two adults. {@link LifecareCaseService} has already put every member, the applicant and
	 * the co-applicant in the same partyId space, so the two exclusions are plain comparisons; a child the citizen
	 * service could not resolve (204) carries a {@code null} partyId, and its name is still useful for the citizen to
	 * recognise.
	 */
	private static RenewalPrefill toPrefill(final LifecareRoster roster) {
		final var children = roster.members().stream()
			.filter(member -> !isSamePerson(member.partyId(), roster.applicant()))
			.filter(member -> !isSamePerson(member.partyId(), roster.coApplicant()))
			.map(member -> PrefilledChild.create()
				.withPartyId(member.partyId())
				.withName(member.name()))
			.toList();

		return RenewalPrefill.create()
			.withLifecareChecked(true)
			.withChildren(children);
	}

	/**
	 * Two identities are the same person only when both are known. Without the null check an unidentifiable child
	 * would match an unidentifiable applicant and be dropped from the pre-fill — the citizen would silently lose a
	 * child from the form rather than see one with a name and no partyId.
	 */
	private static boolean isSamePerson(final String one, final String other) {
		return (one != null) && Objects.equals(one, other);
	}

	private static RenewalPrefill empty() {
		return RenewalPrefill.create().withLifecareChecked(false).withChildren(List.of());
	}
}
