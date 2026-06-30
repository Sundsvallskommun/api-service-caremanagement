/**
 * Citizen integration module — outbound client for citizen/folkbokföring lookups (api-service-citizen): resolve a
 * personId from a personnummer and fetch the citizen incl. addresses, civil status and protected-identity flags. A
 * first-class, type-agnostic integration for case preparation, targeting the Sundsvallskommun api-service-citizen v3
 * contract (municipalityId is a path segment).
 */
@ApplicationModule(displayName = "Citizen")
package se.sundsvall.caremanagement.citizen;

import org.springframework.modulith.ApplicationModule;
