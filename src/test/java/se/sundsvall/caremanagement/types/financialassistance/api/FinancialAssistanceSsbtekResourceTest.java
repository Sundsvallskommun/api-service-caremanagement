package se.sundsvall.caremanagement.types.financialassistance.api;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekBasis;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceSsbtekService;

import static java.time.Month.JANUARY;
import static java.time.Month.MARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceSsbtekResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/ssbtek";
	private static final Map<String, String> PATH_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);

	@MockitoBean
	private FinancialAssistanceSsbtekService ssbtekServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getBasis() {
		when(ssbtekServiceMock.getBasis(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("APPLICANT"), isNull(), isNull(), isNull()))
			.thenReturn(SsbtekBasis.create()
				.withFrom(LocalDate.of(2026, JANUARY, 1))
				.withTo(LocalDate.of(2026, MARCH, 31))
				.withAgencies(Map.of("fk", Map.of("formansinformation", Map.of()))));

		final var result = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(PATH_VARIABLES))
			.header("X-Sent-By", "joe01doe; type=adAccount")
			.exchange()
			.expectStatus().isOk()
			.expectBody(SsbtekBasis.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isNotNull();
		assertThat(result.getFrom()).isEqualTo(LocalDate.of(2026, JANUARY, 1));
		assertThat(result.getTo()).isEqualTo(LocalDate.of(2026, MARCH, 31));
		assertThat(result.getAgencies()).containsOnlyKeys("fk");
		verify(ssbtekServiceMock).getBasis(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("APPLICANT"), isNull(), isNull(), isNull());
	}

	@Test
	void getBasisForTheCoApplicantWithExplicitPeriod() {
		when(ssbtekServiceMock.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CO_APPLICANT", null, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, MARCH, 31)))
			.thenReturn(SsbtekBasis.create().withAgencies(Map.of()));

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("person", "CO_APPLICANT").queryParam("from", "2026-01-01").queryParam("to", "2026-03-31").build(PATH_VARIABLES))
			.header("X-Sent-By", "joe01doe; type=adAccount")
			.exchange()
			.expectStatus().isOk();

		verify(ssbtekServiceMock).getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CO_APPLICANT", null, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, MARCH, 31));
	}

	@Test
	void getBasisForAHouseholdChild() {
		final var childPartyId = "5b1e7c3a-9d2f-4e8b-a6c4-1f0d2e3c4b5a";
		when(ssbtekServiceMock.getBasis(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CHILD"), eq(childPartyId), isNull(), isNull()))
			.thenReturn(SsbtekBasis.create().withAgencies(Map.of()));

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("person", "CHILD").queryParam("childPartyId", childPartyId).build(PATH_VARIABLES))
			.header("X-Sent-By", "joe01doe; type=adAccount")
			.exchange()
			.expectStatus().isOk();

		verify(ssbtekServiceMock).getBasis(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq("CHILD"), eq(childPartyId), isNull(), isNull());
	}
}
