package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

/**
 * The ProfessionalWeb pass-through of api-service-lifecare-integrator. Same service, gateway and OAuth2 registration
 * as the FamilyCare reads, a client of its own so the two do not share a circuit breaker.
 */
@FeignClient(name = ProfessionalWebIntegratorClient.CLIENT_ID, url = "${integration.lifecare-integrator.url}", configuration = ProfessionalWebIntegratorConfiguration.class)
@CircuitBreaker(name = ProfessionalWebIntegratorClient.CLIENT_ID)
public interface ProfessionalWebIntegratorClient {

	String CLIENT_ID = "lifecare-integrator-professionalweb";

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

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/job-stimulus", produces = APPLICATION_JSON_VALUE)
	JsonNode readJobStimulus(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/job-stimulus/end-date", produces = APPLICATION_JSON_VALUE)
	JsonNode readJobStimulusEndDate(@PathVariable final String municipalityId, @RequestParam final String fromDate);

	@PostMapping(path = "/{municipalityId}/professional-web/job-stimulus", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode saveJobStimulus(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/decisions/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readDecisionProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId, @RequestParam final String amountType,
		@RequestParam final int calculationId);

	@GetMapping(path = "/{municipalityId}/professional-web/decision-codes/{decisionCode}/reasons", produces = APPLICATION_JSON_VALUE)
	JsonNode readDecisionReasons(@PathVariable final String municipalityId, @PathVariable final int decisionCode);

	@GetMapping(path = "/{municipalityId}/professional-web/decisions/{decisionId}", produces = APPLICATION_JSON_VALUE)
	JsonNode readDecision(@PathVariable final String municipalityId, @PathVariable final int decisionId);

	@PostMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/decisions", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createDecision(@PathVariable final String municipalityId, @PathVariable final int serviceId, @RequestBody final JsonNode body);

	@PutMapping(path = "/{municipalityId}/professional-web/decisions/{decisionId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode updateDecision(@PathVariable final String municipalityId, @PathVariable final int decisionId, @RequestBody final JsonNode body);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/payments/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readPaymentProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/payments/latest", produces = APPLICATION_JSON_VALUE)
	JsonNode readLatestPayments(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/household", produces = APPLICATION_JSON_VALUE)
	JsonNode hasHousehold(@PathVariable final String municipalityId, @PathVariable final int serviceId, @RequestParam final String date);

	@PostMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/payments", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createPayment(@PathVariable final String municipalityId, @PathVariable final int serviceId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/payees", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createPayee(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/calculations/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readCalculationProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/calculations/latest", produces = APPLICATION_JSON_VALUE)
	JsonNode listLatestCalculations(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/calculations/{calculationId}", produces = APPLICATION_JSON_VALUE)
	JsonNode readCalculation(@PathVariable final String municipalityId, @PathVariable final int calculationId);

	@GetMapping(path = "/{municipalityId}/professional-web/calculations/{calculationId}/edit", produces = APPLICATION_JSON_VALUE)
	JsonNode readCalculationForEdit(@PathVariable final String municipalityId, @PathVariable final int calculationId);

	@PostMapping(path = "/{municipalityId}/professional-web/calculations/place-persons", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode placePersons(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/calculations/amount", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode readAmount(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/calculations/shared-cost", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode readSharedCost(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/calculations/job-stimuli", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode readJobStimuliForCalculation(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/calculations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createCalculation(@PathVariable final String municipalityId, @PathVariable final int serviceId, @RequestBody final JsonNode body);

	@PutMapping(path = "/{municipalityId}/professional-web/calculations/{calculationId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode updateCalculation(@PathVariable final String municipalityId, @PathVariable final int calculationId, @RequestBody final JsonNode body);

	@DeleteMapping(path = "/{municipalityId}/professional-web/calculations/{calculationId}")
	void deleteCalculation(@PathVariable final String municipalityId, @PathVariable final int calculationId);

	@GetMapping(path = "/{municipalityId}/professional-web/persons/{partyId}/documents", produces = APPLICATION_JSON_VALUE)
	JsonNode listDocuments(@PathVariable final String municipalityId, @PathVariable final String partyId);

	@GetMapping(path = "/{municipalityId}/professional-web/journal-notes/{id}", produces = APPLICATION_JSON_VALUE)
	JsonNode readJournalNote(@PathVariable final String municipalityId, @PathVariable final int id);

	@GetMapping(path = "/{municipalityId}/professional-web/journal-notes/{id}/edit", produces = APPLICATION_JSON_VALUE)
	JsonNode readJournalNoteForEdit(@PathVariable final String municipalityId, @PathVariable final int id);

	@GetMapping(path = "/{municipalityId}/professional-web/documents/{id}", produces = APPLICATION_JSON_VALUE)
	JsonNode readDocument(@PathVariable final String municipalityId, @PathVariable final int id);

	@GetMapping(path = "/{municipalityId}/professional-web/documents/{id}/edit", produces = APPLICATION_JSON_VALUE)
	JsonNode readDocumentForEdit(@PathVariable final String municipalityId, @PathVariable final int id);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/journal-notes/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readJournalNoteProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@GetMapping(path = "/{municipalityId}/professional-web/services/{serviceId}/documents/proposal", produces = APPLICATION_JSON_VALUE)
	JsonNode readDocumentProposal(@PathVariable final String municipalityId, @PathVariable final int serviceId);

	@PostMapping(path = "/{municipalityId}/professional-web/journal-notes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createJournalNote(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PostMapping(path = "/{municipalityId}/professional-web/documents", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode createDocument(@PathVariable final String municipalityId, @RequestBody final JsonNode body);

	@PutMapping(path = "/{municipalityId}/professional-web/journal-notes/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode updateJournalNote(@PathVariable final String municipalityId, @PathVariable final int id, @RequestBody final JsonNode body);

	@PutMapping(path = "/{municipalityId}/professional-web/documents/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	JsonNode updateDocument(@PathVariable final String municipalityId, @PathVariable final int id, @RequestBody final JsonNode body);

	@GetMapping(path = "/{municipalityId}/professional-web/decisions/{decisionId}/pdf", produces = APPLICATION_PDF_VALUE)
	byte[] printDecision(@PathVariable final String municipalityId, @PathVariable final int decisionId);

	@GetMapping(path = "/{municipalityId}/professional-web/documents/{id}/pdf", produces = APPLICATION_PDF_VALUE)
	byte[] printDocument(@PathVariable final String municipalityId, @PathVariable final int id);

	@GetMapping(path = "/{municipalityId}/professional-web/calculations/{calculationId}/pdf", produces = APPLICATION_PDF_VALUE)
	byte[] printCalculation(@PathVariable final String municipalityId, @PathVariable final int calculationId);

}
