package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionView;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionBodiesTest.JSON;

class LifecareDecisionMapperTest {

	static final String SAVED = """
		{
		  "decisionId": 98, "decisionCode": 153, "decisionType": 0, "date": "2026-09-23",
		  "fromDate": "2026-09-01", "toDate": "2026-09-30", "reasonCode": 19,
		  "reason": "Arbetar deltid ofrivilligt, otillräcklig inkomst", "decisionMaker": "TEST",
		  "decisionMakerName": "Test Handläggare", "amount": 3000,
		  "decisionPersons": [{"personId": "19880209T050", "name": "Testsson, Test", "coApplicant": false, "personIdFormatted": "880209-T050"}],
		  "type": {"code": 153}, "message": "<p>Beslut</p>", "lockedMessage": false
		}""";

	static JsonNode tree(final String json) {
		return JSON.readTree(json);
	}

	@Test
	void showsTheSavedBeslutAsLifecareHasItWithoutThePersonnummer() {
		assertThat(LifecareDecisionMapper.toView(tree(SAVED))).isEqualTo(new LifecareDecisionView(98, 153, "BIFALL", "2026-09-23", "2026-09-01", "2026-09-30",
			new BigDecimal("3000"), 19, "Arbetar deltid ofrivilligt, otillräcklig inkomst", "<p>Beslut</p>", false, "Test Handläggare"));
	}

	@Test
	void readsLifecaresEmptyValuesAsAbsent() {
		final var view = LifecareDecisionMapper.toView(tree("""
			{"decisionId": 7, "decisionCode": 161, "decisionType": 9, "date": "", "fromDate": "", "toDate": null,
			 "reasonCode": 0, "reason": "", "message": null, "decisionMaker": "RPA_031DEV", "decisionMakerName": null, "lockedMessage": true}"""));

		assertThat(view).isEqualTo(new LifecareDecisionView(7, 161, null, "", null, null, BigDecimal.ZERO, null, null, null, true, "RPA_031DEV"));
		assertThat(LifecareDecisionMapper.toView(tree("{}")).decisionMaker()).isEmpty();
		assertThat(LifecareDecisionMapper.toView(tree("{\"reasonCode\": \"\"}")).reasonCode()).isNull();
	}

	@Test
	void mapsTheCategoriesCaremRegisters() {
		final var factory = JsonNodeFactory.instance;
		assertThat(LifecareDecisionMapper.outcomeFor(factory.numberNode(0))).contains("BIFALL");
		assertThat(LifecareDecisionMapper.outcomeFor(factory.numberNode(10))).contains("AVSLAG");
		assertThat(LifecareDecisionMapper.outcomeFor(factory.numberNode(9))).isEmpty();
		assertThat(LifecareDecisionMapper.outcomeFor(factory.missingNode())).isEmpty();
	}

	@Test
	void listsTheActiveBeslutstyperMarkingTheOnesCaremRegisters() {
		final var types = LifecareDecisionMapper.toTypes(tree("""
			{"decisionTypes": [
			  {"code": 152, "name": "avslag", "type": 10, "isActive": true, "requiresFromDate": false, "requiresToDate": false},
			  {"code": 153, "name": "bifall", "type": 0, "isActive": true, "requiresFromDate": true, "requiresToDate": true},
			  {"code": 161, "name": "EK Återkrav", "type": 9, "isActive": true, "requiresFromDate": false, "requiresToDate": false},
			  {"code": 9, "name": "Utgången", "type": 9, "isActive": false, "requiresFromDate": false, "requiresToDate": false}
			]}"""));

		assertThat(types).extracting(LifecareDecisionType::code, LifecareDecisionType::outcome)
			.containsExactly(tuple(152, "AVSLAG"), tuple(153, "BIFALL"), tuple(161, null));
		assertThat(types.get(1)).isEqualTo(new LifecareDecisionType(153, "bifall", "BIFALL", true, true));
		assertThat(LifecareDecisionMapper.toTypes(tree("{}"))).isEmpty();
	}

	@Test
	void listsTheOrsakerUnderTheHeadingEachSitsIn() {
		final var reasons = LifecareDecisionMapper.toReasons(tree("""
			[
			  {"header": "Arbetar deltid, ofrivilligt", "name": "Arbetar deltid, ofrivilligt", "reasonCode": null,
			   "options": [{"header": "", "name": "Arbetar deltid ofrivilligt, otillräcklig inkomst", "reasonCode": 19, "options": []}]},
			  {"header": "", "name": "Rubrik utan header", "reasonCode": null,
			   "options": [{"header": "", "name": "Underrubrik", "reasonCode": null,
			     "options": [{"header": "", "name": "Djup orsak", "reasonCode": 21, "options": []}]}]},
			  {"header": "Egen", "name": "Orsak med egen header", "reasonCode": 22,
			   "options": [{"header": "", "name": "Barn", "reasonCode": 23, "options": []}]}
			]"""));

		assertThat(reasons).containsExactly(
			new LifecareDecisionReason(19, "Arbetar deltid ofrivilligt, otillräcklig inkomst", "Arbetar deltid, ofrivilligt"),
			new LifecareDecisionReason(21, "Djup orsak", "Underrubrik"),
			new LifecareDecisionReason(22, "Orsak med egen header", "Egen"),
			new LifecareDecisionReason(23, "Barn", "Egen"));
	}
}
