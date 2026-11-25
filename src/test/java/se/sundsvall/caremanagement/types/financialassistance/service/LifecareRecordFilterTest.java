package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareRecordFilterTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final LifecareRecordFilter filter = new LifecareRecordFilter(Set.of("EK"));

	@ParameterizedTest
	@ValueSource(strings = {
		"EK Ekonomiskt bistånd", "EK Återansökan Digital Ekonomiskt bistånd", "EK Nyansökan Fysisk Ekonomiskt bistånd", "ek Ekonomiskt bistånd",
		"  EK Ekonomiskt bistånd", "EK"
	})
	void isFinancialAssistanceForAnEkonomisktBistandOwner(final String ownerType) {
		assertThat(filter.isFinancialAssistance(row(ownerType))).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		"Vux LVM-utredning § 7", "Vux Utredning 14 kap 2 § SoL", "Vux - Anmälan LVM", "BoU Avgift föräldrar", "EKO Ekonomi", "Ekonomiskt bistånd", "   "
	})
	void isNotFinancialAssistanceForAnyOtherOrUnnamedOwner(final String ownerType) {
		assertThat(filter.isFinancialAssistance(row(ownerType))).isFalse();
	}

	@Test
	void isNotFinancialAssistanceWithoutTheField() {
		assertThat(filter.isFinancialAssistance(JSON.readTree("{ \"id\": 1 }"))).isFalse();
	}

	@Test
	void theAreasAreConfigurable() {
		final var configured = new LifecareRecordFilter(Set.of(" ek ", "Vux", ""));

		assertThat(configured.isFinancialAssistance(row("EK Ekonomiskt bistånd"))).isTrue();
		assertThat(configured.isFinancialAssistance(row("Vux LVM-utredning § 7"))).isTrue();
		assertThat(configured.isFinancialAssistance(row("BoU Avgift föräldrar"))).isFalse();
		assertThat(configured.isFinancialAssistance(row(""))).isFalse();
	}

	@Test
	void financialAssistanceOnlyKeepsTheEbRowsInOrderAndTheRestOfTheAnswer() {
		final var list = JSON.readTree("""
			{ "documentModels": [
			  { "id": 1, "ownerTypeText": "EK Ekonomiskt bistånd" },
			  { "id": 2, "ownerTypeText": "Vux LVM-utredning § 7" },
			  { "id": 3, "ownerTypeText": "EK Återansökan Digital Ekonomiskt bistånd" },
			  { "id": 4 } ],
			  "total": 4 }
			""");

		final var filtered = filter.financialAssistanceOnly(list);

		assertThat(filtered.path("documentModels").valueStream().map(row -> row.path("id").intValue())).containsExactly(1, 3);
		assertThat(filtered.path("total").intValue()).isEqualTo(4);
		// The answer Lifecare gave is left as it was.
		assertThat(list.path("documentModels").size()).isEqualTo(4);
	}

	@Test
	void financialAssistanceOnlyOnAnAnswerWithoutRows() {
		assertThat(filter.financialAssistanceOnly(JSON.readTree("{}")).path("documentModels").isEmpty()).isTrue();
		assertThat(filter.financialAssistanceOnly(JSON.readTree("[]")).path("documentModels").isEmpty()).isTrue();
		assertThat(filter.financialAssistanceOnly(null).path("documentModels").isEmpty()).isTrue();
	}

	private static JsonNode row(final String ownerType) {
		final var row = JsonNodeFactory.instance.objectNode().put("id", 1);
		if (ownerType != null) {
			row.put("ownerTypeText", ownerType);
		}
		return row;
	}
}
