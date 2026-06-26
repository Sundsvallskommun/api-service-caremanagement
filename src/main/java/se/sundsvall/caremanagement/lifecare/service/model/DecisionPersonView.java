package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * A single person on a Lifecare beslut (decision), as read for display, with the co-applicant flag.
 */
public record DecisionPersonView(
	String personId,
	String name,
	Boolean coApplicant) {
}
