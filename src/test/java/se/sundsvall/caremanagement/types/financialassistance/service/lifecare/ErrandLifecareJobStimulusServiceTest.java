package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;

@ExtendWith(MockitoExtension.class)
class ErrandLifecareJobStimulusServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, null, null, 2026, 9);

	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareAccessRecorder accessRecorder;
	@Mock
	private LifecareJobStimulusApi jobStimulusApi;

	@InjectMocks
	private ErrandLifecareJobStimulusService service;

	@Captor
	private ArgumentCaptor<JsonNode> bodyCaptor;

	@Test
	void periods() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		final var periods = service.periods(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(periods).hasSize(3).first().isEqualTo(new LifecareJobStimulusPeriod(101, "APPLICANT", "2021-01-01", "2021-12-31"));
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
	}

	@Test
	void addPeriodWithTheEndLifecaresTwoYearRuleGivesSendingEveryExistingPeriodBack() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.readToDate("2028-01-15")).thenReturn(json("\"2030-01-14\""));
		when(jobStimulusApi.save(any())).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		final var periods = service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", null));

		assertThat(periods).hasSize(3);
		verify(jobStimulusApi).save(bodyCaptor.capture());
		final var sent = bodyCaptor.getValue().get("applicant").get("periods");
		assertThat(sent.values()).extracting(period -> period.get("fromDate").asString()).containsExactly("2021-01-01", "2022-07-08", "2026-01-01", "2028-01-15");
		assertThat(sent.get(3).get("toDate").asString()).isEqualTo("2030-01-14");
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
		verify(accessRecorder).written(ERRAND, "CREATE", "JOB_STIMULUS", "Lade till en jobbstimulansperiod i Lifecare (2028-01-15 – 2030-01-14)", null);
	}

	@Test
	void addPeriodWithTheCaseworkersEnd() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.save(any())).thenReturn(MissingNode.getInstance());

		assertThat(service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", "2028-12-31"))).isEmpty();

		verify(jobStimulusApi, never()).readToDate(anyString());
		verify(jobStimulusApi).save(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("applicant").get("periods").get(3).get("toDate").asString()).isEqualTo("2028-12-31");
	}

	@Test
	void addPeriodRefusedForAHouseholdWithAMedsokandeInCareM() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(errandService.coApplicantPresent(ERRAND)).thenReturn(true);

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", "2028-12-31")))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT));
		verifyNoInteractions(jobStimulusApi, accessRecorder);
	}

	@Test
	void addPeriodRefusedForAHouseholdWithAMedsokandeInLifecare() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT.replace("\"hasCoApplicant\": false", "\"hasCoApplicant\": true")));

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", "2028-12-31")))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT));
		verify(jobStimulusApi, never()).save(any());
	}

	@Test
	void addPeriodFailsWhenLifecareGivesNoEndDate() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.readToDate("2028-01-15")).thenReturn(MissingNode.getInstance());

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", null)))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
		verify(jobStimulusApi, never()).save(any());
	}
}
