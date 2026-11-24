package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.integrator.ProfessionalWebIntegratorClient;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Lifecare ProfessionalWeb through lifecare-integrator's resources, one method per resource.
 *
 * <p>
 * Callers deal in JSON trees and never in cookies. Trees rather than typed objects on purpose: every write to
 * ProfessionalWeb is the object Lifecare handed out (a proposal, or the current record) with a few fields changed, and
 * a typed round trip would silently drop every field the type does not know about.
 * </p>
 *
 * <p>
 * The integrator holds the integration account's session and passes Lifecare's JSON through untouched. Nothing is
 * retried here.
 * </p>
 *
 * <p>
 * Nothing that crosses this client is logged: answers carry personnummer, and request
 * bodies carry whatever the caseworker wrote.
 * </p>
 */
@Component
public class ProfessionalWebClient {

	private static final byte[] PDF_SIGNATURE = "%PDF".getBytes(StandardCharsets.US_ASCII);
	static final String ERROR_NOT_A_PDF = "Lifecare did not answer with a PDF";

	/** The integrator's path takes a municipality; there is one Lifecare, Sundsvall's. */
	private static final String MUNICIPALITY_ID = "2281";

	private final ProfessionalWebIntegratorClient integrator;

	public ProfessionalWebClient(final ProfessionalWebIntegratorClient integrator) {
		this.integrator = integrator;
	}

	/** The bevakningar on an insats. */
	public JsonNode listReminders(final int serviceId) {
		return read(() -> integrator.listReminders(MUNICIPALITY_ID, serviceId));
	}

	/** The blank bevakning and the options for a new one on an insats. */
	public JsonNode readReminderProposal(final int serviceId) {
		return read(() -> integrator.readReminderProposal(MUNICIPALITY_ID, serviceId));
	}

	/** A bevakning, as Lifecare's editor reads it. */
	public JsonNode readReminder(final int reminderId) {
		return read(() -> integrator.readReminder(MUNICIPALITY_ID, reminderId));
	}

	/** Creates a bevakning. Not idempotent. */
	public JsonNode createReminder(final JsonNode body) {
		return read(() -> integrator.createReminder(MUNICIPALITY_ID, body));
	}

	/** Saves a changed bevakning; the body's reminderId is the one given. */
	public void updateReminder(final int reminderId, final JsonNode body) {
		write(() -> integrator.updateReminder(MUNICIPALITY_ID, reminderId, body));
	}

	/** Removes a bevakning. */
	public void removeReminder(final int reminderId) {
		write(() -> integrator.removeReminder(MUNICIPALITY_ID, reminderId));
	}

	/** The sökandes and medsökandes jobbstimulans periods on an insats. */
	public JsonNode readJobStimulus(final int serviceId) {
		return read(() -> integrator.readJobStimulus(MUNICIPALITY_ID, serviceId));
	}

	/** The end Lifecare gives a jobbstimulans period starting on the date, a bare JSON string. */
	public JsonNode readJobStimulusEndDate(final String fromDate) {
		return read(() -> integrator.readJobStimulusEndDate(MUNICIPALITY_ID, fromDate));
	}

	/** Saves a person's jobbstimulans periods; a period left out is deleted. */
	public JsonNode saveJobStimulus(final JsonNode body) {
		return read(() -> integrator.saveJobStimulus(MUNICIPALITY_ID, body));
	}

	/**
	 * The underlag for a new beslut on an insats; an empty amountType and calculation 0 for one made without a beräkning.
	 */
	public JsonNode readDecisionProposal(final int serviceId, final String amountType, final int calculationId) {
		return read(() -> integrator.readDecisionProposal(MUNICIPALITY_ID, serviceId, amountType, calculationId));
	}

	/** The orsak catalogue of a beslutstyp. */
	public JsonNode readDecisionReasons(final int decisionCode) {
		return read(() -> integrator.readDecisionReasons(MUNICIPALITY_ID, decisionCode));
	}

	/** A beslut, whole: the object an update takes back. */
	public JsonNode readDecision(final int decisionId) {
		return read(() -> integrator.readDecision(MUNICIPALITY_ID, decisionId));
	}

