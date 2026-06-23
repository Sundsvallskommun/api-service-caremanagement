package se.sundsvall.caremanagement.rpa.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import se.sundsvall.caremanagement.rpa.integration.configuration.RpaConfiguration;
import se.sundsvall.caremanagement.rpa.integration.model.AddQueueItemParameters;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.rpa.integration.configuration.RpaConfiguration.CLIENT_ID;

/**
 * Feign contract for the UiPath Orchestrator OData API — the single {@code AddQueueItem} operation that enqueues an EB
 * RPA task. The {@code X-UIPATH-OrganizationUnitId} header selects the Orchestrator folder (per municipality). OAuth2
 * is applied globally by {@link RpaConfiguration}.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.rpa.url:http://localhost}", configuration = RpaConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface RpaClient {

	@PostMapping(path = "/odata/Queues/UiPathODataSvc.AddQueueItem", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	void addQueueItem(@RequestHeader("X-UIPATH-OrganizationUnitId") String folderId, @RequestBody AddQueueItemParameters queueItem);
}
