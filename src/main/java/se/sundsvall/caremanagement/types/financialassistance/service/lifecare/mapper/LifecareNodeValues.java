package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.Optional;
import tools.jackson.databind.JsonNode;

/**
 * Reads single values out of Lifecare's JSON the way its own JavaScript client does: loosely typed, with ids sometimes
 * written as strings and flags that count when they are set to anything but an empty value.
 */
public final class LifecareNodeValues {

	private static final String INTEGER_PATTERN = "-?\\d{1,9}";

	private LifecareNodeValues() {}

	/**
	 * A string value.
	 *
	 * @param  node the node
	 * @return      the string, or null when the node is not a string
	 */
	public static String text(final JsonNode node) {
		if (node != null && node.isString()) {
			return node.stringValue();
		}
		return null;
	}

	/**
	 * A string value, or the empty string when the node is not a string.
	 *
	 * @param  node the node
	 * @return      the string
	 */
	public static String textOrEmpty(final JsonNode node) {
		return Optional.ofNullable(text(node)).orElse("");
	}

	/**
	 * An integer value, from a number or a numeric string.
	 *
	 * @param  node the node
	 * @return      the integer, or null when the node holds none
	 */
	public static Integer integer(final JsonNode node) {
		if (node == null) {
			return null;
		}
		if (node.isNumber() && node.canConvertToInt()) {
			return node.intValue();
		}
		if (node.isString() && node.stringValue().matches(INTEGER_PATTERN)) {
			return Integer.valueOf(node.stringValue());
		}
		return null;
	}

	/**
	 * Whether a flag is set, as JavaScript reads it: a missing, null, false, zero or empty-string value is not set,
	 * anything else is.
	 *
	 * @param  node the node
	 * @return      true when set
	 */
	public static boolean truthy(final JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return false;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isString()) {
			return !node.stringValue().isEmpty();
		}
		if (node.isNumber()) {
			return node.doubleValue() != 0;
		}
		return true;
	}

	/**
	 * Whether a flag is exactly true.
	 *
	 * @param  node the node
	 * @return      true only for a JSON true
	 */
	public static boolean isTrue(final JsonNode node) {
		return node != null && node.isBoolean() && node.booleanValue();
	}
}