	/** Registers a beslut on an insats. Not idempotent. */
	public JsonNode createDecision(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createDecision(MUNICIPALITY_ID, serviceId, body));
	}

	/** Saves a changed beslut. */
	public JsonNode updateDecision(final int decisionId, final JsonNode body) {
		return read(() -> integrator.updateDecision(MUNICIPALITY_ID, decisionId, body));
	}

	/** The underlag for a new utbetalning on an insats: blank payment, payees, betalsätt, saldon and months. */
	public JsonNode readPaymentProposal(final int serviceId) {
		return read(() -> integrator.readPaymentProposal(MUNICIPALITY_ID, serviceId));
	}

	/** The latest utbetalningar registered on an insats. */
	public JsonNode readLatestPayments(final int serviceId) {
		return read(() -> integrator.readLatestPayments(MUNICIPALITY_ID, serviceId));
	}

	/** Whether the person of the insats has a hushåll on the date (ISO), as a JSON boolean. */
	public JsonNode hasHousehold(final int serviceId, final String date) {
		return read(() -> integrator.hasHousehold(MUNICIPALITY_ID, serviceId, date));
	}

	/** Registers an utbetalning on an insats. Not idempotent and it moves money: never retried. */
	public JsonNode createPayment(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createPayment(MUNICIPALITY_ID, serviceId, body));
	}

	/** Creates a betalningsmottagare. Not idempotent. */
	public JsonNode createPayee(final JsonNode body) {
		return read(() -> integrator.createPayee(MUNICIPALITY_ID, body));
	}

	/** The underlag for a new beräkning on an insats: a blank one with the household, and the catalogues. */
	public JsonNode readCalculationProposal(final int serviceId) {
		return read(() -> integrator.readCalculationProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Every beräkning on an insats, without rows, as an array. */
	public JsonNode listLatestCalculations(final int serviceId) {
		return read(() -> integrator.listLatestCalculations(MUNICIPALITY_ID, serviceId));
	}

	/** A saved beräkning as Lifecare shows it. */
	public JsonNode readCalculation(final int calculationId) {
		return read(() -> integrator.readCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** A saved beräkning whole, what an update takes back, with the catalogues. */
	public JsonNode readCalculationForEdit(final int calculationId) {
		return read(() -> integrator.readCalculationForEdit(MUNICIPALITY_ID, calculationId));
	}

	/** Places the members on the norm's rows. Changes nothing in Lifecare. */
	public JsonNode placePersons(final JsonNode body) {
		return read(() -> integrator.placePersons(MUNICIPALITY_ID, body));
	}

	/** A member's amount for a period. Changes nothing in Lifecare. */
	public JsonNode readAmount(final JsonNode body) {
		return read(() -> integrator.readAmount(MUNICIPALITY_ID, body));
	}

	/** The gemensamma kostnader for a period, a bare number. Changes nothing in Lifecare. */
	public JsonNode readSharedCost(final JsonNode body) {
		return read(() -> integrator.readSharedCost(MUNICIPALITY_ID, body));
	}

	/** Marks which members have jobbstimulans during a beräkning's period. Changes nothing in Lifecare. */
	public JsonNode readJobStimuliForCalculation(final JsonNode body) {
		return read(() -> integrator.readJobStimuliForCalculation(MUNICIPALITY_ID, body));
	}

	/** Creates a beräkning on an insats. Not idempotent. */
	public JsonNode createCalculation(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createCalculation(MUNICIPALITY_ID, serviceId, body));
	}

	/** Saves a changed beräkning; Lifecare answers with it recounted. */
	public JsonNode updateCalculation(final int calculationId, final JsonNode body) {
		return read(() -> integrator.updateCalculation(MUNICIPALITY_ID, calculationId, body));
	}

	/** Removes a beräkning. */
	public void deleteCalculation(final int calculationId) {
		write(() -> integrator.deleteCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** The journalanteckningar and documents in a person's record. */
	public JsonNode listDocuments(final String partyId) {
		return read(() -> integrator.listDocuments(MUNICIPALITY_ID, partyId));
	}

	/** A journalanteckning with its content, in view mode. */
	public JsonNode readJournalNote(final int id) {
		return read(() -> integrator.readJournalNote(MUNICIPALITY_ID, id));
	}

	/** A journalanteckning as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readJournalNoteForEdit(final int id) {
		return read(() -> integrator.readJournalNoteForEdit(MUNICIPALITY_ID, id));
	}

	/** A document with its content, in view mode. */
	public JsonNode readDocument(final int id) {
		return read(() -> integrator.readDocument(MUNICIPALITY_ID, id));
	}

	/** A document as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readDocumentForEdit(final int id) {
		return read(() -> integrator.readDocumentForEdit(MUNICIPALITY_ID, id));
	}

	/** The blank journalanteckning and its note types on an insats. */
	public JsonNode readJournalNoteProposal(final int serviceId) {
		return read(() -> integrator.readJournalNoteProposal(MUNICIPALITY_ID, serviceId));
	}

	/** The blank document and its document types on an insats. */
	public JsonNode readDocumentProposal(final int serviceId) {
		return read(() -> integrator.readDocumentProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Writes a new journalanteckning. Not idempotent. */
	public JsonNode createJournalNote(final JsonNode body) {
		return read(() -> integrator.createJournalNote(MUNICIPALITY_ID, body));
	}

	/** Writes a new document. Not idempotent. */
	public JsonNode createDocument(final JsonNode body) {
		return read(() -> integrator.createDocument(MUNICIPALITY_ID, body));
	}

	/** Saves a changed journalanteckning. */
	public JsonNode updateJournalNote(final int id, final JsonNode body) {
		return read(() -> integrator.updateJournalNote(MUNICIPALITY_ID, id, body));
	}

	/** Saves a changed document. */
	public JsonNode updateDocument(final int id, final JsonNode body) {
		return read(() -> integrator.updateDocument(MUNICIPALITY_ID, id, body));
	}

	/** A beslut as Lifecare's own print renders it. */
	public byte[] printDecision(final int decisionId) {
		return pdf(() -> integrator.printDecision(MUNICIPALITY_ID, decisionId));
	}

	/** A written document or blankett as Lifecare's own print renders it. */
	public byte[] printDocument(final int id) {
		return pdf(() -> integrator.printDocument(MUNICIPALITY_ID, id));
	}

	/** A beräkning as Lifecare's own print renders it. */
	public byte[] printCalculation(final int calculationId) {
		return pdf(() -> integrator.printCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** A PDF from the integrator; anything else is a 502. */
	private static byte[] pdf(final Supplier<byte[]> call) {
		return Optional.ofNullable(ProfessionalWebErrors.call(call))
			.filter(body -> startsWith(body, PDF_SIGNATURE))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_NOT_A_PDF));
	}

	/** An answer from the integrator; a missing node when it answered without a body. */
	private static JsonNode read(final Supplier<JsonNode> call) {
		return Optional.ofNullable(ProfessionalWebErrors.call(call)).orElse(MissingNode.getInstance());
	}

	private static void write(final Runnable call) {
		ProfessionalWebErrors.call(() -> {
			call.run();
			return null;
		});
	}

	private static boolean startsWith(final byte[] body, final byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
