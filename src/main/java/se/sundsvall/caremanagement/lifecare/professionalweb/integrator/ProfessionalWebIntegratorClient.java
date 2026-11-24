package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * The ProfessionalWeb pass-through of api-service-lifecare-integrator. Same service, gateway and OAuth2 registration
 * as the FamilyCare reads, a client of its own so the two do not share a circuit breaker.
 */
@FeignClient(name = ProfessionalWebIntegratorClient.CLIENT_ID, url = "${integration.lifecare-integrator.url}", configuration = ProfessionalWebIntegratorConfiguration.class)
@CircuitBreaker(name = ProfessionalWebIntegratorClient.CLIENT_ID)
public interface ProfessionalWebIntegratorClient {

	String CLIENT_ID = "lifecare-integrator-professionalweb";

	@PostMapping(path = "/{municipalityId}/professional-web/exchange", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ProfessionalWebExchangeResponse exchange(@PathVariable final String municipalityId, @RequestBody final ProfessionalWebExchangeRequest request);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/reminders", produces = APPLICATION_JSON_VALUE)
	JsonNode listReminders(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/reminders/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readReminderProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/reminders/{reminderId}", produces = APPLICATION_JSON_VALUE)
	JsonNode readReminder(@PathVariable final String municipalityId, @PathVariable final int reminderId);

	@PostMapping(path = "/{municipalityId}/professional-web/reminders", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createReminder(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PutMapping(path = "/{municipalityId}/professional-web/reminders/{reminderId}", consumes = APPLICATION_JSON_VALUE)
	void updateReminder(@PathVariable final String municipalityId, @PathVariable final int reminderId, @RequestBody final JsonNode body);

	@DeleteMapping(path = "/{municipalityId}/professional-web/reminders/{reminderId}")
	void removeReminder(@PathVariable final String municipalityId, @PathVariable final int reminderId);
}
