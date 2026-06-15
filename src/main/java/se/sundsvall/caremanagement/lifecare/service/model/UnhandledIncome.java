package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * An SSBTEK income that was not transferred to the FC normberäkning, with the SSBTEK descriptors kept so the warning
 * can
 * name it exactly (regelverk: "Följande inkomst som Drakel inte får hantera finns i SSBTEK …").
 *
 * @param forman     the SSBTEK förmån
 * @param delforman  the SSBTEK delförmån, may be {@code null}
 * @param beloppstyp the SSBTEK beloppstyp, may be {@code null}
 * @param reason     why it was not transferred
 */
public record UnhandledIncome(
	String forman,
	String delforman,
	String beloppstyp,
	UnhandledReason reason) {
}
