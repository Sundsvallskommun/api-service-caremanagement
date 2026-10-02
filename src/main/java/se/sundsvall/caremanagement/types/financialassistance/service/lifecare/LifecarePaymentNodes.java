package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

/**
 * Small readers over the JSON trees Lifecare's payment and jobbstimulans endpoints answer with.
 *
 * <p>
 * Lifecare's objects are read field by field rather than bound to types, because every write sends Lifecare's own
 * object back with a few fields changed. A field that is absent or JSON null reads as null throughout.
 * </p>
 */
final class LifecarePaymentNodes {

	/** Lifecare's businessType for: the businessId is an insats. */
	static final String SERVICE_BUSINESS_TYPE = "8";

	/** Lifecare's sentinel payee Adress (Int32.MaxValue - 1): pays to the client's registered address. */
	static final int ADDRESS_PAYEE_ID = 2147483646;

	static final JsonNodeFactory NODES = JsonNodeFactory.instance;

	private static final Pattern NON_DIGITS = Pattern.compile("\\D");
	private static final Pattern LIFECARE_MONTH = Pattern.compile("^\\d{6}$");

	private LifecarePaymentNodes() {}

	static JsonNode field(final JsonNode node, final String name) {
		if (node == null) {
			return null;
		}
		final var value = node.get(name);
		if (value == null || value.isNull() || value.isMissingNode()) {
			return null;
		}
		return value;
	}

	static String text(final JsonNode node, final String name) {
		return Optional.ofNullable(field(node, name)).map(JsonNode::asString).orElse(null);
	}

	static String textOrEmpty(final JsonNode node, final String name) {
		return Objects.toString(text(node, name), "");
	}

	static Integer integer(final JsonNode node, final String name) {
		return Optional.ofNullable(field(node, name)).filter(JsonNode::isNumber).map(JsonNode::intValue).orElse(null);
	}

	static BigDecimal decimal(final JsonNode node, final String name) {
		return Optional.ofNullable(field(node, name)).filter(JsonNode::isNumber).map(JsonNode::decimalValue).orElse(null);
	}

	static boolean flag(final JsonNode node, final String name) {
		return Optional.ofNullable(field(node, name)).filter(JsonNode::isBoolean).map(JsonNode::asBoolean).orElse(false);
	}

	static List<JsonNode> array(final JsonNode node, final String name) {
		return Optional.ofNullable(field(node, name)).filter(JsonNode::isArray).map(value -> List.copyOf(value.values())).orElse(List.of());
	}

	static List<JsonNode> elements(final JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		return List.copyOf(node.values());
	}

	static String digitsOnly(final String value) {
		if (value == null) {
			return "";
		}
		return NON_DIGITS.matcher(value).replaceAll("");
	}

	/** Makulerad: Lifecare leaves cancellationDate empty until an utbetalning is. */
	static boolean cancelled(final JsonNode payment) {
		return StringUtils.hasLength(text(payment, "cancellationDate"));
	}

	/** Lifecare's yyyyMM as yyyy-MM; anything else is passed on as it came. */
	static String toMonth(final String concernedMonth) {
		if (concernedMonth != null && LIFECARE_MONTH.matcher(concernedMonth).matches()) {
			return concernedMonth.substring(0, 4) + "-" + concernedMonth.substring(4);
		}
		return Objects.toString(concernedMonth, "");
	}

	/** An amount as a caseworker reads it: 3 rather than 3.00. */
	static String plain(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	static ObjectNode copyOf(final JsonNode node) {
		if (node instanceof final ObjectNode object) {
			return object.deepCopy();
		}
		return NODES.objectNode();
	}

	static ThrowableProblem refuse(final String reason) {
		return Problem.valueOf(UNPROCESSABLE_CONTENT, reason);
	}
}
