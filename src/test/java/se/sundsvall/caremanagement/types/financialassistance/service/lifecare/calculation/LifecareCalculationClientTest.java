package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;

@ExtendWith(MockitoExtension.class)
class LifecareCalculationClientTest {

	@Mock
	private ProfessionalWebClient professionalWeb;
	@Mock
	private ProfessionalWebProperties properties;

	@Captor
	private ArgumentCaptor<Map<String, String>> paramsCaptor;
	@Captor
	private ArgumentCaptor<Object> bodyCaptor;

	private LifecareCalculationClient client;

	@BeforeEach
	void setUp() {
		client = new LifecareCalculationClient(professionalWeb, properties);
	}

	private Map<String, String> readWith(final String path) {
		verify(professionalWeb).get(eq(path), paramsCaptor.capture());
		return paramsCaptor.getValue();
	}

	@Test
	void readsTheUnderlagForANewBeräkningOnTheInsats() {
		when(professionalWeb.get(any(), anyMap())).thenReturn(json("{\"calculation\":{}}"));

		assertThat(client.readProposal(24).has("calculation")).isTrue();

		assertThat(new ArrayList<>(readWith("api2/Calculation/GetProposalService").entrySet())).containsExactly(entry("businessType", "8"), entry("businessId", "24"));
	}

	@Test
	void listsTheInsatssBeräkningar() {
		client.listForService(24);

		assertThat(new ArrayList<>(readWith("api2/Calculation/ListCalculations").entrySet())).containsExactly(entry("businessType", "8"), entry("businessId", "24"),
			entry("investigationId", "0"), entry("serviceId", "24"), entry("onlylatest", "true"));
	}

	@Test
	void readsASavedBeräkning() {
		client.read(31);
		assertThat(readWith("api2/Calculation/GetCalculation")).containsExactly(entry("businessType", "3"), entry("businessId", "31"));
	}

	@Test
	void readsASavedBeräkningForEdit() {
		client.readForEdit(31);
		assertThat(readWith("api2/Calculation/GetCalculationForEdit")).containsExactly(entry("businessType", "3"), entry("businessId", "31"));
	}

	@Test
	void readsTheInsatssJobbstimulans() {
		client.readJobStimulus(24);
		assertThat(readWith("api2/Calculation/GetJobStimulusForService")).containsExactly(entry("businessType", "8"), entry("businessId", "24"));
	}

	@Test
	void placesPersons() {
		final var body = json("{\"normId\":1}");

		client.placePersons(body);

		verify(professionalWeb).post("api2/Calculation/PlacePersons", Map.of(), body);
	}

	@Test
	void countsAMembersAmount() {
		when(professionalWeb.post(eq("api2/Calculation/GetAmount"), eq(Map.of()), bodyCaptor.capture())).thenReturn(json("{\"amount\":1300}"));

		final var amount = client.amountFor(json("{\"personId\":\"a\"}"), json("{\"rowId\":2}"), tree("\"2026-09-01\""), tree("\"2026-09-30\""));

		assertThat(amount.intValue()).isEqualTo(1300);
		assertThat(bodyCaptor.getValue()).isEqualTo(json("""
			{"person":{"personId":"a"},"normRow":{"rowId":2},"startDate":"2026-09-01","endDate":"2026-09-30"}"""));
	}

	@Test
	void countsTheGemensammaKostnader() {
		when(professionalWeb.post(eq("api2/Calculation/GetSharedCost"), eq(Map.of()), bodyCaptor.capture())).thenReturn(tree("2030"));

		assertThat(client.sharedCost(tree("\"2026-09-01\""), tree("\"2026-09-30\""), json("{\"noOfMembers\":4}")).intValue()).isEqualTo(2030);
		assertThat(bodyCaptor.getValue()).isEqualTo(json("{\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-30\",\"normShared\":{\"noOfMembers\":4}}"));
	}

	@Test
	void marksJobbstimulans() {
		client.withJobStimuli(json("{\"calculationId\":1}"), json("{\"applicant\":null}"));

		verify(professionalWeb).post(eq("api2/Calculation/GetJobStimuliForCalculation"), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue()).isEqualTo(json("{\"calculation\":{\"calculationId\":1},\"jobStimulus\":{\"applicant\":null}}"));
	}

	@Test
	void createsOnTheInsatsAndUpdatesTheBeräkning() {
		final ObjectNode body = json("{}");

		client.create(24, body);
		client.update(31, body);

		verify(professionalWeb).post(eq("api2/Calculation/Create"), paramsCaptor.capture(), eq(body));
		assertThat(paramsCaptor.getValue()).containsExactly(entry("businessType", "8"), entry("businessId", "24"));
		verify(professionalWeb).post(eq("api2/Calculation/Update"), paramsCaptor.capture(), eq(body));
		assertThat(paramsCaptor.getValue()).containsExactly(entry("businessType", "3"), entry("businessId", "31"));
	}

	@Test
	void printsTheBeräkningWithLifecaresTemplate() {
		when(properties.calculationPrintTemplateId()).thenReturn("template");
		when(professionalWeb.getPdf(eq("RenderPdf/PrintContainer"), paramsCaptor.capture())).thenReturn(new byte[] {
			1
		});

		assertThat(client.print(31)).containsExactly(1);
		assertThat(paramsCaptor.getValue()).containsExactly(entry("templateId", "template"), entry("ownerType", "BERAK"), entry("ownerCode", "999999999"),
			entry("objectId", "31"));
	}
}
