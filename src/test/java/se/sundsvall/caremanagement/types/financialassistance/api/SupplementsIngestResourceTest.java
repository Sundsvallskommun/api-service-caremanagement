package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareSupplements;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestOutcome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestResult;
import se.sundsvall.caremanagement.types.financialassistance.service.SupplementsIngestService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class SupplementsIngestResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare-supplements";

	@MockitoBean
	private SupplementsIngestService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void deliverSupplementsReturnsReceipt() {
		when(serviceMock.ingest(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(LifecareSupplements.class)))
			.thenReturn(new SupplementsIngestResult(List.of(
				new SupplementsIngestOutcome("documents", "27", "CREATED", null),
				new SupplementsIngestOutcome("jobStimulus", null, "REPLACED", "2 period(s)"))));

		// A near-raw robot dump: unknown fields present, codes as numbers, journal note as a documents[] row.
		final var body = """
			{
			  "capturedAt": "2026-08-25",
			  "documents": [
			    {
			      "id": 27,
			      "documentId": null,
			      "title": "Journalanteckning",
			      "date": "2026-08-05",
			      "time": "11:06",
			      "typeCode": 1,
			      "documentType": 3,
			      "content": "<p>Hej!</p>",
			      "updateSignature": "RPA_031DEV",
			      "documentType_Name": "JournalNote",
			      "someUnknownField": true
			    }
			  ],
			  "jobStimulus": {
			    "applicant": { "personId": "19800101T001", "periods": [ { "jobStimulusId": 89, "fromDate": "2021-01-01", "toDate": "2021-12-31", "markedForRemoval": false } ] },
			    "coApplicant": null,
			    "hasCoApplicant": false
			  }
			}
			""";

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(body)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(SupplementsIngestResult.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.results()).hasSize(2);
		assertThat(response.results().getFirst().outcome()).isEqualTo("CREATED");

		final var captor = ArgumentCaptor.forClass(LifecareSupplements.class);
		verify(serviceMock).ingest(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), captor.capture());
		final var supplements = captor.getValue();
		assertThat(supplements.capturedAt()).isEqualTo("2026-08-25");
		assertThat(supplements.reminders()).isNull();
		assertThat(supplements.documents()).hasSize(1);
		assertThat(supplements.documents().getFirst().id()).isEqualTo("27");
		assertThat(supplements.documents().getFirst().typeCode()).isEqualTo("1");
		assertThat(supplements.documents().getFirst().documentType()).isEqualTo("3");
		assertThat(supplements.documents().getFirst().documentTypeName()).isEqualTo("JournalNote");
		assertThat(supplements.jobStimulus().applicant().periods()).hasSize(1);
		assertThat(supplements.jobStimulus().applicant().periods().getFirst().fromDate()).isEqualTo("2021-01-01");
		assertThat(supplements.jobStimulus().coApplicant()).isNull();
	}
}
