package se.sundsvall.caremanagement.journal.api;

import java.time.OffsetDateTime;
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
import se.sundsvall.caremanagement.journal.service.JournalEntryService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class JournalEntryResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/journal-entries";
	private static final OffsetDateTime ENTRY_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@MockitoBean
	private JournalEntryService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addWithBlankType() {
		post(new CreateJournalEntry(" ", "Rubrik", "body", ENTRY_DATE_TIME, "carola"));
	}

	@Test
	void addWithBlankHeading() {
		post(new CreateJournalEntry("Journalfört meddelande", " ", "body", ENTRY_DATE_TIME, "carola"));
	}

	@Test
	void addWithMissingEntryDateTime() {
		post(new CreateJournalEntry("Journalfört meddelande", "Rubrik", "body", null, "carola"));
	}

	@Test
	void addWithInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(new CreateJournalEntry("Journalfört meddelande", "Rubrik", "body", ENTRY_DATE_TIME, "carola"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "invalid", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateJournalEntry("Journalfört meddelande", "Rubrik", "body", ENTRY_DATE_TIME, "carola"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	private void post(final CreateJournalEntry request) {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
