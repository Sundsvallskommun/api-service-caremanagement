package se.sundsvall.caremanagement.decisions.service;

import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * Builds the Swedish, human-readable description of the notification raised when a decision is recorded. The
 * notification is shown to caseworkers in Draken as is, so the stored decision codes are translated here; a type or
 * value without a translation is shown as the raw code.
 */
final class DecisionNotificationText {

	private static final Map<String, String> TYPE_LABELS = Map.of(
		"PAYMENT", "Utbetalningsbeslut",
		"RECOMMENDATION", "Rekommendation",
		"ACTUALISATION", "Aktualisering");

	private static final Map<String, String> VALUE_LABELS = Map.of(
		"BIFALL", "bifall",
		"DELAVSLAG", "delvis bifall",
		"AVSLAG", "avslag",
		"APPROVED", "beviljat",
		"REJECTED", "avslag",
		"OK", "inga anmärkningar",
		"REVIEW_REQUIRED", "kräver granskning");

	private DecisionNotificationText() {}

	static String describe(final String decisionType, final String value) {
		final var typeLabel = TYPE_LABELS.getOrDefault(decisionType, decisionType);
		if (!hasText(value)) {
			return typeLabel;
		}
		return typeLabel + ": " + VALUE_LABELS.getOrDefault(value, value);
	}
}
