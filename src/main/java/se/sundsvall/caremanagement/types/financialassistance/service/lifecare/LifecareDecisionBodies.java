package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.outcomeFor;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.text;

/**
 * The bodies of Decision/Create and Decision/Update: a Lifecare beslut object filled in with the caseworker's choices
 * the way Lifecare's web app does it. For a new beslut that is the underlag's blank one, for a change the registered
 * one. Each field keeps its place in the object; beslutstyp, period, orsak, belopp, beslutsfattare and meddelande are
 * filled in, every person is marked as included, and the fields the web app adds and drops are treated as it treats
 * them.
 *
 * <p>
 * Lifecare accepts a wrong beslut without complaint, so anything that cannot be decided safely is refused with 422
 * instead of sent: a beslutstyp whose category careM does not register yet, a household with a medsökande (sending
 * NOONE for one is accepted and silently leaves them out of the beslut), a beslutsfattare Lifecare does not know (the
 * beslut must name the caseworker, never fall back on the integration account), a period the beslutstyp requires but
 * the beslut lacks, and for a change also a beslut Lifecare has locked and a change of beslutstyp.
 * </p>
 *
 * <p>
 * The refusals are worded in Swedish for the caseworker, as the Draken BFF worded them.
 * </p>
 */
final class LifecareDecisionBodies {

	/** What the web app sends as coApplicant when the household has no medsökande. */
	static final String NO_CO_APPLICANT = "NOONE";

	static final String ERROR_UNKNOWN_TYPE = "Beslutstypen %s finns inte på insatsen i Lifecare.";
	static final String ERROR_UNREGISTERED_TYPE = "Beslutstypen \"%s\" kan inte registreras från Drakel ännu. Registrera beslutet direkt i Lifecare.";
	static final String ERROR_CO_APPLICANT = "Hushållet har en medsökande. Sådana beslut kan inte registreras i Lifecare från Drakel ännu.";
	static final String ERROR_UNKNOWN_DECISION_MAKER = "Handläggaren %s finns inte som beslutsfattare i Lifecare.";
	static final String ERROR_MISSING_PERIOD = "Beslutet saknar period, som beslutstypen kräver i Lifecare.";
	static final String ERROR_LOCKED = "Beslutet är låst i Lifecare och kan inte ändras från Drakel.";
	static final String ERROR_TYPE_CHANGE = "Beslutstypen kan inte ändras på ett beslut som redan finns i Lifecare. Ändra beslutet direkt i Lifecare.";
	static final String ERROR_NO_BESLUT = "Lifecare answered without a beslut object to fill in";

	private LifecareDecisionBodies() {}

	/**
	 * The Decision/Create body for a new beslut on the insats (capture 2026-09-23).
	 *
	 * @param  proposal the underlag, GetProposalForService
	 * @param  input    the caseworker's choices
	 * @return          the body
	 */
	static ObjectNode buildCreate(final JsonNode proposal, final LifecareDecisionInput input) {
		final var body = fill(proposal.path("decision"), proposal, input);
		// Only a new beslut goes without it; the web app keeps it on a change.
		body.remove("sharedCustody");
		return body;
	}

	/**
	 * The Decision/Update body for a beslut already registered in Lifecare (capture 2026-09-23): the beslut as
	 * GetDecision returned it, filled in as for a new one, with the beslutstyp sent back carrying an empty reasons list
	 * as the web app sends it. The underlag supplies the beslutstyper and beslutsfattare.
	 *
	 * @param  saved    the registered beslut, GetDecision
	 * @param  proposal the underlag, GetProposalForService
	 * @param  input    the caseworker's choices
	 * @return          the body
	 */
	static ObjectNode buildUpdate(final JsonNode saved, final JsonNode proposal, final LifecareDecisionInput input) {
		if (isTrue(saved.path("lockedMessage"))) {
			throw refuse(ERROR_LOCKED);
		}
		if (!Objects.equals(integer(saved.path("decisionCode")).orElse(null), input.decisionCode())) {
			throw refuse(ERROR_TYPE_CHANGE);
		}
		final var body = fill(saved, proposal, input);
		if (saved.path("type").isObject()) {
			final var type = (ObjectNode) saved.get("type").deepCopy();
			type.set("reasons", JsonNodeFactory.instance.arrayNode());
			body.set("type", type);
		}
		return body;
	}

