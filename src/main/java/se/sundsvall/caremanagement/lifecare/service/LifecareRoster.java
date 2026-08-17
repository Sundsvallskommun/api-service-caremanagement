package se.sundsvall.caremanagement.lifecare.service;

import java.util.List;

/**
 * The household roster distilled from a person's most recent Lifecare FC calculation (the persons it included),
 * paired with the co-applicant flagged on the most recent decision. Drives a financial assistance renewal pre-fill —
 * the applicant, the
 * co-applicant and the remaining household members (children). Lifecare supplies personal identity number + name only;
 * everything
 * else on the form the citizen fills in.
 *
 * @param applicant   the queried person's personal identity number (the applicant)
 * @param coApplicant the co-applicant's personal identity number from the latest decision, or {@code null} when
 *                    applying alone
 * @param members     every person on the latest calculation (personal identity number + name); includes the applicant
 *                    and, when
 *                    present, the co-applicant
 */
public record LifecareRoster(String applicant, String coApplicant, List<Member> members) {

	/** A single person on the calculation — personal identity number and name as Lifecare stores them. */
	public record Member(String personalNumber, String name) {
	}
}
