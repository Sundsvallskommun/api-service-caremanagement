/**
 * Lifecare ProfessionalWeb integration.
 *
 * <p>
 * The internal api2 surface Lifecare's own web client uses, reached with a signed-in session of an integration account
 * (SAML via MobilityGuard) rather than an API key. It covers what the public FamilyCare API cannot: editing a
 * normberäkning, decisions, payments, payees, reminders, journal notes, documents and job stimulus periods.
 * </p>
 *
 * <p>
 * Exposed to the type modules through
 * {@link se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient}.
 * The API is undocumented and unversioned on Lifecare's side, so everything that depends on its shape stays behind
 * that client and the services built on it.
 * </p>
 */
@NamedInterface("professionalweb")
package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.springframework.modulith.NamedInterface;
