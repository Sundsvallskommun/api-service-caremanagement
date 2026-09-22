package se.sundsvall.caremanagement.lifecare.service;

import java.util.List;

/**
 * The household roster distilled from a person's most recent Lifecare FamilyCare calculation (the persons it included),
 * paired with the co-applicant flagged on the most recent decision. Drives a financial assistance renewal pre-fill —
 * the applicant, the co-applicant and the remaining household members (children). Lifecare supplies an identity and a
 * name only; everything else on the form the citizen fills in.
 *
 * <p>
 * Everyone here is identified by {@code partyId}, never by personal identity number. Lifecare itself answers with one
 * or the other depending on which route served the call, so {@link LifecareCaseService} resolves both onto party ids
 * before building this record — which keeps the personnummer out of the model and makes the two routes comparable.
 * A person the citizen service cannot resolve carries a {@code null} party id; the name is still worth showing.
 *
 * @param applicant   the queried person's partyId (the applicant)
 * @param coApplicant the co-applicant from the latest decision, or {@code null} when applying alone. A partyId when
 *                    the decision flagged a person; FamilyCare's own free-text co-applicant field otherwise, which is
 *                    not an identity at all — compare it against a party id only as a best effort.
 * @param members     every person on the latest calculation (partyId + name), applicant and co-applicant included
 */
public record LifecareRoster(String applicant, String coApplicant, List<Member> members) {

	/** A single person on the calculation — partyId and the name as Lifecare stores it. */
	public record Member(String partyId, String name) {
	}
}
