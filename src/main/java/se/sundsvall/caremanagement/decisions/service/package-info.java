/**
 * Decisions service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can record decisions on an errand — for example a
 * {@code RECOMMENDATION} produced by the automated calculation pipeline — through
 * {@link se.sundsvall.caremanagement.decisions.service.DecisionService}, without reaching into the decisions
 * persistence layer.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.decisions.service;

import org.springframework.modulith.NamedInterface;
