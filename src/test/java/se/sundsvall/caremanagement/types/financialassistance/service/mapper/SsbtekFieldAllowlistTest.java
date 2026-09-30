package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsbtekFieldAllowlistTest {

	private static final String PERSONAL_NUMBER = "199001011234";

	@Test
	void keepsOnlyTheListedKeysAtEveryDepth() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.a.b", "fk.a.c.d", "so.x"));
		final var agencies = agencies(
			"fk", map("a", map("b", "kept", "c", map("d", 1, "personnummer", PERSONAL_NUMBER), "name", "Test Testsson"), "personnummer", PERSONAL_NUMBER),
			"so", map("x", "kept too", "y", "dropped"));

		assertThat(allowlist.retainIn(agencies)).isEqualTo(agencies(
			"fk", map("a", map("b", "kept", "c", map("d", 1))),
			"so", map("x", "kept too")));
	}

	@Test
	void leavesOutAnAgencyNoPathNames() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.a"));

		assertThat(allowlist.retainIn(agencies("af", map("a", "x"), "fk", map("a", "y"), "csn", map("a", "z"))))
			.containsOnlyKeys("fk");
	}

	@Test
	void treatsAListAsTheSameStepAsItsElementsAndKeepsElementsAndOrder() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.items.date", "fk.items.parts.amount"));
		final var agencies = agencies("fk", map("items", List.of(
			map("date", "2026-09-01", "personnummer", PERSONAL_NUMBER, "parts", List.of(map("amount", 1, "name", "a"), map("amount", 2, "name", "b"))),
			map("date", "2026-08-01", "parts", map("amount", 3, "name", "c")))));

		assertThat(allowlist.retainIn(agencies)).isEqualTo(agencies("fk", map("items", List.of(
			map("date", "2026-09-01", "parts", List.of(map("amount", 1), map("amount", 2))),
			map("date", "2026-08-01", "parts", map("amount", 3))))));
	}

	@Test
	void keepsALoneItemALoneItem() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("so.list.item.date"));

		assertThat(allowlist.retainIn(agencies("so", map("list", map("item", map("date", "2026-09-01", "name", "x"))))))
			.isEqualTo(agencies("so", map("list", map("item", map("date", "2026-09-01")))));
	}

	@Test
	void keepsAnObjectOnAnAllowedPathEvenWhenNothingInItIsAllowed() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.items.date"));

		assertThat(allowlist.retainIn(agencies("fk", map("items", List.of(map("personnummer", PERSONAL_NUMBER), map("date", "2026-09-01"))))))
			.isEqualTo(agencies("fk", map("items", List.of(map(), map("date", "2026-09-01")))));
	}

	@Test
	void keepsAPlainValueOnlyWhereAPathEnds() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.text", "fk.number", "fk.flag", "fk.parent.child"));

		assertThat(allowlist.retainIn(agencies("fk", map("text", "t", "number", 12.5, "flag", true, "parent", "plain where the path continues", "other", "dropped"))))
			.isEqualTo(agencies("fk", map("text", "t", "number", 12.5, "flag", true)));
	}

	@Test
	void dropsAnObjectOrListFoundWhereAPathEnds() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.date", "fk.dates", "fk.mixed"));

		assertThat(allowlist.retainIn(agencies("fk", map(
			"date", map("personnummer", PERSONAL_NUMBER),
			"dates", List.of("2026-09-01", "2026-09-02"),
			"mixed", List.of("2026-09-01", map("personnummer", PERSONAL_NUMBER), List.of(map("personnummer", PERSONAL_NUMBER)))))))
			.isEqualTo(agencies("fk", map(
				"dates", List.of("2026-09-01", "2026-09-02"),
				"mixed", List.of("2026-09-01", List.of()))));
	}

	@Test
	void letsANodeThroughAsTextAndAsObjectWhenBothAreListed() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.typ", "fk.typ.beskrivning"));

		assertThat(allowlist.retainIn(agencies("fk", map("typ", "Månad")))).isEqualTo(agencies("fk", map("typ", "Månad")));
		assertThat(allowlist.retainIn(agencies("fk", map("typ", map("id", 1, "beskrivning", "Månad")))))
			.isEqualTo(agencies("fk", map("typ", map("beskrivning", "Månad"))));
	}

	@Test
	void keepsANullOnAnAllowedPathAndDropsOneElsewhere() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.a", "fk.b.c"));
		final var answer = new HashMap<String, Object>();
		answer.put("a", null);
		answer.put("b", null);
		answer.put("other", null);

		assertThat(allowlist.retainIn(agencies("fk", answer))).isEqualTo(agencies("fk", mapWithNulls("a", "b")));
	}

	@Test
	void leavesOutAnAgencyWithoutAnAnswer() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.a", "so.a"));
		final var agencies = new HashMap<String, Map<String, Object>>();
		agencies.put("fk", null);
		agencies.put("so", map());

		assertThat(allowlist.retainIn(agencies)).isEqualTo(agencies("so", map()));
	}

	@Test
	void answersAnEmptyMapForNoAgencies() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.a"));

		assertThat(allowlist.retainIn(null)).isNotNull().isEmpty();
		assertThat(allowlist.retainIn(Map.of())).isNotNull().isEmpty();
	}

	@Test
	void answersNothingForAnEmptyAllowlist() {
		assertThat(SsbtekFieldAllowlist.of(List.of()).retainIn(agencies("fk", map("a", "x")))).isEmpty();
	}

	@Test
	void changesNeitherTheInputNorSharesStructureWithIt() {
		final var allowlist = SsbtekFieldAllowlist.of(List.of("fk.items.date"));
		final var items = new ArrayList<Object>(List.of(map("date", "2026-09-01", "personnummer", PERSONAL_NUMBER)));
		final var agencies = agencies("fk", map("items", items, "personnummer", PERSONAL_NUMBER));

		final var result = allowlist.retainIn(agencies);
		result.get("fk").put("added", "later");

		assertThat(agencies.get("fk")).containsOnlyKeys("items", "personnummer");
		assertThat(items).containsExactly(map("date", "2026-09-01", "personnummer", PERSONAL_NUMBER));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"", " ", "fk", "fk.", ".fk", "fk..a", "fk. .a"
	})
	void refusesAPathThatIsNotAnAgencyAndAtLeastOneField(final String path) {
		final var paths = List.of(path);

		assertThatThrownBy(() -> SsbtekFieldAllowlist.of(paths))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("allowed path");
	}

	@Test
	void refusesANullPath() {
		final var paths = Collections.<String>singletonList(null);

		assertThatThrownBy(() -> SsbtekFieldAllowlist.of(paths))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("allowed path");
	}

	private static Map<String, Map<String, Object>> agencies(final Object... agencyAndAnswer) {
		final var agencies = new LinkedHashMap<String, Map<String, Object>>();
		for (var i = 0; i < agencyAndAnswer.length; i += 2) {
			@SuppressWarnings("unchecked")
			final var answer = (Map<String, Object>) agencyAndAnswer[i + 1];
			agencies.put((String) agencyAndAnswer[i], answer);
		}
		return agencies;
	}

	private static Map<String, Object> map(final Object... keyAndValue) {
		final var map = new LinkedHashMap<String, Object>();
		for (var i = 0; i < keyAndValue.length; i += 2) {
			map.put((String) keyAndValue[i], keyAndValue[i + 1]);
		}
		return map;
	}

	private static Map<String, Object> mapWithNulls(final String... keys) {
		final var map = new LinkedHashMap<String, Object>();
		for (final var key : keys) {
			map.put(key, null);
		}
		return map;
	}
}
