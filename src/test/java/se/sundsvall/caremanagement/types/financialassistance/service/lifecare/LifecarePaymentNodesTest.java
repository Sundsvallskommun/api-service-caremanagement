package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;

class LifecarePaymentNodesTest {

	private static final String NODE = """
		{ "text": "a", "number": 3.50, "flag": true, "textFlag": "true", "nothing": null, "list": [1, 2], "object": { "x": 1 } }
		""";

	@Test
	void readsAbsentAndNullFieldsAsNull() {
		final var node = json(NODE);

		assertThat(LifecarePaymentNodes.field(null, "text")).isNull();
		assertThat(LifecarePaymentNodes.text(node, "nothing")).isNull();
		assertThat(LifecarePaymentNodes.text(node, "missing")).isNull();
		assertThat(LifecarePaymentNodes.textOrEmpty(node, "missing")).isEmpty();
		assertThat(LifecarePaymentNodes.text(node, "text")).isEqualTo("a");
		assertThat(LifecarePaymentNodes.integer(node, "text")).isNull();
		assertThat(LifecarePaymentNodes.decimal(node, "number")).isEqualByComparingTo("3.5");
		assertThat(LifecarePaymentNodes.flag(node, "flag")).isTrue();
		assertThat(LifecarePaymentNodes.flag(node, "textFlag")).isFalse();
		assertThat(LifecarePaymentNodes.array(node, "object")).isEmpty();
		assertThat(LifecarePaymentNodes.array(node, "list")).hasSize(2);
		assertThat(LifecarePaymentNodes.elements(null)).isEmpty();
		assertThat(LifecarePaymentNodes.copyOf(node.get("list")).size()).isZero();
	}

	@Test
	void normalisesAccountsMonthsAndAmounts() {
		assertThat(LifecarePaymentNodes.digitsOnly(null)).isEmpty();
		assertThat(LifecarePaymentNodes.digitsOnly("1111-1111 ")).isEqualTo("11111111");
		assertThat(LifecarePaymentNodes.toMonth("202609")).isEqualTo("2026-09");
		assertThat(LifecarePaymentNodes.toMonth("2026-09")).isEqualTo("2026-09");
		assertThat(LifecarePaymentNodes.toMonth(null)).isEmpty();
		assertThat(LifecarePaymentNodes.plain(new BigDecimal("3.00"))).isEqualTo("3");
		assertThat(LifecarePaymentNodes.plain(new BigDecimal("1E+3"))).isEqualTo("1000");
		assertThat(LifecarePaymentNodes.cancelled(json("{ \"cancellationDate\": \"\" }"))).isFalse();
		assertThat(LifecarePaymentNodes.cancelled(json("{ \"cancellationDate\": \"2026-09-22\" }"))).isTrue();
	}
}
