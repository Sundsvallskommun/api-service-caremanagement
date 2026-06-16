package se.sundsvall.caremanagement.lifecare.service;

import java.util.List;

/**
 * The household roster distilled from a person's most recent Lifecare FC normberäkning (the persons it included),
 * paired with the co-applicant flagged on the most recent beslut. Drives an EB återansökan pre-fill — the sökande, the
 * medsökande and the remaining household members (children). Lifecare supplies personnummer + name only; everything
 * else on the form the citizen fills in.
 *
 * @param applicant   the queried person's personnummer (the sökande)
 * @param coApplicant the medsökande's personnummer from the latest beslut, or {@code null} when applying alone
 * @param members     every person on the latest normberäkning (personnummer + name); includes the applicant and, when
 *                    present, the co-applicant
 */
public record LifecareEbRoster(String applicant, String coApplicant, List<Member> members) {

	/** A single person on the normberäkning — personnummer and name as Lifecare stores them. */
	public record Member(String personalNumber, String name) {
	}
}
