package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * The effective household member for the normberäkning at commit — the person and the effective number of days in the
 * home (the handläggare value when set, otherwise the process value), ready to post to Lifecare FC.
 */
public record EffectivePerson(
	String partyId,
	Integer numberOfDays) {
}
