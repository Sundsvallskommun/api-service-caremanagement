package se.sundsvall.caremanagement.rpa.service;

import java.util.Set;

/**
 * The financial assistance RPA actions a robot can be asked to perform. They all ride on the one financial assistance
 * queue (see
 * {@code integration.rpa.queue}); the action string is the {@code action} key in the queue item's
 * {@code SpecificContent}, telling the robot which Lifecare GUI flow to run.
 *
 * <p>
 * One <em>fetch</em> action (the robot reads Lifecare and writes the supplements back onto the errand via the existing
 * CareManagement endpoints) and a set of <em>write</em> actions (the robot types CareManagement-held data into
 * Lifecare).
 */
public final class RpaAction {

	private RpaAction() {}

	/** Fetch watches/reminders / journal / documents from Lifecare and feed them back into the errand. */
	public static final String FETCH_SUPPLEMENTS = "FETCH_SUPPLEMENTS";

	/** Write the committed calculation into Lifecare. */
	public static final String WRITE_NORMBERAKNING = "WRITE_NORMBERAKNING";

	/** Write the decision into Lifecare. */
	public static final String WRITE_DECISION = "WRITE_DECISION";

	/** Write a journal entry into Lifecare. */
	public static final String WRITE_JOURNAL = "WRITE_JOURNAL";

	/** Write a dokument into Lifecare. */
	public static final String WRITE_DOCUMENT = "WRITE_DOCUMENT";

	/** Mirror a bevakning onto the person in Lifecare. */
	public static final String WRITE_MONITORING = "WRITE_MONITORING";

	/** Register the utbetalning in Lifecare (the handläggare's manual step, when automated). */
	public static final String REGISTER_PAYMENT = "REGISTER_PAYMENT";

	/** All recognised actions — the allow-list for the {@code action} request field. */
	public static final Set<String> ACTIONS = Set.of(
		FETCH_SUPPLEMENTS,
		WRITE_NORMBERAKNING,
		WRITE_DECISION,
		WRITE_JOURNAL,
		WRITE_DOCUMENT,
		WRITE_MONITORING,
		REGISTER_PAYMENT);
}
