package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * Why an SSBTEK income could not be transferred to the FC normberäkning — surfaced to the handläggare as a warning
 * (ssbtek-regelverk.txt: "allt som inte finns på listan föranleder en varning").
 */
public enum UnhandledReason {

	/** The förmån is not on the Drakel whitelist (regelverk table) — must be reviewed manually. */
	NOT_ON_WHITELIST,

	/** The förmån maps to an FC normberäkning type that the FC calculation proposal does not offer for this person. */
	FC_TYPE_NOT_IN_PROPOSAL
}
