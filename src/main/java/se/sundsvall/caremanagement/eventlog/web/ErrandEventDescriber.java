package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import java.util.Map;

/**
 * Turns an errand-scoped request into a plain-language (Swedish) description of what happened — e.g. "Öppnade ärendet",
 * "Lade till inkomst i utkastberäkningen", "Uppdaterade beräkningshuvud", "Visade beslut". Derived generically from the
 * HTTP method and the path, so it reads well across every endpoint without per-endpoint wiring. The structured
 * {@code action}/{@code target} fields stay machine-friendly (English) for filtering; this only shapes the human
 * {@code description}.
 *
 * <p>
 * {@code tail} is the list of non-id, non-blank path segments after the errand id (e.g. {@code [calculation, draft,
 * incomes]}); {@code itemLevel} is true when the request targeted a specific sub-item (an id segment was present).
 */
final class ErrandEventDescriber {

	// leaf path segment -> { singular, plural } Swedish nouns
	private static final Map<String, String[]> NOUNS = Map.ofEntries(
		Map.entry("decisions", new String[] {
			"beslut", "beslut"
		}),
		Map.entry("notes", new String[] {
			"anteckning", "anteckningar"
		}),
		Map.entry("attachments", new String[] {
			"bilaga", "bilagor"
		}),
		Map.entry("stakeholders", new String[] {
			"intressent", "intressenter"
		}),
		Map.entry("documents", new String[] {
			"dokument", "dokument"
		}),
		Map.entry("journal-entries", new String[] {
			"journalanteckning", "journalanteckningar"
		}),
		Map.entry("messages", new String[] {
			"meddelande", "meddelanden"
		}),
		Map.entry("process-messages", new String[] {
			"processmeddelande", "processmeddelanden"
		}),
		Map.entry("parameters", new String[] {
			"parameter", "parametrar"
		}),
		Map.entry("permits", new String[] {
			"tillstånd", "tillstånd"
		}),
		Map.entry("referrals", new String[] {
			"remiss", "remisser"
		}),
		Map.entry("notifications", new String[] {
			"notis", "notiser"
		}),
		Map.entry("incomes", new String[] {
			"inkomst", "inkomster"
		}),
		Map.entry("expenses", new String[] {
			"utgift", "utgifter"
		}),
		Map.entry("persons", new String[] {
			"person", "personer"
		}),
		Map.entry("warnings", new String[] {
			"varning", "varningar"
		}),
		Map.entry("monitorings", new String[] {
			"bevakning", "bevakningar"
		}),
		Map.entry("sections", new String[] {
			"sektion", "sektioner"
		}),
		Map.entry("status-history", new String[] {
			"statushistorik", "statushistorik"
		}),
		Map.entry("data", new String[] {
			"ärendeuppgifter", "ärendeuppgifter"
		}),
		Map.entry("header", new String[] {
			"beräkningshuvud", "beräkningshuvud"
		}),
		Map.entry("draft", new String[] {
			"utkastberäkning", "utkastberäkning"
		}));

	private ErrandEventDescriber() {}

	static String describe(final String method, final List<String> tail, final boolean itemLevel) {
		if (tail.isEmpty()) {
			return switch (method) {
				case "GET" -> "Öppnade ärendet";
				case "POST" -> "Skapade ärendet";
				case "PUT", "PATCH" -> "Uppdaterade ärendet";
				case "DELETE" -> "Tog bort ärendet";
				default -> "Ärende";
			};
		}

		final var leaf = tail.get(tail.size() - 1);

		if ("restore".equals(leaf)) {
			final var resource = tail.size() >= 2 ? tail.get(tail.size() - 2) : "rad";
			return "Återställde " + singular(resource) + calculationContext(tail, resource);
		}
		if ("approval".equals(leaf) || "approvals".equals(leaf)) {
			return "GET".equals(method) ? "Visade sektionsgodkännanden" : "Godkände en sektion";
		}
		if ("acknowledged".equals(leaf)) {
			return "Kvitterade notiser";
		}

		final var noun = "GET".equals(method) && !itemLevel ? plural(leaf) : singular(leaf);
		return verb(method) + " " + noun + calculationContext(tail, leaf);
	}

	private static String verb(final String method) {
		return switch (method) {
			case "GET" -> "Visade";
			case "POST" -> "Lade till";
			case "PUT", "PATCH" -> "Uppdaterade";
			case "DELETE" -> "Tog bort";
			default -> method;
		};
	}

	private static String calculationContext(final List<String> tail, final String resource) {
		final var rowType = "incomes".equals(resource) || "expenses".equals(resource) || "persons".equals(resource);
		return tail.contains("calculation") && rowType ? " i utkastberäkningen" : "";
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
