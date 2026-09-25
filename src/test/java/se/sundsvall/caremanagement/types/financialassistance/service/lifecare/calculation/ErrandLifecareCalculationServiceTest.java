package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningDraftReader.DraftWithNumbers;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.applicantPerson;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.draft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErrandLifecareCalculationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final LifecareErrand UNLINKED = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, null, null, 2026, 9);
	private static final LifecareErrand LINKED = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, 31, null, null, 2026, 9);

	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareCalculationClient client;
	@Mock
	private NormberakningDraftReader draftReader;
	@Mock
	private LifecareAccessRecorder recorder;

	@Captor
	private ArgumentCaptor<ObjectNode> bodyCaptor;

	private ErrandLifecareCalculationService service;

	/** Calculation/Create's answer (capture 2026-09-24): beräkning 31 with Lifecare's summering. */
	private static ObjectNode saved() {
		return json("""
			{"calculationId":31,"normId":1,"normText":"Riksnorm 2026","date":"2026-09-24","startDate":"2026-09-01","endDate":"2026-09-30",
			 "calculationPersons":[%s],"calculationIncomes":[],"calculationExpenses":[],"calculationSpecialExpenses":[],
			 "hasCustomHouseholdSize":false,"isFinalized":false,"updateTimestamp":"2026-09-24",
			 "calculationSummary":{"income":0,"jobStimulus":0,"jobStimulusDeduction":0,"norm":-5220,"expences":0,"sum":-5220,"specialPurpose":0,
			   "balance":-5220,"deficitSum":5220,"commonHouseholdCost":1280,"familyCost":3940}}""".formatted(applicantPerson(true, 2, 3940)));
	}

	private static ObjectNode proposal() {
		return json("""
			{"calculation":{"calculationId":0,"aktualiseringId":0,"normId":1,"normText":null,"date":"2026-09-24","startDate":"","endDate":"",
			   "calculationSummary":null,"calculationPersons":[%s],"calculationIncomes":[],"calculationExpenses":[],"calculationSpecialExpenses":[],
			   "householdSize":0,"numberOfFamilyMembers":0,"hasCustomHouseholdSize":false,"isFinalized":false,"updateTimestamp":""},
			 "norms":[{"normId":1,"name":"Riksnorm 2026"}],"incomeTypes":[{"id":27,"text":"Efterlevandestöd","isActive":true}],
			 "expenseTypes":[{"id":8,"text":"A-kasseavgift","isActive":true},{"id":3,"text":"Boendekostnad","isActive":true}],
			 "specialExpenseTypes":[{"id":7,"text":"Tandvård","isActive":true}]}""".formatted(applicantPerson(false, 0, 0)));
	}

	private static ObjectNode forEdit(final ObjectNode calculation) {
		final var forEdit = proposal();
		forEdit.set("calculation", calculation);
		return forEdit;
	}

	@BeforeEach
	void setUp() {
		service = new ErrandLifecareCalculationService(errandService, client, new LifecareCalculationEditService(client, recorder), draftReader, recorder);
		final var draft = draft();
		when(draftReader.read(any())).thenReturn(new DraftWithNumbers(draft, Map.of("p1", "880209-T050")));
		when(client.readProposal(1)).thenReturn(proposal());
		when(client.readJobStimulus(1)).thenReturn(json("{\"applicant\":null,\"coApplicant\":null,\"hasCoApplicant\":false}"));
		when(client.placePersons(any())).thenReturn(json("{\"calculationPersons\":[" + applicantPerson(true, 2, 3940) + "]}"));
		when(client.withJobStimuli(any(), any())).thenReturn(json("{\"hasApplicantJobStimuli\":true}"));
	}

	private void errandIs(final LifecareErrand errand) {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);
	}

	@Test
	void createsTheBeräkningTheFirstTimePlacedOnTheNormAndLinksTheErrand() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenReturn(saved());

		final var view = service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false);

		verify(client).create(eq(1), bodyCaptor.capture());
		final var body = bodyCaptor.getValue();
		assertThat(objects(body, "calculationPersons").getFirst().path("normRowId").intValue()).isEqualTo(2);
		assertThat(objects(body, "calculationPersons").getFirst().path("amount").intValue()).isEqualTo(3940);
		assertThat(body.path("hasApplicantJobStimuli").booleanValue()).isTrue();
		assertThat(body.path("startDate").stringValue()).isEqualTo("2026-09-01");
		assertThat(objects(body, "calculationIncomes")).extracting(row -> row.path("incomeCode").intValue()).containsExactly(27);
		assertThat(body.path("HouseholdSize").intValue()).isEqualTo(1);
		assertThat(body.has("aktualiseringId")).isFalse();
		verify(errandService).linkCalculation(UNLINKED, 31);
		verify(recorder).read(UNLINKED, "CALCULATION", "Läste beräkningsunderlag i Lifecare");
		verify(recorder).written(UNLINKED, "CREATE", "CALCULATION", "Sparade normberäkningen i Lifecare", "31");
		verify(client, never()).update(anyInt(), any());
		// Lifecare's summering, signed the way the caseworker reads it.
		assertThat(view.getId()).isEqualTo(31);
		assertThat(view.getSummary().getNorm()).isEqualByComparingTo("5220");
		assertThat(view.getSummary().getFamilyCost()).isEqualByComparingTo("3940");
		assertThat(view.getSummary().getCommonHouseholdCost()).isEqualByComparingTo("1280");
		assertThat(view.getSummary().getResult()).isEqualByComparingTo("-5220");
	}

	@Test
	void changesTheSameBeräkningEveryTimeAfterNeverMakingASecondOne() {
		errandIs(LINKED);
		when(client.readForEdit(31)).thenReturn(forEdit(saved()));
		when(client.update(eq(31), any())).thenReturn(saved());

		service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false);

		verify(client, never()).create(anyInt(), any());
		verify(client).update(eq(31), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("calculationId").intValue()).isEqualTo(31);
		assertThat(bodyCaptor.getValue().path("HouseholdSize").intValue()).isEqualTo(1);
		verifyNoInteractions(draftReader);
	}

	@Test
	void savesAsSlutligWithIsFinalizedOnTheSameBeräkning() {
		errandIs(LINKED);
		when(client.readForEdit(31)).thenReturn(forEdit(saved()));
		when(client.update(eq(31), any())).thenReturn(saved().put("isFinalized", true));

		final var view = service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, true);

		verify(client).update(eq(31), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("isFinalized").booleanValue()).isTrue();
		assertThat(view.getFinalized()).isTrue();
	}

	@Test
	void createsTheBeräkningFirstWhenSavingAsSlutligBeforeItExists() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenReturn(saved());
		when(client.readForEdit(31)).thenReturn(forEdit(saved()));
		when(client.update(eq(31), any())).thenReturn(saved().put("isFinalized", true));

		service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, true);

		verify(client).update(eq(31), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("isFinalized").booleanValue()).isTrue();
	}

	@Test
	void refusesToChangeABeräkningSavedAsSlutlig() {
		errandIs(LINKED);
		when(client.readForEdit(31)).thenReturn(forEdit(saved().put("isFinalized", true)));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false)).hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		verify(client, never()).update(anyInt(), any());
	}

	@Test
	void refusesADraftLifecareCannotTakeBeforeCreatingAnything() {
		errandIs(UNLINKED);
		when(draftReader.read(any())).thenReturn(new DraftWithNumbers(draft().withCalculationFromDate(null), Map.of()));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false)).hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		verify(client, never()).create(anyInt(), any());
	}

	@Test
	void refusesToCreateWithoutAnInsats() {
		errandIs(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false)).hasFieldOrPropertyWithValue("status", CONFLICT);
		verifyNoInteractions(client);
	}

	@Test
	void tellsTheCaseworkerToCheckLifecareWhenACreateGotNoAnswer() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare could not be reached"));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("Kontrollera i Lifecare om den finns innan du sparar igen");
		verify(client).create(eq(1), any());
		verify(errandService, never()).linkCalculation(any(), anyInt());
	}

	@Test
	void treatsAnythingButARefusalOnCreateAsNoAnswer() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenThrow(new IllegalStateException("socket closed"));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("Lifecare svarade inte");
	}

	@Test
	void passesOnLifecaresRefusalOfACreate() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenThrow(Problem.valueOf(UNPROCESSABLE_CONTENT, "Datum saknas"));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Datum saknas");

		doThrow(Problem.valueOf(BAD_REQUEST, "Fel")).when(client).create(eq(1), any());
		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false)).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void saysTheBeräkningExistsWhenTheErrandCannotBeLinkedToIt() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenReturn(saved());
		doThrow(Problem.valueOf(CONFLICT, "Another calculation is linked")).when(errandService).linkCalculation(UNLINKED, 31);

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("beräkning 31")
			.hasMessageContaining("Spara inte igen");
		verify(recorder).written(UNLINKED, "CREATE", "CALCULATION", "Sparade normberäkningen i Lifecare", "31");
	}

	@Test
	void failsWhenLifecareCreatesWithoutAnswerAnId() {
		errandIs(UNLINKED);
		when(client.create(eq(1), any())).thenReturn(json("{}"));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("utan dess id");
		verify(errandService, never()).linkCalculation(any(), anyInt());
	}

	@Test
	void givesTheBeräkningAsLifecarePrintsIt() {
		errandIs(LINKED);
		when(client.print(31)).thenReturn("%PDF-1.7".getBytes());

		assertThat(service.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).asString().isEqualTo("%PDF-1.7");
		verify(recorder).read(LINKED, "CALCULATION", "Läste normberäkningen som PDF i Lifecare", "31");
	}

	@Test
	void hasNoPdfBeforeTheBeräkningIsSaved() {
		errandIs(UNLINKED);

		assertThatThrownBy(() -> service.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verifyNoInteractions(client);
	}

	@Test
	void readsTheBeräkningAsItStandsInLifecare() {
		errandIs(LINKED);
		when(client.readForEdit(31)).thenReturn(forEdit(saved()));

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).get().satisfies(view -> assertThat(view.getId()).isEqualTo(31));
		verify(recorder).read(LINKED, "CALCULATION", "Läste normberäkningen i Lifecare", "31");
	}

	@Test
	void hasNoBeräkningToShowBeforeOneIsSaved() {
		errandIs(UNLINKED);

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
		verifyNoInteractions(client, recorder);
	}

	// ---- the previous beräkning ------------------------------------------------------------------------------------

	private static final String LISTED = """
		[{"calculationId":31,"date":"2026-09-23","startDate":"2026-09-01","endDate":"","isFinalized":true},
		 {"calculationId":1,"date":"2026-09-23","startDate":"2026-01-01","endDate":"","isFinalized":true}]""";

	@Test
	void findsThePreviousBeräkningInTheInsatssListInLifecare() {
		errandIs(LINKED);
		when(client.listForService(1)).thenReturn(tree(LISTED));
		when(client.read(1)).thenReturn(json("{\"calculationId\":1,\"normText\":\"Riksnorm 2026\",\"startDate\":\"2026-01-01\"}"));

		final var previous = service.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(previous).get().satisfies(calculation -> {
			assertThat(calculation.getId()).isEqualTo(1);
			assertThat(calculation.getNorm()).isEqualTo("Riksnorm 2026");
		});
		verify(recorder).read(LINKED, "CALCULATION", "Läste insatsens normberäkningar i Lifecare");
		verify(recorder).read(LINKED, "CALCULATION", "Läste föregående normberäkning i Lifecare", "1");
		verifyNoInteractions(draftReader);
	}

	@Test
	void takesTheErrandsPeriodFromCaremsDraftBeforeTheBeräkningIsSavedInLifecare() {
		errandIs(UNLINKED);
		when(client.listForService(1)).thenReturn(tree(LISTED));
		when(draftReader.periodStart(UNLINKED)).thenReturn(Optional.of("2026-10-01"));
		when(client.read(31)).thenReturn(json("{\"calculationId\":31}"));

		assertThat(service.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).get().satisfies(calculation -> assertThat(calculation.getId()).isEqualTo(31));
		verify(client).read(31);
	}

	@Test
	void hasNothingToShowWhenTheInsatsHasNoBeräkningBeforeThePeriod() {
		errandIs(UNLINKED);
		when(client.listForService(1)).thenReturn(tree(LISTED));
		when(draftReader.periodStart(UNLINKED)).thenReturn(Optional.of("2026-01-01"));

		assertThat(service.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
		verify(client, never()).read(anyInt());
	}

	@Test
	void hasNothingToShowWithoutAnInsats() {
		errandIs(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));

		assertThat(service.readPrevious(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
		verifyNoInteractions(client, recorder);
	}
}
