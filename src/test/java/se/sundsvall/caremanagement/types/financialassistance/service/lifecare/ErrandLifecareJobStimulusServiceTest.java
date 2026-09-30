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
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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

	/** The fixture's sökande with a medsökande who has no periods yet, as Lifecare holds such a household. */
	private static final String WITH_CO_APPLICANT = LifecareJobStimulusMapperTest.CURRENT
		.replace("\"coApplicant\": null", "\"coApplicant\": { \"periods\": [], \"personId\": \"20120505T020\", \"name\": \"Jeppson, Medsökande\", \"personIdFormatted\": \"120505-T020\" }")
		.replace("\"hasCoApplicant\": false", "\"hasCoApplicant\": true");

	@Test
	void periods() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		final var periods = service.periods(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(periods).hasSize(3).first().isEqualTo(new LifecareJobStimulusPeriod(101, "APPLICANT", "2021-01-01", "2021-12-31"));
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
	}

	@Test
	void periodsAreNotServedWhenTheReadCannotBeLogged() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		doThrow(new IllegalStateException("log down")).when(accessRecorder).read(any(), anyString(), anyString());

		assertThatThrownBy(() -> service.periods(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void addPeriodWithTheEndLifecaresTwoYearRuleGivesSendingEveryExistingPeriodBack() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.readToDate("2028-01-15")).thenReturn(json("\"2030-01-14\""));
		when(jobStimulusApi.save(any())).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		final var periods = service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", null, null));

		assertThat(periods).hasSize(3);
		verify(jobStimulusApi).save(bodyCaptor.capture());
		final var sent = bodyCaptor.getValue().get("applicant").get("periods");
		assertThat(sent.values()).extracting(period -> period.get("fromDate").asString()).containsExactly("2021-01-01", "2022-07-08", "2026-01-01", "2028-01-15");
		assertThat(sent.get(3).get("toDate").asString()).isEqualTo("2030-01-14");
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
		verify(accessRecorder).written(ERRAND, "CREATE", "JOB_STIMULUS", "Lade till en jobbstimulansperiod i Lifecare (2028-01-15 – 2030-01-14)", null);
	}

	@Test
	void addPeriodWritesNothingWhenTheReadCannotBeLogged() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		doThrow(new IllegalStateException("log down")).when(accessRecorder).read(any(), anyString(), anyString());
		final var request = new LifecareJobStimulusPeriodRequest("2028-01-15", "2028-12-31", null);

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request))
			.isInstanceOf(IllegalStateException.class);
		verify(jobStimulusApi, never()).save(any());
	}

	@Test
	void addPeriodWithTheCaseworkersEnd() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.save(any())).thenReturn(MissingNode.getInstance());

		assertThat(service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", "2028-12-31", null))).isEmpty();

		verify(jobStimulusApi, never()).readToDate(anyString());
		verify(jobStimulusApi).save(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("applicant").get("periods").get(3).get("toDate").asString()).isEqualTo("2028-12-31");
	}

	@Test
	void addPeriodForTheMedsokandeSendsBothPersonsPeriods() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(WITH_CO_APPLICANT));
		when(jobStimulusApi.save(any())).thenReturn(json(WITH_CO_APPLICANT));

		service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2026-09-01", "2028-08-31", "CO_APPLICANT"));

		verify(jobStimulusApi).save(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("coApplicant").get("periods").values()).extracting(period -> period.get("fromDate").asString()).containsExactly("2026-09-01");
		assertThat(bodyCaptor.getValue().get("applicant").get("periods").size()).isEqualTo(3);
		verify(accessRecorder).written(ERRAND, "CREATE", "JOB_STIMULUS", "Lade till en jobbstimulansperiod för medsökanden i Lifecare (2026-09-01 – 2028-08-31)", null);
	}

	@Test
	void addPeriodForAMedsokandeTheInsatsDoesNotHaveIsRefused() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2026-09-01", "2028-08-31", "CO_APPLICANT")))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT));
		verify(jobStimulusApi, never()).save(any());
	}

	@Test
	void addPeriodFailsWhenLifecareGivesNoEndDate() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.readToDate("2028-01-15")).thenReturn(MissingNode.getInstance());

		assertThatThrownBy(() -> service.addPeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecareJobStimulusPeriodRequest("2028-01-15", null, null)))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
		verify(jobStimulusApi, never()).save(any());
	}

	@Test
	void removePeriodSavesTheSetWithoutItAndAnswersWithTheNewIds() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.save(any())).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT.replace("101", "110").replace("103", "111")));

		final var periods = service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 102);

		assertThat(periods).extracting(LifecareJobStimulusPeriod::id).contains(110, 111);
		verify(jobStimulusApi).save(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("applicant").get("periods").values()).extracting(period -> period.get("fromDate").asString())
			.containsExactly("2021-01-01", "2026-01-01");
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
		verify(accessRecorder).written(ERRAND, "DELETE", "JOB_STIMULUS", "Tog bort en jobbstimulansperiod i Lifecare (2022-07-08 – 2023-07-07)", "102");
	}

	@Test
	void removePeriodWithoutEndIsLoggedAsOpenEnded() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT.replace("\"toDate\": \"2023-07-07\"", "\"toDate\": \"\"")));
		when(jobStimulusApi.save(any())).thenReturn(MissingNode.getInstance());

		assertThat(service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 102)).isEmpty();

		verify(accessRecorder).written(ERRAND, "DELETE", "JOB_STIMULUS", "Tog bort en jobbstimulansperiod i Lifecare (2022-07-08 – tills vidare)", "102");
	}

	@Test
	void removePeriodThatIsNoLongerInLifecareSavesNothing() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));

		assertThatThrownBy(() -> service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 107))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(NOT_FOUND);
				assertThat(problem.getDetail()).contains("Läs om listan");
			});
		verify(accessRecorder).read(ERRAND, "JOB_STIMULUS", "Läste jobbstimulans i Lifecare");
		verify(jobStimulusApi, never()).save(any());
		verify(accessRecorder, never()).written(any(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void removePeriodWritesNothingWhenTheReadCannotBeLogged() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		doThrow(new IllegalStateException("log down")).when(accessRecorder).read(any(), anyString(), anyString());

		assertThatThrownBy(() -> service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 102)).isInstanceOf(IllegalStateException.class);
		verify(jobStimulusApi, never()).save(any());
	}

	@Test
	void removePeriodOfTheMedsokandeKeepsTheSokandes() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(WITH_CO_APPLICANT.replace("\"periods\": [], \"personId\": \"20120505T020\"",
			"\"periods\": [{ \"jobStimulusId\": 115, \"personId\": \"20120505T020\", \"fromDate\": \"2026-09-01\", \"toDate\": \"2028-08-31\", \"markedForRemoval\": false }], \"personId\": \"20120505T020\"")));
		when(jobStimulusApi.save(any())).thenReturn(json(WITH_CO_APPLICANT));

		service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 115);

		verify(jobStimulusApi).save(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("coApplicant").get("periods").isEmpty()).isTrue();
		assertThat(bodyCaptor.getValue().get("applicant").get("periods").size()).isEqualTo(3);
		verify(accessRecorder).written(ERRAND, "DELETE", "JOB_STIMULUS", "Tog bort en jobbstimulansperiod för medsökanden i Lifecare (2026-09-01 – 2028-08-31)", "115");
	}

	@Test
	void removePeriodPassesLifecaresFailureOn() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(jobStimulusApi.readForService(1)).thenReturn(json(LifecareJobStimulusMapperTest.CURRENT));
		when(jobStimulusApi.save(any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));

		assertThatThrownBy(() -> service.removePeriod(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 102))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
		verify(accessRecorder, never()).written(any(), anyString(), anyString(), anyString(), any());
	}
}
