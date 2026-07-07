package se.sundsvall.caremanagement.rpa.service;

/**
 * The financial assistance RPA actions a robot can be asked to perform. They all ride on the one financial assistance
 * queue (see {@code integration.rpa.queue}); the constant name is the {@code action} key in the queue item's
 * {@code SpecificContent}, telling the robot which Lifecare GUI flow to run. The API validates the inbound string
 * against this enum via {@code @MemberOf(RpaAction.class)} on {@code RpaTaskRequest.action}, and {@code RpaService}
 * serialises the enum to its constant name at the wire boundary — so the enum is the single source of truth for the
 * action catalogue.
 *
 * <p>
 * One <em>fetch</em> action (the robot reads Lifecare and writes the supplements back onto the errand via the existing
 * CareManagement endpoints) and a set of <em>write</em> actions (the robot types CareManagement-held data into
 * Lifecare).
 */
public enum RpaAction {

	/** Fetch watches/reminders / journal / documents from Lifecare and feed them back into the errand. */
	FETCH_SUPPLEMENTS,

	/** Write the committed calculation into Lifecare. */
	WRITE_NORMBERAKNING,

	/** Write the decision into Lifecare. */
	WRITE_DECISION,

	/** Write a journal entry into Lifecare. */
	WRITE_JOURNAL,

	/** Write a dokument into Lifecare. */
	WRITE_DOCUMENT,

	/** Mirror a bevakning onto the person in Lifecare. */
	WRITE_MONITORING,

	/** Register the utbetalning in Lifecare (the handläggare's manual step, when automated). */
	REGISTER_PAYMENT
}
