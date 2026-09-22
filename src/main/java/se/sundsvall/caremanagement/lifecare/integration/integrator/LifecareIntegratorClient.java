package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecareintegrator.CalculationProposal;
import generated.se.sundsvall.lifecareintegrator.DecisionsResponse;
import generated.se.sundsvall.lifecareintegrator.PagedActualisationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedCalculationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedDocumentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedPaymentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedServiceResponse;
import generated.se.sundsvall.lifecareintegrator.Person;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration.CLIENT_ID;

/**
 * Reads Lifecare FamilyCare through {@code api-service-lifecare-integrator} rather than from FamilyCare directly.
 *
 * <p>
 * Two differences from {@link se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareClient} are worth
 * knowing when reading this. Everything is keyed on {@code partyId} rather than a personal identity number — the
 * integrator refuses to take or return one — and the period parameters are plain {@link LocalDate}s rather than
 * FamilyCare's RFC 3339 timestamps, so no date rendering is needed on this side.
 *
 * <p>
 * Only the operations the financial assistance flow actually uses are declared. FamilyCare's investigations,
 * executions and resource allocations are on the direct client but called from nowhere, so they are left out rather
 * than carried along.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.lifecare-integrator.url}", configuration = se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface LifecareIntegratorClient {

	@GetMapping(path = "/{municipalityId}/person", produces = APPLICATION_JSON_VALUE)
	Person getPerson(@PathVariable final String municipalityId, @RequestParam final String partyId);

	@GetMapping(path = "/{municipalityId}/calculations", produces = APPLICATION_JSON_VALUE)
	PagedCalculationResponse getCalculations(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/calculations/proposal", produces = APPLICATION_JSON_VALUE)
	CalculationProposal getCalculationProposal(@PathVariable final String municipalityId, @RequestParam final String partyId);

	@GetMapping(path = "/{municipalityId}/decisions", produces = APPLICATION_JSON_VALUE)
	DecisionsResponse getDecisions(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/payments", produces = APPLICATION_JSON_VALUE)
	PagedPaymentResponse getPayments(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/actualisations", produces = APPLICATION_JSON_VALUE)
	PagedActualisationResponse getActualisations(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/services", produces = APPLICATION_JSON_VALUE)
	PagedServiceResponse getServices(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/documents", produces = APPLICATION_JSON_VALUE)
	PagedDocumentResponse getDocuments(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);
}
