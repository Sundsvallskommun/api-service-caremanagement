package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandLifecareCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandNormberakningService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareCalculationResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String ROW_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";
	private static final Map<String, String> PATH_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);

	@MockitoBean
	private ErrandLifecareCalculationService calculationServiceMock;
	@MockitoBean
	private ErrandNormberakningService normberakningServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoMore() {
		verifyNoMoreInteractions(calculationServiceMock, normberakningServiceMock);
	}

	@Test
	void readCalculation() {
		when(calculationServiceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(LifecareCalculationView.create().withId(31)));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculation").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareCalculationView.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull().extracting(LifecareCalculationView::getId).isEqualTo(31);
		verify(calculationServiceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readCalculationBeforeOneIsSaved() {
		when(calculationServiceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.empty());

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculation").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(calculationServiceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void saveCalculation() {
		when(calculationServiceMock.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, true)).thenReturn(LifecareCalculationView.create().withId(31).withFinalized(true));

		final var result = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(LifecareCalculationSaveRequest.create().withFinalize(true))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareCalculationView.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull().extracting(LifecareCalculationView::getFinalized).isEqualTo(true);
		verify(calculationServiceMock).save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, true);
	}

	@Test
	void saveCalculationWithoutFinalize() {
		when(calculationServiceMock.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false)).thenReturn(LifecareCalculationView.create());

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue("{}")
			.exchange()
			.expectStatus().isOk();

		verify(calculationServiceMock).save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false);
	}

	@Test
	void readCalculationPdf() {
		when(calculationServiceMock.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn("%PDF-1.7".getBytes());

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculation/pdf").build(PATH_VARIABLES))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_PDF)
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(result).asString().isEqualTo("%PDF-1.7");
		verify(calculationServiceMock).pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readPreviousCalculation() {
		when(calculationServiceMock.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(NormberakningPreviousCalculation.create().withId(1)));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/normberakning/previous").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormberakningPreviousCalculation.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull().extracting(NormberakningPreviousCalculation::getId).isEqualTo(1);
		verify(calculationServiceMock).readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readPreviousCalculationWhenThereIsNone() {
		when(calculationServiceMock.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.empty());

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/normberakning/previous").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(calculationServiceMock).readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readNormberakning() {
		when(normberakningServiceMock.readDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(NormberakningDraft.create().withSource("LIFECARE"));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/normberakning").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormberakningDraft.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull().extracting(NormberakningDraft::getSource).isEqualTo("LIFECARE");
		verify(normberakningServiceMock).readDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readNormberakningTypes() {
		when(normberakningServiceMock.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(NormberakningTypes.create());

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/normberakning/types").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(NormberakningTypes.class);

		verify(normberakningServiceMock).types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateNormberakningHeader() {
		final var input = NormHeaderInput.create().withNormId(6);

		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/normberakning/header").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(input)
			.exchange()
			.expectStatus().isNoContent();

		verify(normberakningServiceMock).updateHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, input);
	}

	@Test
	void addNormberakningRow() {
		final var input = NormberakningRowInput.create().withTypeId(1).withApplicantCaseworkerAmount(BigDecimal.TEN).withApplicantAmountDate("2026-09-02T00:00:00+02:00");

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/incomes").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(input)
			.exchange()
			.expectStatus().isNoContent();

		verify(normberakningServiceMock).addRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "incomes", input);
	}

	@Test
	void updateNormberakningRow() {
		final var input = NormberakningRowInput.create().withCostType("3").withBucket("EXPENSE").withCaseworkerAmount(BigDecimal.TEN);

		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/normberakning/expenses/E-3-2").build(PATH_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(input)
			.exchange()
			.expectStatus().isNoContent();

		verify(normberakningServiceMock).updateRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "expenses", "E-3-2", input);
	}

	@Test
	void deleteNormberakningRow() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/normberakning/persons/" + ROW_ID).build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(normberakningServiceMock).deleteRow(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "persons", ROW_ID);
	}

	@Test
	void restoreNormberakningRow() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/incomes/" + ROW_ID + "/restore").build(PATH_VARIABLES))
			.exchange()
			.expectStatus().isNoContent();

		verify(normberakningServiceMock).restoreRow(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("incomes"), any());
	}
}
