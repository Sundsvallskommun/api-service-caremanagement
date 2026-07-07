package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PDF;

class FinancialAssistanceLifecareResourceTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void listCalculations() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(serviceMock.listCalculations(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull()))
			.thenReturn(List.of(LifecareCalculation.create().withId(7001).withNormSum(10500.0)));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculations").queryParam("partyId", partyId).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareCalculation.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).singleElement().satisfies(calculation -> assertThat(calculation.getId()).isEqualTo(7001));
		verify(serviceMock).listCalculations(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull());
	}

	@Test
	void listCalculationsWithExplicitPeriod() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(serviceMock.listCalculations(MUNICIPALITY_ID, partyId, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30)))
			.thenReturn(List.of());

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculations").queryParam("partyId", partyId).queryParam("from", "2026-01-01").queryParam("to", "2026-06-30").build(base()))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).listCalculations(MUNICIPALITY_ID, partyId, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void listDecisions() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(serviceMock.listDecisions(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull()))
			.thenReturn(List.of(LifecareDecision.create().withId(9900).withType("Bifall")));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/decisions").queryParam("partyId", partyId).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareDecision.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).singleElement().satisfies(decision -> assertThat(decision.getId()).isEqualTo(9900));
		verify(serviceMock).listDecisions(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull());
	}

	@Test
	void listDocuments() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(serviceMock.listDocuments(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull()))
			.thenReturn(List.of(LifecareDocument.create().withId("doc-1").withTitle("Beslut")));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents").queryParam("partyId", partyId).build(base()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareDocument.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).singleElement().satisfies(document -> assertThat(document.getId()).isEqualTo("doc-1"));
		verify(serviceMock).listDocuments(eq(MUNICIPALITY_ID), eq(partyId), isNull(), isNull());
	}

	@Test
	void readDocumentContent() {
		final var documentId = "a3f1c2d4-0000-1111-2222-333344445555";
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		when(serviceMock.readDocumentContent(eq(MUNICIPALITY_ID), eq(partyId), eq(documentId), isNull(), isNull())).thenReturn("%PDF-1.4".getBytes());

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents/{documentId}/content").queryParam("partyId", partyId).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "documentId", documentId)))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_PDF)
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isEqualTo("%PDF-1.4".getBytes());
		verify(serviceMock).readDocumentContent(MUNICIPALITY_ID, partyId, documentId, null, null);
	}

}
