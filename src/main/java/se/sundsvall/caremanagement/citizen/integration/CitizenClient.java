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
 * identity). A first-class, type-agnostic integration for case preparation.
 * <p>
 * <b>Contract note:</b> the request paths below follow the self-hosted <b>api-service-citizen v2</b> contract used by
 * the POC — there is no {@code municipalityId} path segment (it is a query parameter on the guid lookup), and the
 * citizen-by-id lookup takes only the personId. The Sundsvallskommun <b>v3</b> API differs; adjust the mappings (and
 * {@code integration.citizen.url}) when pointing at v3 or a v3-shaped WireMock. The generated models are v3-shaped.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.citizen.url}", configuration = CitizenConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CitizenClient {

	/**
	 * Resolve a citizen's personId (GUID) from their personnummer.
	 * <p>
	 * api-service-citizen v2: {@code GET /{personNumber}/guid?municipalityId=...}
	 *
	 * @param  municipalityId the id of the municipality (query parameter)
	 * @param  personNumber   the applicant's personnummer (path variable)
	 * @return                the citizen's personId (GUID), or {@code null} on 204 No Content
	 */
	@GetMapping(path = "/{personNumber}/guid", produces = APPLICATION_JSON_VALUE)
	String getGuid(
		@RequestParam("municipalityId") final String municipalityId,
		@PathVariable final String personNumber);

	/**
	 * Fetch a citizen (incl. addresses) by personId.
	 * <p>
	 * api-service-citizen v2: {@code GET /{personId}}
	 *
	 * @param  personId the citizen's personId (GUID)
	 * @return          the citizen, or {@code null} on 204 No Content
	 */
	@GetMapping(path = "/{personId}", produces = APPLICATION_JSON_VALUE)
	CitizenExtended getCitizen(
		@PathVariable final String personId);
}
