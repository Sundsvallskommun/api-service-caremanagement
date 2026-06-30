package se.sundsvall.caremanagement.lifecare.service;

import java.util.List;

/**
 * The household roster distilled from a person's most recent Lifecare FC calculation (the persons it included),
 * paired with the co-applicant flagged on the most recent decision. Drives an EB renewal pre-fill — the applicant, the
 * co-applicant and the remaining household members (children). Lifecare supplies personnummer + name only; everything
 * else on the form the citizen fills in.
 *
 * @param applicant   the queried person's personnummer (the applicant)
 * @param coApplicant the co-applicant's personnummer from the latest decision, or {@code null} when applying alone
 * @param members     every person on the latest calculation (personnummer + name); includes the applicant and, when
 *                    present, the co-applicant
 */
public record LifecareEbRoster(String applicant, String coApplicant, List<Member> members) {

	/** A single person on the calculation — personnummer and name as Lifecare stores them. */
	public record Member(String personalNumber, String name) {
	}
}
