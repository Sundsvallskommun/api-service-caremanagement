/**
 * Citizen service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can resolve a personnummer from a partyId through
 * {@link se.sundsvall.caremanagement.citizen.service.CitizenService} without reaching into the integration layer or
 * handling personnummer-bearing identifiers at the API edge.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.citizen.service;

import org.springframework.modulith.NamedInterface;
