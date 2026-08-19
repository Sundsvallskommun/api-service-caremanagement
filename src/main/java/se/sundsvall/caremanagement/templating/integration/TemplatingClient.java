package se.sundsvall.caremanagement.templating.integration;

import generated.se.sundsvall.templating.RenderRequest;
import generated.se.sundsvall.templating.RenderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.caremanagement.templating.integration.configuration.TemplatingConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.templating.integration.configuration.TemplatingConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.templating.url}", configuration = TemplatingConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface TemplatingClient {

	/**
	 * Renders a stored template as PDF.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  renderRequest  the request carrying the template identifier and parameters
	 * @return                the rendered PDF as a BASE64-encoded string wrapped in a {@link RenderResponse}
	 */
	@PostMapping(path = "/{municipalityId}/render/pdf", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	RenderResponse render(
		@PathVariable final String municipalityId,
		@RequestBody final RenderRequest renderRequest);
}
