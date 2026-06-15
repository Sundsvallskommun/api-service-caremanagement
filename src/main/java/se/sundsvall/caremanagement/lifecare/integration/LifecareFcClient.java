package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.caremanagement.lifecare.integration.configuration.LifecareFcConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.caremanagement.lifecare.integration.configuration.LifecareFcConfiguration.CLIENT_ID;

/**
 * Feign contract for the EB write-back subset of the Tieto/Lifecare FamilyCare (FC) API: create an aktualisering (case
 * intake) and a calculation (normberäkning), plus the two proposal lookups that supply the code lists those POST bodies
 * reference. The mandatory {@code domain} + {@code key} auth (and the {@code X-API-Key} header) are added globally by
 * {@link LifecareFcConfiguration}, so they are not part of these method signatures. Full API documented in
 * vof-ekonomiskt-bistand/architecture/lifecare-fc-api.md.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.lifecare-fc.url}", configuration = LifecareFcConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface LifecareFcClient {

	/**
	 * Fetch the proposal (valid code lists: types, reasons, fromWho, organisations, working status, investigation/service
	 * types, attachment types) needed to build a {@link PostAktualiseringsBodyRequest} for the given person.
	 *
	 * @param  personId the full personnummer the aktualisering concerns
	 * @return          the aktualisering proposal lookups
	 */
	@GetMapping(path = "/apifc/v1/Actualisations/Proposals", produces = APPLICATION_JSON_VALUE)
	PersonBasedAktualiseringProposalDTO getActualisationProposal(
		@RequestParam("personId") final String personId);

	/**
	 * Create an aktualisering (case intake) in Lifecare FC.
	 *
	 * @param  body the aktualisering to create (codes resolved from {@link #getActualisationProposal(String)})
	 * @return      the id of the created aktualisering
	 */
	@PostMapping(path = "/apifc/v1/Actualisations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Integer createActualisation(
		@RequestBody final PostAktualiseringsBodyRequest body);

	/**
	 * Fetch the proposal (norms, household members, income/expense/special-expense types, linkable
	 * investigations/services/aktualiseringar) needed to build a {@link PostCalculationBodyRequest} for the given person.
	 *
	 * @param  personId the full personnummer the calculation concerns
	 * @return          the calculation proposal lookups
	 */
	@GetMapping(path = "/apifc/v1/Calculations/Proposals", produces = APPLICATION_JSON_VALUE)
	PersonBasedCalculationProposalDTO getCalculationProposal(
		@RequestParam("personId") final String personId);

	/**
	 * Create a calculation (normberäkning) in Lifecare FC.
	 *
	 * @param  body the calculation to create (codes resolved from {@link #getCalculationProposal(String)})
	 * @return      the id of the created calculation
	 */
	@PostMapping(path = "/apifc/v1/Calculations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Integer createCalculation(
		@RequestBody final PostCalculationBodyRequest body);
}
