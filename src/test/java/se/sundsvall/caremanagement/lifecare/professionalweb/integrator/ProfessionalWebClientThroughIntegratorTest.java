package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebFormField;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * The ProfessionalWeb client driven through the only transport there is, the integrator's exchange. Only the Feign
 * client to lifecare-integrator is mocked.
 */
@ExtendWith(MockitoExtension.class)
class ProfessionalWebClientThroughIntegratorTest {

	private static final String MUNICIPALITY_ID = "2281";
	/** Lifecare's parameter page for "Beslut", as captured from a real browser. */
	private static final String BESLUT_PAGE = """
		<!DOCTYPE html><html><head><title>Parameterfrågor</title></head><body>
		<div id="app-config"></div>
		<form id="myForm" method="post" novalidate autocomplete="off">
		  <input type="checkbox" name="51_0_2_1" id="51_0_2_1" value="true" checked><label for="51_0_2_1">Visa medsökande</label>
		  <input type="hidden" name="51_0_2_1" value="false">
		  <input type="hidden" name="X-LEGACY-TOKEN" value="tok">
		  <button type="submit" id="submitButton">Klar</button>
		</form>
		<script src="../dist2/parameter_query_main.js"></script>
		</body></html>""";

	@Mock
	private ProfessionalWebIntegratorClient integratorClient;

	@Captor
	private ArgumentCaptor<ProfessionalWebExchangeRequest> requestCaptor;

	private ProfessionalWebClient client;

	@BeforeEach
	void setUp() {
		client = new ProfessionalWebClient(new IntegratorProfessionalWebTransport(integratorClient));
	}

