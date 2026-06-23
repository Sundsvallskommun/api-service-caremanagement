package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import java.util.Map;

/**
 * Turns an errand-scoped request into a plain-language description of what happened — e.g. "Opened errand",
 * "Added income in the draft calculation", "Updated calculation header", "Viewed decisions". Derived generically from
 * the HTTP method and the path, so it reads well across every endpoint without per-endpoint wiring. The structured
 * {@code action}/{@code target} fields stay machine-friendly for filtering; this only shapes the human
 * {@code description}.
 *
 * <p>
 * {@code tail} is the list of non-id, non-blank path segments after the errand id (e.g. {@code [calculation, draft,
 * incomes]}); {@code itemLevel} is true when the request targeted a specific sub-item (an id segment was present).
 */
final class ErrandEventDescriber {

	// leaf path segment -> { singular, plural } human nouns
	private static final Map<String, String[]> NOUNS = Map.ofEntries(
		Map.entry("decisions", new String[] {
			"decision", "decisions"
		}),
		Map.entry("notes", new String[] {
			"note", "notes"
		}),
		Map.entry("attachments", new String[] {
			"attachment", "attachments"
		}),
		Map.entry("stakeholders", new String[] {
			"stakeholder", "stakeholders"
		}),
		Map.entry("documents", new String[] {
			"document", "documents"
		}),
		Map.entry("journal-entries", new String[] {
			"journal entry", "journal entries"
		}),
		Map.entry("messages", new String[] {
			"message", "messages"
		}),
		Map.entry("process-messages", new String[] {
			"process message", "process messages"
		}),
		Map.entry("parameters", new String[] {
			"parameter", "parameters"
		}),
		Map.entry("permits", new String[] {
			"permit", "permits"
		}),
		Map.entry("referrals", new String[] {
			"referral", "referrals"
		}),
		Map.entry("notifications", new String[] {
			"notification", "notifications"
		}),
		Map.entry("incomes", new String[] {
			"income", "incomes"
		}),
		Map.entry("expenses", new String[] {
			"expense", "expenses"
		}),
		Map.entry("persons", new String[] {
			"person", "persons"
		}),
		Map.entry("warnings", new String[] {
			"warning", "warnings"
		}),
		Map.entry("monitorings", new String[] {
			"monitoring", "monitorings"
		}),
		Map.entry("sections", new String[] {
			"section", "sections"
		}),
		Map.entry("status-history", new String[] {
			"status history", "status history"
		}),
		Map.entry("data", new String[] {
			"case data", "case data"
		}),
		Map.entry("header", new String[] {
			"calculation header", "calculation header"
		}),
		Map.entry("draft", new String[] {
			"draft calculation", "draft calculation"
		}));

	private ErrandEventDescriber() {}

	static String describe(final String method, final List<String> tail, final boolean itemLevel) {
		if (tail.isEmpty()) {
			return switch (method) {
				case "GET" -> "Opened errand";
				case "POST" -> "Created errand";
				case "PUT", "PATCH" -> "Updated errand";
				case "DELETE" -> "Deleted errand";
				default -> "Errand";
			};
		}

		final var leaf = tail.get(tail.size() - 1);

		if ("restore".equals(leaf)) {
			final var resource = tail.size() >= 2 ? tail.get(tail.size() - 2) : "row";
			return "Restored " + singular(resource) + calculationContext(tail, resource);
		}
		if ("approval".equals(leaf) || "approvals".equals(leaf)) {
			return "GET".equals(method) ? "Viewed section approvals" : "Approved a section";
		}
		if ("acknowledged".equals(leaf)) {
			return "Acknowledged notifications";
		}

		final var noun = "GET".equals(method) && !itemLevel ? plural(leaf) : singular(leaf);
		return verb(method) + " " + noun + calculationContext(tail, leaf);
	}

	private static String verb(final String method) {
		return switch (method) {
			case "GET" -> "Viewed";
			case "POST" -> "Added";
			case "PUT", "PATCH" -> "Updated";
			case "DELETE" -> "Deleted";
			default -> method;
		};
	}

	private static String calculationContext(final List<String> tail, final String resource) {
		final var rowType = "incomes".equals(resource) || "expenses".equals(resource) || "persons".equals(resource);
		return tail.contains("calculation") && rowType ? " in the draft calculation" : "";
	}

	private static String singular(final String leaf) {
		final var mapped = NOUNS.get(leaf);
		if (mapped != null) {
			return mapped[0];
		}
		final var humanized = humanize(leaf);
		return humanized.endsWith("s") ? humanized.substring(0, humanized.length() - 1) : humanized;
	}

	private static String plural(final String leaf) {
		final var mapped = NOUNS.get(leaf);
		return mapped != null ? mapped[1] : humanize(leaf);
	}

	private static String humanize(final String leaf) {
		return leaf.replace('-', ' ');
	}
}
