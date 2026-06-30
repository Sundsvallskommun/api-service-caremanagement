package se.sundsvall.caremanagement.journal.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.journal.api.model.CreateJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.JournalEntry;
import se.sundsvall.caremanagement.journal.api.model.LockJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.UpdateJournalEntry;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class JournalEntryResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String JOURNAL_ENTRY_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/journal-entries";
	private static final LocalDate ENTRY_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime ENTRY_TIME = LocalTime.of(14, 30);

	@MockitoBean
	private JournalEntryService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void add() {
		final var request = new CreateJournalEntry("Journalfört meddelande", "Rubrik", "body", ENTRY_DATE, ENTRY_TIME, "carola");
		when(serviceMock.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(JOURNAL_ENTRY_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/journal-entries/" + JOURNAL_ENTRY_ID);

		verify(serviceMock).add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}

	@Test
	void list() {
		when(serviceMock.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(JournalEntry.create().withId("je1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(JournalEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void read() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID)).thenReturn(JournalEntry.create().withId(JOURNAL_ENTRY_ID).withHeading("H"));

		final var entry = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{journalEntryId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "journalEntryId", JOURNAL_ENTRY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(JournalEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(entry).isNotNull();
		assertThat(entry.getId()).isEqualTo(JOURNAL_ENTRY_ID);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID);
	}

	@Test
	void update() {
		final var request = new UpdateJournalEntry("Journalfört meddelande", "Ny rubrik", "updated", ENTRY_DATE, ENTRY_TIME, "editor");
		when(serviceMock.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, request)).thenReturn(JournalEntry.create().withId(JOURNAL_ENTRY_ID).withHeading("Ny rubrik").withModifiedBy("editor"));

		final var entry = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{journalEntryId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "journalEntryId", JOURNAL_ENTRY_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(JournalEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(entry).isNotNull();
		assertThat(entry.getHeading()).isEqualTo("Ny rubrik");
		assertThat(entry.getModifiedBy()).isEqualTo("editor");
		verify(serviceMock).update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, request);
	}

	@Test
	void lock() {
		final var request = new LockJournalEntry("carola");
		when(serviceMock.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, request)).thenReturn(JournalEntry.create().withId(JOURNAL_ENTRY_ID).withStatus("LOCKED").withLockedBy("carola"));

		final var entry = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/{journalEntryId}/lock").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "journalEntryId", JOURNAL_ENTRY_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(JournalEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(entry).isNotNull();
		assertThat(entry.getStatus()).isEqualTo("LOCKED");
		assertThat(entry.getLockedBy()).isEqualTo("carola");
		verify(serviceMock).lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, request);
	}

	@Test
	void lockWithoutBody() {
		when(serviceMock.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, null)).thenReturn(JournalEntry.create().withId(JOURNAL_ENTRY_ID).withStatus("LOCKED"));

		final var entry = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/{journalEntryId}/lock").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "journalEntryId", JOURNAL_ENTRY_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(JournalEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(entry).isNotNull();
		assertThat(entry.getStatus()).isEqualTo("LOCKED");
		verify(serviceMock).lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID, null);
	}

	@Test
	void delete() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{journalEntryId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "journalEntryId", JOURNAL_ENTRY_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, JOURNAL_ENTRY_ID);
	}
}