	@Test
	void jsonReadReachesTheExchangeUnchanged() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "application/json", "{\"id\":7}"));
		final var params = new LinkedHashMap<String, String>();
		params.put("b", "2");
		params.put("a", "1");

		final var result = client.get("api2/Thing/Get", params);

		assertThat(result.get("id").asInt()).isEqualTo(7);
		verify(integratorClient).exchange(eq(MUNICIPALITY_ID), requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.method()).isEqualTo("GET");
		assertThat(request.path()).isEqualTo("api2/Thing/Get");
		assertThat(request.params()).containsExactly(Map.entry("b", "2"), Map.entry("a", "1"));
		assertThat(request.body()).isNull();
		assertThat(request.form()).isNull();
	}

	@Test
	void jsonWriteReachesTheExchangeWithItsBody() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "application/json", "{\"saved\":1}"));

		final var result = client.post("api2/Decision/Create", Map.of("businessType", "8"), Map.of("a", 1, "b", "x"));

		assertThat(result.get("saved").asInt()).isEqualTo(1);
		verify(integratorClient).exchange(eq(MUNICIPALITY_ID), requestCaptor.capture());
		final var request = requestCaptor.getValue();
		assertThat(request.method()).isEqualTo("POST");
		assertThat(request.path()).isEqualTo("api2/Decision/Create");
		assertThat(request.params()).containsExactly(Map.entry("businessType", "8"));
		assertThat(request.body().get("a").asInt()).isEqualTo(1);
		assertThat(request.body().get("b").asString()).isEqualTo("x");
		assertThat(request.body().size()).isEqualTo(2);
		assertThat(request.form()).isNull();
	}

	@Test
	void deleteSendsTheIdInTheBodyAndAcceptsAnEmptyAnswer() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(new ProfessionalWebExchangeResponse(200, null, null));

		assertThat(client.delete("api2/Thing/Remove", Map.of("id", 5)).isMissingNode()).isTrue();

		verify(integratorClient).exchange(eq(MUNICIPALITY_ID), requestCaptor.capture());
		assertThat(requestCaptor.getValue().method()).isEqualTo("DELETE");
		assertThat(requestCaptor.getValue().body().get("id").asInt()).isEqualTo(5);
	}

	@Test
	void deleteWithoutABodySendsNone() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(new ProfessionalWebExchangeResponse(200, null, null));

		assertThat(client.delete("api2/Thing/Remove", Map.of("businessId", "53"), null).isMissingNode()).isTrue();

		verify(integratorClient).exchange(eq(MUNICIPALITY_ID), requestCaptor.capture());
		assertThat(requestCaptor.getValue().params()).containsExactly(Map.entry("businessId", "53"));
		assertThat(requestCaptor.getValue().body()).isNull();
	}

	@Test
	void lifecaresRefusalIsMappedAsBefore() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(461, "application/json", "{\"exceptionMessage\":\"Datum i framtiden\"}"));

		assertThatThrownBy(() -> client.post("api2/Thing/Save", Map.of(), Map.of()))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus().value()).isEqualTo(422);
				assertThat(problem.getDetail()).isEqualTo("Datum i framtiden");
			});
	}

	@Test
	void anAnswerThatIsNotJsonIsABadGateway() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "application/json", "<nope"));

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of()))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY))
			.hasMessageContaining("not JSON");
	}

	@Test
	void anAnswerWithoutLifecaresAnswerIsABadGateway() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(null);

		assertThatThrownBy(() -> client.get("api2/Thing/Get", Map.of()))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY))
			.hasMessageContaining("without Lifecare's answer");
	}

	@Test
	void aPdfIsReturnedAsItsBytes() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "application/pdf", "%PDF-1.7 x"));

		assertThat(client.getPdf("RenderPdf/Print", Map.of("id", "1"))).isEqualTo("%PDF-1.7 x".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void somethingThatIsNotAPdfIsRefused() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "application/json", "{}"));

		assertThatThrownBy(() -> client.getPdf("RenderPdf/Print", Map.of())).hasMessageContaining("did not answer with a PDF");
	}

	@Test
	void aFormPostingReachesTheExchangeAsAPostWithItsFieldsInOrder() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any()))
			.thenReturn(answer(200, "text/html; charset=utf-8", BESLUT_PAGE))
			.thenReturn(answer(200, "application/pdf", "%PDF-1.7 beslut"));

		final var pdf = client.getPdf("RenderPdf/PrintDecision", printParams());

		assertThat(pdf).isEqualTo("%PDF-1.7 beslut".getBytes(StandardCharsets.UTF_8));
		verify(integratorClient, times(2)).exchange(eq(MUNICIPALITY_ID), requestCaptor.capture());
		final var form = requestCaptor.getAllValues().get(1);
		assertThat(form.method()).isEqualTo("POST");
		assertThat(form.path()).isEqualTo("RenderPdf/PrintDecision");
		assertThat(form.params()).containsExactly(Map.entry("templateId", "885bfb68"), Map.entry("decisionId", "134"), Map.entry("hideRevisions", "true"));
		assertThat(form.body()).isNull();
		assertThat(form.form()).containsExactly(
			new ProfessionalWebFormField("51_0_2_1", "true"),
			new ProfessionalWebFormField("51_0_2_1", "false"),
			new ProfessionalWebFormField("X-LEGACY-TOKEN", "tok"));
	}

	@Test
	void givesUpWhenLifecareAsksTheParameterQuestionsAgain() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any())).thenReturn(answer(200, "text/html; charset=utf-8", BESLUT_PAGE));

		assertThatThrownBy(() -> client.getPdf("RenderPdf/PrintDecision", printParams())).hasMessageContaining("parameterfråga");
		verify(integratorClient, times(2)).exchange(eq(MUNICIPALITY_ID), any());
	}

	@Test
	void reportsLifecaresRefusalOfTheParameters() {
		when(integratorClient.exchange(eq(MUNICIPALITY_ID), any()))
			.thenReturn(answer(200, "text/html; charset=utf-8", BESLUT_PAGE))
			.thenReturn(answer(461, "application/json", "{\"exceptionMessage\":\"Ogiltig parameter\"}"));

		assertThatThrownBy(() -> client.getPdf("RenderPdf/PrintDecision", printParams()))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getDetail()).isEqualTo("Ogiltig parameter"));
	}

	private static ProfessionalWebExchangeResponse answer(final int status, final String contentType, final String body) {
		return new ProfessionalWebExchangeResponse(status, contentType, Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)));
	}

	private static Map<String, String> printParams() {
		final var params = new LinkedHashMap<String, String>();
		params.put("templateId", "885bfb68");
		params.put("decisionId", "134");
		params.put("hideRevisions", "true");
		return params;
	}
}
