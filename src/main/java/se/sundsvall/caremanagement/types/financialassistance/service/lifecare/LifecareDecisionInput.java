package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The beslut to write to Lifecare, as the caseworker decided it on the Beslut tab.
 *
 * @param decisionCode    Lifecare's beslutstyp code
 * @param date            the beslutsdatum, null for Lifecare's proposal
 * @param periodFrom      start of the period, null when none
 * @param periodTo        end of the period, null when none
 * @param amount          the amount, null for 0
 * @param reasonCode      the orsak's code in Lifecare's catalogue, null when the beslut has none
 * @param message         the beslutsmeddelande as HTML
 * @param writeProtect    save the beslut with its meddelande locked
 * @param decisionMakerId the beslutsfattare's signature
 */
record LifecareDecisionInput(Integer decisionCode, LocalDate date, LocalDate periodFrom, LocalDate periodTo, BigDecimal amount, Integer reasonCode,
	String message, boolean writeProtect, String decisionMakerId) {
}
