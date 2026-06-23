package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandEventDescriberTest {

	@Test
	void errandRoot() {
		assertThat(ErrandEventDescriber.describe("GET", List.of(), false)).isEqualTo("Opened errand");
		assertThat(ErrandEventDescriber.describe("POST", List.of(), false)).isEqualTo("Created errand");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of(), false)).isEqualTo("Updated errand");
		assertThat(ErrandEventDescriber.describe("PUT", List.of(), false)).isEqualTo("Updated errand");
		assertThat(ErrandEventDescriber.describe("DELETE", List.of(), false)).isEqualTo("Deleted errand");
		assertThat(ErrandEventDescriber.describe("HEAD", List.of(), false)).isEqualTo("Errand");
	}

	@Test
	void collectionVsItemReads() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("decisions"), false)).isEqualTo("Viewed decisions");
		assertThat(ErrandEventDescriber.describe("GET", List.of("decisions"), true)).isEqualTo("Viewed decision");
	}

	@Test
	void writesUseSingular() {
		assertThat(ErrandEventDescriber.describe("POST", List.of("decisions"), false)).isEqualTo("Added decision");
		assertThat(ErrandEventDescriber.describe("POST", List.of("notes"), false)).isEqualTo("Added note");
		assertThat(ErrandEventDescriber.describe("POST", List.of("attachments"), false)).isEqualTo("Added attachment");
		assertThat(ErrandEventDescriber.describe("DELETE", List.of("attachments"), true)).isEqualTo("Deleted attachment");
		assertThat(ErrandEventDescriber.describe("GET", List.of("journal-entries"), false)).isEqualTo("Viewed journal entries");
	}

	@Test
	void calculationRows() {
		final var incomes = List.of("calculation", "draft", "incomes");
		assertThat(ErrandEventDescriber.describe("POST", incomes, false)).isEqualTo("Added income in the draft calculation");
		assertThat(ErrandEventDescriber.describe("PATCH", incomes, true)).isEqualTo("Updated income in the draft calculation");
		assertThat(ErrandEventDescriber.describe("DELETE", incomes, true)).isEqualTo("Deleted income in the draft calculation");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("calculation", "draft", "expenses"), true)).isEqualTo("Updated expense in the draft calculation");
	}

	@Test
	void calculationHeaderAndDraftAndData() {
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("calculation", "draft", "header"), false)).isEqualTo("Updated calculation header");
		assertThat(ErrandEventDescriber.describe("GET", List.of("calculation", "draft"), false)).isEqualTo("Viewed draft calculation");
		assertThat(ErrandEventDescriber.describe("GET", List.of("data"), false)).isEqualTo("Viewed case data");
	}

	@Test
	void actionStyleLeaves() {
		assertThat(ErrandEventDescriber.describe("POST", List.of("calculation", "draft", "incomes", "restore"), false)).isEqualTo("Restored income in the draft calculation");
		assertThat(ErrandEventDescriber.describe("POST", List.of("sections", "INKOMSTER", "approval"), false)).isEqualTo("Approved a section");
		assertThat(ErrandEventDescriber.describe("GET", List.of("sections", "approvals"), false)).isEqualTo("Viewed section approvals");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("notifications", "acknowledged"), false)).isEqualTo("Acknowledged notifications");
	}

	@Test
	void statusHistoryIsUncountable() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("status-history"), false)).isEqualTo("Viewed status history");
	}

	@Test
	void unknownLeafFallsBackToHumanizedName() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("widgets"), false)).isEqualTo("Viewed widgets");
		assertThat(ErrandEventDescriber.describe("POST", List.of("widgets"), false)).isEqualTo("Added widget");
	}
}
