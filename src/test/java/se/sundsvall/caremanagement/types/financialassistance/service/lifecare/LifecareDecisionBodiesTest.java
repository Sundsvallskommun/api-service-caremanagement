package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionBodies.buildCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionBodies.buildUpdate;

/**
 * Ported from the Draken BFF's lifecare-decision.test.ts and lifecare-decision-update.test.ts: the bodies must match
 * the captures of Lifecare's own web app field for field and in order.
 */
class LifecareDecisionBodiesTest {

	static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String MESSAGE = "<p style=\"font-family: 'Times New Roman'; font-size: 12pt;\">Din ans&ouml;kan om f&ouml;rs&ouml;rjningsst&ouml;d enligt 12 Kap 1&sect; SoL&nbsp; Socialtj&auml;nstlagen f&ouml;r period xx-xx har avslagits/delvis avslagits.</p>";

	static JsonNode fixture(final String name) {
		try {
			return JSON.readTree(new ClassPathResource("lifecare/decision/" + name).getContentAsString(StandardCharsets.UTF_8));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The fixture as compact JSON, its fields in the file's order: the comparison is field for field and in order. */
	private static String raw(final String name) {
		return JSON.writeValueAsString(fixture(name));
	}

	private static LifecareDecisionInput bifall() {
		return new LifecareDecisionInput(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), new BigDecimal("5"), 16,
			"<p>test av beslutsmeddelande</p>", false, "RPA_031DEV");
	}

	private static LifecareDecisionInput withCode(final LifecareDecisionInput input, final int code) {
		return new LifecareDecisionInput(code, input.date(), input.periodFrom(), input.periodTo(), input.amount(), input.reasonCode(), input.message(),
			input.writeProtect(), input.decisionMakerId());
	}

	private static LifecareDecisionInput withMaker(final LifecareDecisionInput input, final String maker) {
		return new LifecareDecisionInput(input.decisionCode(), input.date(), input.periodFrom(), input.periodTo(), input.amount(), input.reasonCode(),
			input.message(), input.writeProtect(), maker);
	}

	private static LifecareDecisionInput avslag() {
		return new LifecareDecisionInput(152, null, null, null, BigDecimal.ZERO, null, MESSAGE, false, "RPA_031DEV");
	}

	private static void assertRefused(final Runnable call, final String reasonPart) {
		assertThatThrownBy(call::run)
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining(reasonPart);
	}

	// Decision/Create

	@Test
	void createTurnsTheCapturedUnderlagIntoExactlyTheCapturedBody() {
		final var body = buildCreate(fixture("create-proposal.json"), bifall());

		assertThat(JSON.writeValueAsString(body)).isEqualTo(raw("create-body.json"));
	}

	@Test
	void createNamesTheCaseworkerWhateverTheCaseOfTheirSignature() {
		final var body = buildCreate(fixture("create-proposal.json"), withMaker(bifall(), "test"));

		assertThat(body.path("decisionMaker").stringValue()).isEqualTo("TEST");
		assertThat(body.path("decisionMakerTitle").stringValue()).isEqualTo("Testhandläggare");
		// A blank beslut leaves the name to Lifecare.
		assertThat(body.path("decisionMakerName").isNull()).isTrue();
	}

	@Test
	void createRefusesADecisionMakerLifecareDoesNotKnowNeverFallingBack() {
		assertRefused(() -> buildCreate(fixture("create-proposal.json"), withMaker(bifall(), "oli09bor")), "oli09bor");
	}

	@Test
	void createRefusesAMissingDecisionMaker() {
		assertRefused(() -> buildCreate(fixture("create-proposal.json"), withMaker(bifall(), null)), "beslutsfattare");
	}

	@Test
	void createRegistersAnyBifallTypeGoingByCategoryRatherThanCode() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		proposal.withArrayProperty("decisionTypes").addObject()
			.put("code", 150).put("name", "EK Ekonomiskt bistånd 12 Kap 2 § SoL, bifall").put("type", 0).put("isActive", true)
			.put("requiresFromDate", true).put("requiresToDate", true);

		assertThat(buildCreate(proposal, withCode(bifall(), 150)).path("decisionCode").asInt()).isEqualTo(150);
	}

	@Test
	void createRefusesABeslutstypCaremDoesNotRegister() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		proposal.withArrayProperty("decisionTypes").addObject()
			.put("code", 161).put("name", "EK Återkrav Ekonomiskt bistånd, grundbeslut").put("isActive", true)
			.put("requiresFromDate", false).put("requiresToDate", false);

