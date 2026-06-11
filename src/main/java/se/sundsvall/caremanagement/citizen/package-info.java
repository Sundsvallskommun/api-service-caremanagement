/**
 * Citizen integration module — outbound client for citizen/folkbokföring lookups (api-service-citizen): resolve a
 * personId from a personnummer and fetch the citizen incl. addresses, civil status and protected-identity flags. A
 * first-class, type-agnostic integration for case preparation. See {@code CitizenClient} for the v2/v3 contract note.
 */
@ApplicationModule(displayName = "Citizen")
package se.sundsvall.caremanagement.citizen;

import org.springframework.modulith.ApplicationModule;
