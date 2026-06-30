package se.sundsvall.caremanagement.citizen.integration;

import generated.se.sundsvall.citizen.CitizenExtended;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.caremanagement.citizen.integration.configuration.CitizenConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.citizen.integration.configuration.CitizenConfiguration.CLIENT_ID;

/**
 * Client for citizen/folkbokföring lookups (personId resolution + citizen incl. addresses, civil status, protected
 * identity) against the Sundsvallskommun <b>api-service-citizen v3</b> API. A first-class, type-agnostic integration
 * for
 * case preparation.
 * <p>
 * The v3 contract takes {@code municipalityId} as a path segment. {@code integration.citizen.url} must point at the v3
 * base path (ending in {@code /api/v3/citizen}); the mappings below are relative to it.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.citizen.url}", configuration = CitizenConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CitizenClient {

	/**
	 * Resolve a citizen's personId (GUID) from their personnummer.
	 * <p>
	 * api-service-citizen v3: {@code GET /{municipalityId}/{personNumber}/guid}
	 *
	 * @param  municipalityId the id of the municipality (path variable)
	 * @param  personNumber   the applicant's personnummer (path variable)
	 * @return                the citizen's personId (GUID), or {@code null} on 204 No Content
	 */
	@GetMapping(path = "/{municipalityId}/{personNumber}/guid", produces = APPLICATION_JSON_VALUE)
	String getGuid(
		@PathVariable final String municipalityId,
		@PathVariable final String personNumber);

	/**
	 * Resolve a citizen's personnummer from their personId (GUID) — the reverse of {@link #getGuid}.
	 * <p>
	 * api-service-citizen v3: {@code GET /{municipalityId}/{personId}/personnumber}
	 *
	 * @param  municipalityId the id of the municipality (path variable)
	 * @param  personId       the citizen's personId (GUID / partyId)
	 * @return                the citizen's personnummer, or {@code null} on 204 No Content
	 */
	@GetMapping(path = "/{municipalityId}/{personId}/personnumber", produces = APPLICATION_JSON_VALUE)
	String getPersonNumber(
		@PathVariable final String municipalityId,
		@PathVariable final String personId);

	/**
	 * Fetch a citizen (incl. addresses) by personId.
	 * <p>
	 * api-service-citizen v3: {@code GET /{municipalityId}/{personId}?ShowClassified=...}
	 *
	 * @param  municipalityId the id of the municipality (path variable)
	 * @param  personId       the citizen's personId (GUID)
	 * @param  showClassified whether to include classified/protected data (requires authorization)
	 * @return                the citizen, or {@code null} on 204 No Content
	 */
	@GetMapping(path = "/{municipalityId}/{personId}", produces = APPLICATION_JSON_VALUE)
	CitizenExtended getCitizen(
		@PathVariable final String municipalityId,
		@PathVariable final String personId,
		@RequestParam("ShowClassified") final boolean showClassified);
}