	private static ObjectNode fill(final JsonNode base, final JsonNode proposal, final LifecareDecisionInput input) {
		if (!base.isObject()) {
			throw Problem.valueOf(BAD_GATEWAY, ERROR_NO_BESLUT);
		}
		final var decisionType = elements(proposal.path("decisionTypes")).stream()
			.filter(candidate -> Objects.equals(integer(candidate.path("code")).orElse(null), input.decisionCode()) && isTrue(candidate.path("isActive")))
			.findFirst()
			.orElseThrow(() -> refuse(ERROR_UNKNOWN_TYPE.formatted(input.decisionCode())));
		if (outcomeFor(decisionType.path("type")).isEmpty()) {
			throw refuse(ERROR_UNREGISTERED_TYPE.formatted(text(decisionType.path("name")).orElse("")));
		}

		if (elements(base.path("decisionPersons")).stream().anyMatch(person -> isTrue(person.path("coApplicant")))) {
			throw refuse(ERROR_CO_APPLICANT);
		}

		final var signature = Optional.ofNullable(input.decisionMakerId()).orElse("").toLowerCase(Locale.ROOT);
		final var decisionMaker = elements(proposal.path("decisionMakers")).stream()
			.filter(candidate -> text(candidate.path("id")).map(id -> id.toLowerCase(Locale.ROOT)).filter(signature::equals).isPresent())
			.findFirst()
			.orElseThrow(() -> refuse(ERROR_UNKNOWN_DECISION_MAKER.formatted(input.decisionMakerId())));

		final var fromDate = toLifecareDate(input.periodFrom());
		final var toDate = toLifecareDate(input.periodTo());
		if ((isTrue(decisionType.path("requiresFromDate")) && fromDate.isEmpty()) || (isTrue(decisionType.path("requiresToDate")) && toDate.isEmpty())) {
			throw refuse(ERROR_MISSING_PERIOD);
		}

		// Assigning onto a copy keeps each field where Lifecare had it; the web app's own additions go last.
		final var body = (ObjectNode) base.deepCopy();
		body.set("decisionCode", decisionType.path("code"));
		if (input.date() != null) {
			body.put("date", toLifecareDate(input.date()));
		}
		body.put("fromDate", fromDate);
		body.put("toDate", toDate);
		putReasonCode(body, input.reasonCode());
		body.put("reasonCodeCoApplicant", "");
		setOrRemove(body, "decisionMaker", decisionMaker.path("id"));
		// A registered beslut names its beslutsfattare; a blank one leaves the name to Lifecare.
		if (base.path("decisionMakerName").isString()) {
			setOrRemove(body, "decisionMakerName", decisionMaker.path("name"));
		}
		setOrRemove(body, "decisionMakerTitle", decisionMaker.path("title"));
		final var persons = body.putArray("decisionPersons");
		elements(base.path("decisionPersons")).forEach(person -> persons.add(include(person)));
		putAmount(body, Optional.ofNullable(input.amount()).orElse(BigDecimal.ZERO));
		body.put("coApplicant", NO_CO_APPLICANT);
		body.put("message", input.message());
		if (input.writeProtect()) {
			body.put("lockedMessage", true);
		}
		body.remove("aktualiseringId");
		body.remove("whereDidChildGoType");
		body.remove("guardianType");
		return body;
	}

	/** A person the beslut concerns, marked as included as the web app marks it. */
	private static JsonNode include(final JsonNode person) {
		if (!person.isObject()) {
			return person;
		}
		final var included = (ObjectNode) person.deepCopy();
		included.put("included", true);
		included.remove("notificationType");
		included.put("personIdAndName", text(person.path("personIdFormatted")).orElse("") + ", " + text(person.path("name")).orElse(""));
		included.put("receivedDateIsInvalid", false);
		included.put("isValid", true);
		return included;
	}

	/** A careM date as Lifecare's yyyy-MM-dd; an empty string when there is none. */
	private static String toLifecareDate(final LocalDate date) {
		return Optional.ofNullable(date).map(LocalDate::toString).orElse("");
	}

	/** The orsak as a number, or the empty string Lifecare's web app sends for none. */
	private static void putReasonCode(final ObjectNode body, final Integer reasonCode) {
		if (reasonCode == null) {
			body.put("reasonCode", "");
			return;
		}
		body.put("reasonCode", reasonCode.intValue());
	}

	/** A whole amount as an integer, as Lifecare's web app sends it; a fraction as a decimal. */
	private static void putAmount(final ObjectNode body, final BigDecimal amount) {
		final var stripped = amount.stripTrailingZeros();
		if (stripped.scale() <= 0) {
			body.put("amount", stripped.longValueExact());
			return;
		}
		body.put("amount", stripped);
	}

	/** A value Lifecare left out is left out of the body too, as the web app's JSON leaves an undefined field out. */
	private static void setOrRemove(final ObjectNode body, final String field, final JsonNode value) {
		if (value.isMissingNode()) {
			body.remove(field);
			return;
		}
		body.set(field, value);
	}

	private static RuntimeException refuse(final String reason) {
		return Problem.valueOf(UNPROCESSABLE_CONTENT, reason);
	}
}
