package se.sundsvall.caremanagement.financialaid.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.caremanagement.financialaid.integration.configuration.FinancialAidConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.financialaid.integration.configuration.FinancialAidConfiguration.CLIENT_ID;

/**
 * Feign contract for api-service-financial-aid, the SSBTEK proxy (SOAP + mTLS to Försäkringskassan's composite
 * service).
 * The response is the aggregated per-agency basis (af/csn/fk/skv/so/tns/miv) returned as a nested, untyped JSON map —
 * the agency payloads are heterogeneous, so there is no typed model. The SSBTEK reader downstream turns this into
 * normalised {@code SsbtekIncome}s; see vof-ekonomiskt-bistand/architecture/lifecare-familycare-api.md.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.financial-aid.url}", configuration = FinancialAidConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface FinancialAidClient {

	/**
	 * Fetch a person's aggregated financial-aid (SSBTEK) basis for a period.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  personalNumber the person's full personnummer
	 * @param  fromDate       the period start (yyyy-MM-dd)
	 * @param  toDate         the period end (yyyy-MM-dd)
	 * @return                the per-agency basis as a nested, untyped JSON map
	 */
	@GetMapping(path = "/{municipalityId}/financial-aid", produces = APPLICATION_JSON_VALUE)
	Map<String, Map<String, Object>> getFinancialAidBasis(
		@PathVariable final String municipalityId,
		@RequestParam("personalNumber") final String personalNumber,
		@RequestParam("fromDate") final String fromDate,
		@RequestParam("toDate") final String toDate);
}