		assertRefused(() -> buildCreate(proposal, withCode(bifall(), 161)), "EK Återkrav Ekonomiskt bistånd, grundbeslut");
	}

	@Test
	void createRefusesAnInactiveOrUnknownBeslutstyp() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		proposal.withArrayProperty("decisionTypes").addObject()
			.put("code", 9).put("name", "Utgången").put("type", 0).put("isActive", false);

		assertRefused(() -> buildCreate(proposal, withCode(bifall(), 9)), "Beslutstypen 9 finns inte");
		assertRefused(() -> buildCreate(proposal, withCode(bifall(), 4711)), "Beslutstypen 4711 finns inte");
	}

	@Test
	void createRefusesAHouseholdWithACoApplicantRatherThanSendingNoone() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		final var persons = proposal.withObjectProperty("decision").withArrayProperty("decisionPersons");
		final var coApplicant = (ObjectNode) persons.get(0).deepCopy();
		coApplicant.put("personId", "19850101T222").put("coApplicant", true);
		persons.add(coApplicant);

		assertRefused(() -> buildCreate(proposal, bifall()), "medsökande");
	}

	@Test
	void createRefusesABifallWithoutThePeriodItsTypeRequiresButTakesAnAvslagWithoutOne() {
		final var noFrom = new LifecareDecisionInput(153, null, null, LocalDate.of(2026, 9, 30), new BigDecimal("5"), 16, null, false, "RPA_031DEV");
		final var noTo = new LifecareDecisionInput(153, null, LocalDate.of(2026, 9, 1), null, new BigDecimal("5"), 16, null, false, "RPA_031DEV");
		final var avslagWithoutPeriod = new LifecareDecisionInput(152, null, null, null, BigDecimal.ZERO, null, null, false, "RPA_031DEV");

		assertRefused(() -> buildCreate(fixture("create-proposal.json"), noFrom), "saknar period");
		assertRefused(() -> buildCreate(fixture("create-proposal.json"), noTo), "saknar period");
		final var body = buildCreate(fixture("create-proposal.json"), avslagWithoutPeriod);
		assertThat(body.path("decisionCode").asInt()).isEqualTo(152);
		assertThat(body.path("fromDate").stringValue()).isEmpty();
		assertThat(body.path("reasonCode").stringValue()).isEmpty();
		assertThat(body.path("amount").asInt()).isZero();
		assertThat(body.path("message").isNull()).isTrue();
	}

	@Test
	void createCarriesTheDateAFractionalAmountAndWriteProtection() {
		final var input = new LifecareDecisionInput(153, LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
			new BigDecimal("7900.50"), 16, "<p>x</p>", true, "RPA_031DEV");

		final var body = buildCreate(fixture("create-proposal.json"), input);

		assertThat(body.path("date").stringValue()).isEqualTo("2026-09-24");
		assertThat(body.path("amount").asDecimal()).isEqualByComparingTo("7900.5");
		assertThat(body.path("lockedMessage").booleanValue()).isTrue();
		assertThat(body.has("sharedCustody")).isFalse();
	}

	@Test
	void createRefusesAnUnderlagWithoutABeslut() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		proposal.remove("decision");

		assertThatThrownBy(() -> buildCreate(proposal, bifall()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void createLeavesOutATitleLifecareDoesNotGive() {
		final var proposal = (ObjectNode) fixture("create-proposal.json");
		((ObjectNode) proposal.withArrayProperty("decisionMakers").get(1)).remove("title");

		assertThat(buildCreate(proposal, bifall()).has("decisionMakerTitle")).isFalse();
	}

	// Decision/Update

	@Test
	void updateTurnsTheReadBeslutIntoExactlyTheCapturedBody() {
		final var body = buildUpdate(fixture("update-saved.json"), fixture("update-proposal.json"), avslag());

		assertThat(JSON.writeValueAsString(body)).isEqualTo(raw("update-body.json"));
	}

	@Test
	void updateCarriesTheChangedMessageAndNamesTheCaseworkerWhoSavedIt() {
		final var input = new LifecareDecisionInput(152, null, null, null, BigDecimal.ZERO, null, "<p>Nytt</p>", false, "test");

		final var body = buildUpdate(fixture("update-saved.json"), fixture("update-proposal.json"), input);

		assertThat(body.path("message").stringValue()).isEqualTo("<p>Nytt</p>");
		assertThat(body.path("decisionMaker").stringValue()).isEqualTo("TEST");
		assertThat(body.path("decisionMakerName").stringValue()).isEqualTo("Test Handläggare");
		assertThat(body.path("type").path("reasons").isArray()).isTrue();
	}

	@Test
	void updateRefusesABeslutWhoseMessageLifecareHasLocked() {
		final var saved = (ObjectNode) fixture("update-saved.json");
		saved.put("lockedMessage", true);

		assertRefused(() -> buildUpdate(saved, fixture("update-proposal.json"), avslag()), "låst");
	}

	@Test
	void updateRefusesToChangeTheBeslutstypOfARegisteredBeslut() {
		final var input = new LifecareDecisionInput(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), BigDecimal.ZERO, null, MESSAGE, false,
			"RPA_031DEV");

		assertRefused(() -> buildUpdate(fixture("update-saved.json"), fixture("update-proposal.json"), input), "Beslutstypen kan inte ändras");
	}

	@Test
	void updateSendsNoTypeWhenTheBeslutHasNone() {
		final var saved = (ObjectNode) fixture("update-saved.json");
		saved.putNull("type");

		assertThat(buildUpdate(saved, fixture("update-proposal.json"), avslag()).path("type").isNull()).isTrue();
	}
}
