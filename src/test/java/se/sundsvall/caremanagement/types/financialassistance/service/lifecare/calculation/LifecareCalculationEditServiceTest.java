package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.forEdit;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.savedCalculation;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LifecareCalculationEditServiceTest {

	private static final LifecareErrand ERRAND = new LifecareErrand("2281", "FINANCIAL_ASSISTANCE", "e1", 1, 30, null, null, 2026, 9);
	private static final int CALCULATION_ID = 30;

	@Mock
	private LifecareCalculationClient client;
	@Mock
	private LifecareAccessRecorder recorder;

	@Captor
	private ArgumentCaptor<ObjectNode> bodyCaptor;

	private LifecareCalculationEditService service;

	@BeforeEach
	void setUp() {
		service = new LifecareCalculationEditService(client, recorder);
		when(client.readJobStimulus(1)).thenReturn(json("{\"applicant\":null,\"coApplicant\":null,\"hasCoApplicant\":false}"));
		// Lifecare places the member where it was and finds jobbstimulans in the period.
		when(client.placePersons(any())).thenReturn(json("{\"calculationPersons\":" + savedCalculation().path("calculationPersons") + "}"));
		when(client.withJobStimuli(any(), any())).thenAnswer(invocation -> ((ObjectNode) invocation.getArgument(0)).deepCopy().put("hasApplicantJobStimuli", true));
		when(client.update(eq(CALCULATION_ID), any())).thenReturn(savedCalculation());
	}

	private void readsForEdit(final ObjectNode forEdit) {
		when(client.readForEdit(CALCULATION_ID)).thenReturn(forEdit);
	}

	private ObjectNode sentUpdate() {
		verify(client).update(eq(CALCULATION_ID), bodyCaptor.capture());
		return bodyCaptor.getValue();
	}

	@Test
	void changesARowCountingJobbstimulansFromTheGrossOnlyOnce() {
		readsForEdit(forEdit(savedCalculation()));

		final var updated = service.change(ERRAND, CALCULATION_ID, (calculation, _) -> CalculationRowChanges.changeExpense(calculation, "E-3",
			NormberakningRowInput.create().withAppliedAmount(BigDecimal.valueOf(5000)).withCaseworkerAmount(BigDecimal.valueOf(4500))), false);

		final var body = sentUpdate();
		assertThat(objects(body, "calculationExpenses").getFirst().path("approvedAmount").intValue()).isEqualTo(4500);
		// The untouched income stays 3 750 counted from 5 000 gross, not 25 % off 3 750 again.
		assertThat(objects(body, "calculationIncomes").getFirst().path("amountApplicant").intValue()).isEqualTo(3750);
		assertThat(objects(body, "calculationIncomes").getFirst().path("grossAmountApplicant").intValue()).isEqualTo(5000);
		assertThat(body.path("isFinalized").booleanValue()).isFalse();
		assertThat(updated.path("calculationId").intValue()).isEqualTo(CALCULATION_ID);
		verify(recorder).read(ERRAND, "CALCULATION", "Läste beräkningsunderlag i Lifecare");
		verify(recorder).written(ERRAND, "UPDATE", "CALCULATION", "Ändrade normberäkningen i Lifecare", "30");
		verify(client, never()).amountFor(any(), any(), any(), any());
		verify(client, never()).sharedCost(any(), any(), any());
	}

	@Test
	void sendsPlacePersonsOnlyTheIncludedMembersOnTheirOldRows() {
		final var saved = savedCalculation();
		((ArrayNode) saved.path("calculationPersons")).add(json("{\"personId\":\"x\",\"included\":false,\"normRowId\":7}"));
		readsForEdit(forEdit(saved));

		service.change(ERRAND, CALCULATION_ID, (calculation, _) -> calculation, false);

		verify(client).placePersons(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue()).isEqualTo(json("""
			{"startDate":"2026-09-01","endDate":"2026-09-30","normId":1,"calculationPersons":[%s]}""".formatted(savedCalculation().path("calculationPersons").get(0))));
		final var body = sentUpdate();
		assertThat(objects(body, "calculationPersons").get(1).has("normRowId")).isFalse();
		assertThat(objects(body, "calculationPersons").get(1).path("amount").intValue()).isZero();
	}

	@Test
	void hasLifecareCountAMembersAmountAgainWhenTheDaysOrTheNormintervallChange() {
		final var saved = savedCalculation();
		saved.set("norm", tree("{\"rows\":[{\"rowId\":2,\"name\":\"Ensamstående 3940.00\",\"monthlyAmount\":3940,\"dailyAmount\":130}]}"));
		readsForEdit(forEdit(saved));
		when(client.amountFor(any(), any(), any(), any())).thenReturn(tree("1300"));

		service.change(ERRAND, CALCULATION_ID, (calculation, _) -> CalculationRowChanges.changePerson(calculation, "1",
			NormberakningRowInput.create().withCaseworkerDays(10).withNormRowId(2)), false);

		final var member = ArgumentCaptor.forClass(JsonNode.class);
		final var row = ArgumentCaptor.forClass(JsonNode.class);
		verify(client).amountFor(member.capture(), row.capture(), eq(tree("\"2026-09-01\"")), eq(tree("\"2026-09-30\"")));
		assertThat(member.getValue().path("personId").stringValue()).isEqualTo("19880209T050");
		assertThat(member.getValue().path("deviationDays").intValue()).isEqualTo(10);
		// The norm row goes along whole, as the web app sends it.
		assertThat(row.getValue()).isEqualTo(tree("{\"rowId\":2,\"name\":\"Ensamstående 3940.00\",\"monthlyAmount\":3940,\"dailyAmount\":130}"));
		final var sent = objects(sentUpdate(), "calculationPersons").getFirst();
		assertThat(sent.path("normRowId").intValue()).isEqualTo(2);
		assertThat(sent.path("amount").intValue()).isEqualTo(1300);
		assertThat(sent.path("deviationDays").intValue()).isEqualTo(10);
		assertThat(sent.path("daySubscription")).isEqualTo(tree("{\"da\":10,\"Jb\":false,\"Kb\":null,\"hb\":null}"));
	}

	@Test
	void keepsANormintervallAMemberAlreadyHas() {
		readsForEdit(forEdit(savedCalculation()));
		// Lifecare would place the applicant elsewhere; the row it is on stays.
		when(client.placePersons(any())).thenReturn(json("""
			{"calculationPersons":[{"personId":"19880209T050","normRowId":1,"normRow":"Make/maka/sambo","amount":3550}]}"""));

		service.change(ERRAND, CALCULATION_ID, (calculation, _) -> CalculationRowChanges.changeExpense(calculation, "E-3",
			NormberakningRowInput.create().withCaseworkerAmount(BigDecimal.valueOf(5000))), false);

		final var sent = objects(sentUpdate(), "calculationPersons").getFirst();
		assertThat(sent.path("normRowId").intValue()).isEqualTo(2);
		assertThat(sent.path("amount").intValue()).isEqualTo(3940);
		verify(client, never()).amountFor(any(), any(), any(), any());
	}

	@Test
	void setsAnOwnHouseholdSizeAndCountsTheGemensammaKostnader() {
		final var saved = savedCalculation();
		// Norm 1's gemensamma kostnader by household size (capture 2026-09-24).
		saved.set("norm", tree("{\"shared\":[{\"normId\":1,\"noOfMembers\":1,\"monthlyAmount\":1280},{\"normId\":1,\"noOfMembers\":4,\"monthlyAmount\":2030}]}"));
		readsForEdit(forEdit(saved));
		when(client.sharedCost(any(), any(), any())).thenReturn(tree("2030"));

		service.change(ERRAND, CALCULATION_ID, (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit,
			NormHeaderInput.create().withHasCustomHouseholdSize(true).withHouseholdSize(4)), false);

		verify(client).sharedCost(tree("\"2026-09-01\""), tree("\"2026-09-30\""), tree("{\"normId\":1,\"noOfMembers\":4,\"monthlyAmount\":2030}"));
		final var body = sentUpdate();
		assertThat(body.path("hasCustomHouseholdSize").booleanValue()).isTrue();
		assertThat(body.path("householdSize").intValue()).isEqualTo(4);
		assertThat(body.path("saveHouseholdSize").booleanValue()).isTrue();
		assertThat(body.path("amountForHouseholdSize").intValue()).isEqualTo(2030);
		// One member's share of a household of four: 2 030 x 1/4.
		assertThat(body.path("commonHouseholdCost").intValue()).isEqualTo(508);
		assertThat(body.path("HasCustomHouseholdSize").booleanValue()).isTrue();
		assertThat(body.path("HouseholdSize").intValue()).isEqualTo(4);
		assertThat(body.path("NumberOfFamilyMembers").intValue()).isEqualTo(1);
	}

	@Test
	void leavesTheGemensammaKostnaderWhenTheNormHasNoRowForTheSize() {
		readsForEdit(forEdit(savedCalculation()));

		service.change(ERRAND, CALCULATION_ID, (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit,
			NormHeaderInput.create().withHasCustomHouseholdSize(true).withHouseholdSize(4)), false);

		verify(client, never()).sharedCost(any(), any(), any());
		assertThat(sentUpdate().has("amountForHouseholdSize")).isFalse();
	}

	@Test
	void failsWhenLifecareDoesNotCountTheGemensammaKostnader() {
		final var saved = savedCalculation();
		saved.set("norm", tree("{\"shared\":[{\"normId\":1,\"noOfMembers\":4}]}"));
		readsForEdit(forEdit(saved));
		when(client.sharedCost(any(), any(), any())).thenReturn(tree("{}"));

		assertThatThrownBy(() -> service.change(ERRAND, CALCULATION_ID, (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit,
			NormHeaderInput.create().withHasCustomHouseholdSize(true).withHouseholdSize(4)), false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
		verify(client, never()).update(anyInt(), any());
	}

	@Test
	void refusesThePeriodOrANormLifecareDoesNotHave() {
		readsForEdit(forEdit(savedCalculation()));

		assertThatThrownBy(() -> service.change(ERRAND, CALCULATION_ID, (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit,
			NormHeaderInput.create().withCalculationFromDate(LocalDate.of(2026, 9, 2))), false))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		assertThatThrownBy(() -> service.change(ERRAND, CALCULATION_ID, (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit,
			NormHeaderInput.create().withNormId(99)), false))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		verify(client, never()).update(anyInt(), any());
	}

	@Test
	void putsTheBeräkningOnAnotherNormEveryMemberWhereLifecarePlacesThem() {
		final var forEdit = forEdit(savedCalculation());
		forEdit.set("norms", tree("[{\"normId\":1,\"name\":\"Riksnorm 2026\"},{\"normId\":6,\"name\":\"Specnorm\"}]"));
		readsForEdit(forEdit);
		when(client.placePersons(any())).thenReturn(json("{\"calculationPersons\":[{\"personId\":\"19880209T050\",\"normRowId\":1,\"normRow\":\"Make\",\"amount\":2000}]}"));

		service.change(ERRAND, CALCULATION_ID, (calculation, edit) -> CalculationRowChanges.changeHeader(calculation, edit, NormHeaderInput.create().withNormId(6)), false);

		// Sent on their old rows, as the web app does; Lifecare decides what fits the new norm.
		verify(client).placePersons(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("normId").intValue()).isEqualTo(6);
		assertThat(bodyCaptor.getValue().path("calculationPersons").get(0).path("normRowId").intValue()).isEqualTo(2);
		final var body = sentUpdate();
		assertThat(body.path("normId").intValue()).isEqualTo(6);
		assertThat(body.path("normText").stringValue()).isEqualTo("Specnorm");
		assertThat(objects(body, "calculationPersons").getFirst().path("normRowId").intValue()).isEqualTo(1);
		assertThat(objects(body, "calculationPersons").getFirst().path("amount").intValue()).isEqualTo(2000);
		verify(client, never()).amountFor(any(), any(), any(), any());
	}

	@Test
	void takesTheNewNormsRowsAndNamesEachMembersRow() {
		final var forEdit = forEdit(savedCalculation());
		forEdit.set("norms", tree("[{\"normId\":1,\"name\":\"Riksnorm 2026\"},{\"normId\":3,\"name\":\"Norm 3\"}]"));
		readsForEdit(forEdit);
		when(client.placePersons(any())).thenReturn(json("""
			{"calculationPersons":[{"personId":"19880209T050","normRowId":1,"normRow":null,"amount":4380}],
			 "norm":{"rows":[{"rowId":1,"name":"Make/maka/sambo 4380.00","monthlyAmount":4380}]}}"""));

		service.change(ERRAND, CALCULATION_ID, (calculation, edit) -> CalculationRowChanges.changeHeader(calculation, edit, NormHeaderInput.create().withNormId(3)), false);

		final var body = sentUpdate();
		final var member = objects(body, "calculationPersons").getFirst();
		assertThat(member.path("normRowId").intValue()).isEqualTo(1);
		assertThat(member.path("normRow").stringValue()).isEqualTo("Make/maka/sambo");
		assertThat(member.path("amount").intValue()).isEqualTo(4380);
		assertThat(body.path("norm").path("rows").get(0).path("rowId").intValue()).isEqualTo(1);
	}

	@Test
	void leavesAMemberLifecareDoesNotPlaceOnTheNewNormWithoutANormintervall() {
		final var forEdit = forEdit(savedCalculation());
		forEdit.set("norms", tree("[{\"normId\":1,\"name\":\"Riksnorm 2026\"},{\"normId\":3,\"name\":\"Matnorm 2026\"}]"));
		readsForEdit(forEdit);
		when(client.placePersons(any())).thenReturn(json("{\"calculationPersons\":[{\"personId\":\"19880209T050\",\"normRowId\":0,\"normRow\":null,\"amount\":0}],\"norm\":null}"));

		service.change(ERRAND, CALCULATION_ID, (calculation, edit) -> CalculationRowChanges.changeHeader(calculation, edit, NormHeaderInput.create().withNormId(3)), false);

		final var member = objects(sentUpdate(), "calculationPersons").getFirst();
		// Sent unplaced the way the web app sends such a member: no normintervall at all.
		assertThat(member.path("amount").intValue()).isZero();
		assertThat(member.has("normRowId")).isFalse();
		verify(client, never()).amountFor(any(), any(), any(), any());
	}

	@Test
	void savesAsSlutlig() {
		readsForEdit(forEdit(savedCalculation()));

		service.change(ERRAND, CALCULATION_ID, (calculation, _) -> calculation, true);

		assertThat(sentUpdate().path("isFinalized").booleanValue()).isTrue();
		verify(recorder).written(ERRAND, "UPDATE", "CALCULATION", "Sparade normberäkningen som slutlig i Lifecare", "30");
	}

	@Test
	void refusesAnyChangeToABeräkningLifecareHoldsAsSlutlig() {
		readsForEdit(forEdit(savedCalculation().put("isFinalized", true)));

		assertThatThrownBy(() -> service.change(ERRAND, CALCULATION_ID, (calculation, _) -> calculation, false))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("slutlig");
		verify(client, never()).update(anyInt(), any());
		verifyNoInteractions(recorder);
	}

	@Test
	void failsWhenLifecareAnswersWithoutABeräkning() {
		readsForEdit(json("{}"));

		assertThatThrownBy(() -> service.change(ERRAND, CALCULATION_ID, (calculation, _) -> calculation, false))
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void refusesAChangeOnAnErrandWithoutInsats() {
		final var withoutInsats = new LifecareErrand("2281", "FINANCIAL_ASSISTANCE", "e1", null, 30, null, null, null, null);

		assertThatThrownBy(() -> service.change(withoutInsats, CALCULATION_ID, (calculation, _) -> calculation, false))
			.hasFieldOrPropertyWithValue("status", CONFLICT);
		verify(client, never()).readForEdit(anyInt());
	}

	@Test
	void showsTheSavedBeräkningInTheTabsShape() {
		readsForEdit(forEdit(savedCalculation()));

		final var view = service.readDraftView(ERRAND, CALCULATION_ID);

		assertThat(view.getSource()).isEqualTo("LIFECARE");
		assertThat(view.getApplicationMonth()).isEqualTo("2026-09");
		assertThat(view.getIncomes().getFirst().getApplicantCaseworkerAmount()).isEqualByComparingTo("5000");
		verify(recorder).read(ERRAND, "CALCULATION", "Läste normberäkningen i Lifecare", "30");
	}

	@Test
	void offersLifecaresOwnTypes() {
		readsForEdit(forEdit(savedCalculation()));

		final var types = service.readTypes(ERRAND, CALCULATION_ID);

		assertThat(types.getNorms()).singleElement().satisfies(norm -> assertThat(norm.getDisplayName()).isEqualTo("Riksnorm 2026"));
		assertThat(types.getIncomeTypes()).singleElement().satisfies(type -> assertThat(type.getCode()).isEqualTo("1"));
		assertThat(types.getCostTypes()).singleElement().satisfies(type -> assertThat(type.getDisplayName()).isEqualTo("Boendekostnad"));
		assertThat(types.getLivingCostTypes()).isEmpty();
		verify(recorder).read(ERRAND, "CALCULATION", "Läste normberäkningen i Lifecare", "30");
	}

	@Test
	void namesTheApplicationMonth() {
		assertThat(LifecareCalculationEditService.applicationMonthOf(ERRAND)).isEqualTo("2026-09");
		assertThat(LifecareCalculationEditService.applicationMonthOf(new LifecareErrand("2281", "n", "e", 1, null, null, null, 2026, null))).isNull();
		assertThat(LifecareCalculationEditService.applicationMonthOf(new LifecareErrand("2281", "n", "e", 1, null, null, null, null, 9))).isNull();
	}
}
