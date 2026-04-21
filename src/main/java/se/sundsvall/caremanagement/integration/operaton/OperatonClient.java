package se.sundsvall.caremanagement.integration.operaton;

import generated.se.sundsvall.operaton.ProcessInstanceResponse;
import generated.se.sundsvall.operaton.ProcessInstancesResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.caremanagement.integration.operaton.configuration.OperatonConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.integration.operaton.configuration.OperatonConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.operaton.url}", configuration = OperatonConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface OperatonClient {

	/**
	 * List active process instances.
	 *
	 * @param  municipalityId the id of the municipality
	 * @return                response containing the active process instances
	 */
	@GetMapping(path = "/{municipalityId}/process-instances", produces = APPLICATION_JSON_VALUE)
	ProcessInstancesResponse getProcessInstances(
		@PathVariable("municipalityId") final String municipalityId);

	/**
	 * Start a new process instance by process definition key.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  request        containing process definition key and optional variables
	 * @return                response describing the started process instance
	 */
	@PostMapping(path = "/{municipalityId}/process-instances", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	ProcessInstanceResponse startProcessInstance(
		@PathVariable("municipalityId") final String municipalityId,
		@RequestBody final StartProcessInstanceRequest request);

	/**
	 * Get status and details for a specific process instance.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  id             the process instance id
	 * @return                response describing the process instance
	 */
	@GetMapping(path = "/{municipalityId}/process-instances/{id}", produces = APPLICATION_JSON_VALUE)
	ProcessInstanceResponse getProcessInstance(
		@PathVariable("municipalityId") final String municipalityId,
		@PathVariable("id") final String id);
}
