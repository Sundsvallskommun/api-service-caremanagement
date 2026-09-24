package se.sundsvall.caremanagement.eventlog.web;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandEventDescriberTest {

	@Test
	void errandRoot() {
		assertThat(ErrandEventDescriber.describe("GET", List.of(), false)).isEqualTo("Öppnade ärendet");
		assertThat(ErrandEventDescriber.describe("POST", List.of(), false)).isEqualTo("Skapade ärendet");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of(), false)).isEqualTo("Uppdaterade ärendet");
		assertThat(ErrandEventDescriber.describe("PUT", List.of(), false)).isEqualTo("Uppdaterade ärendet");
		assertThat(ErrandEventDescriber.describe("DELETE", List.of(), false)).isEqualTo("Tog bort ärendet");
		assertThat(ErrandEventDescriber.describe("HEAD", List.of(), false)).isEqualTo("Ärende");
	}

	@Test
	void collectionVsItemReads() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("decisions"), false)).isEqualTo("Visade beslut");
		assertThat(ErrandEventDescriber.describe("GET", List.of("decisions"), true)).isEqualTo("Visade beslut");
	}

	@Test
	void writesUseSingular() {
		assertThat(ErrandEventDescriber.describe("POST", List.of("decisions"), false)).isEqualTo("Lade till beslut");
		assertThat(ErrandEventDescriber.describe("POST", List.of("notes"), false)).isEqualTo("Lade till anteckning");
		assertThat(ErrandEventDescriber.describe("POST", List.of("attachments"), false)).isEqualTo("Lade till bilaga");
		assertThat(ErrandEventDescriber.describe("DELETE", List.of("attachments"), true)).isEqualTo("Tog bort bilaga");
		assertThat(ErrandEventDescriber.describe("GET", List.of("journal-entries"), false)).isEqualTo("Visade journalanteckningar");
	}

	@Test
	void calculationRows() {
		final var incomes = List.of("calculation", "draft", "incomes");
		assertThat(ErrandEventDescriber.describe("POST", incomes, false)).isEqualTo("Lade till inkomst i utkastberäkningen");
		assertThat(ErrandEventDescriber.describe("PATCH", incomes, true)).isEqualTo("Uppdaterade inkomst i utkastberäkningen");
		assertThat(ErrandEventDescriber.describe("DELETE", incomes, true)).isEqualTo("Tog bort inkomst i utkastberäkningen");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("calculation", "draft", "expenses"), true)).isEqualTo("Uppdaterade utgift i utkastberäkningen");
	}

	@Test
	void calculationHeaderAndDraftAndData() {
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("calculation", "draft", "header"), false)).isEqualTo("Uppdaterade beräkningshuvud");
		assertThat(ErrandEventDescriber.describe("GET", List.of("calculation", "draft"), false)).isEqualTo("Visade utkastberäkning");
		// GUI-01: the manual SSBTEK read, logged on the errand
		assertThat(ErrandEventDescriber.describe("GET", List.of("ssbtek"), false)).isEqualTo("Visade SSBTEK-underlag");
		// The process's read of the household's personal numbers is a disclosure, logged in words
		assertThat(ErrandEventDescriber.describe("GET", List.of("household-identifiers"), false)).isEqualTo("Visade hushållets personnummer");
		assertThat(ErrandEventDescriber.describe("GET", List.of("data"), false)).isEqualTo("Visade ärendeuppgifter");
	}

	@Test
	void actionStyleLeaves() {
		assertThat(ErrandEventDescriber.describe("POST", List.of("calculation", "draft", "incomes", "restore"), false)).isEqualTo("Återställde inkomst i utkastberäkningen");
		assertThat(ErrandEventDescriber.describe("PATCH", List.of("notifications", "acknowledged"), false)).isEqualTo("Kvitterade notiser");
	}

	@Test
	void statusHistoryIsUncountable() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("status-history"), false)).isEqualTo("Visade statushistorik");
	}

	@Test
	void unknownLeafFallsBackToHumanizedName() {
		assertThat(ErrandEventDescriber.describe("GET", List.of("widgets"), false)).isEqualTo("Visade widgets");
		assertThat(ErrandEventDescriber.describe("POST", List.of("widgets"), false)).isEqualTo("Lade till widget");
	}
}
