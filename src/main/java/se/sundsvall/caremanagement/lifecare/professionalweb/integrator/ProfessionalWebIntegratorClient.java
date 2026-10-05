package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * The ProfessionalWeb pass-through of api-service-lifecare-integrator. Same service, gateway and OAuth2 registration
 * as the FamilyCare reads, a client of its own so the two do not share a circuit breaker.
 */
@FeignClient(name = ProfessionalWebIntegratorClient.CLIENT_ID, url = "${integration.lifecare-integrator.url}", configuration = LifecareIntegratorConfiguration.class)
@CircuitBreaker(name = ProfessionalWebIntegratorClient.CLIENT_ID)
public interface ProfessionalWebIntegratorClient {

	String CLIENT_ID = "lifecare-integrator-professionalweb";

	@PostMapping(path = "/{municipalityId}/professional-web/exchange", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ProfessionalWebExchangeResponse exchange(@PathVariable final String municipalityId, @RequestBody final ProfessionalWebExchangeRequest request);
}
