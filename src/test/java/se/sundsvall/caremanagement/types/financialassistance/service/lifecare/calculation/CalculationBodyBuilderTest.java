package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.HouseholdSize;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.applyDraft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.blank;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.catalogues;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.draft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.personsOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.householdSizeOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withPlacedPersons;

class CalculationBodyBuilderTest {

	/** Filled from the draft and placed by Lifecare (PlacePersons, capture 2026-09-24): Riksnorm, Ensamstående, 3 940. */
	private static ObjectNode placed() {
		final var filled = applyDraft(blank(), List.of(), draft(), personsOf(draft(), "880209-T050"), catalogues(), "2026-09-24");
		return withPlacedPersons(filled, List.of(json("""
			{"personId":"19880209T050","included":true,"normRowId":2,"normRow":null,"amount":3940}""")), true);
	}

	private static List<String> keys(final ObjectNode node) {
		return new ArrayList<>(node.propertyNames());
	}

	@Test
	void sendsANewBeräkningTheWayTheWebAppDoes() {
		final var calculation = placed();

		final var body = CalculationBodyBuilder.create(calculation, householdSizeOf(calculation, false, null));

		final var persons = objects(body, "calculationPersons");
		assertThat(persons.get(0)).isEqualTo(json("""
			{"calculationId":0,"personId":"19880209T050","personKey":0,"name":"Testsson, Test","normRowId":2,"normRow":null,"amount":3940,
			 "deviationFromDate":"","deviationToDate":"","deviationDays":null,"lunchDeduction":0,"included":true,"birthDate":"","relationType":0,
			 "isBonusChild":"","personIdFormatted":"880209-T050","isValid":true,
			 "normSubscription":{"da":2,"Jb":false,"Kb":null,"hb":null},
			 "dateSubscriptions":[{"da":"","Jb":false,"Kb":null,"hb":null},{"da":"","Jb":false,"Kb":null,"hb":null}],
			 "daySubscription":{"da":null,"Jb":false,"Kb":null,"hb":null}}"""));
		assertThat(keys(persons.get(0)).subList(16, 20)).containsExactly("isValid", "normSubscription", "dateSubscriptions", "daySubscription");
		// A member never placed goes without a norm row at all.
		assertThat(persons.get(1).has("normRowId")).isFalse();
		assertThat(persons.get(1).path("normSubscription")).isEqualTo(tree("{\"Jb\":false,\"Kb\":null,\"hb\":null}"));
		assertThat(objects(body, "calculationIncomes")).allSatisfy(row -> {
			assertThat(row.path("changeable").booleanValue()).isTrue();
			assertThat(row.path("isValid").booleanValue()).isTrue();
		});
		assertThat(objects(body, "calculationExpenses")).allSatisfy(row -> {
			assertThat(row.path("changeable").booleanValue()).isTrue();
			assertThat(row.has("isValid")).isFalse();
		});
		assertThat(objects(body, "calculationSpecialExpenses")).allSatisfy(row -> assertThat(row.path("changeable").booleanValue()).isTrue());
		assertThat(body.has("aktualiseringId")).isFalse();
		assertThat(body.has("householdSize")).isFalse();
		assertThat(body.has("numberOfFamilyMembers")).isFalse();
		final var keys = keys(body);
		assertThat(keys.subList(keys.size() - 3, keys.size())).containsExactly("HasCustomHouseholdSize", "HouseholdSize", "NumberOfFamilyMembers");
		assertThat(body.path("HasCustomHouseholdSize").booleanValue()).isFalse();
		assertThat(body.path("HouseholdSize").intValue()).isEqualTo(1);
		assertThat(body.path("NumberOfFamilyMembers").intValue()).isEqualTo(1);
		// The beräkning handed in is not changed.
		assertThat(objects(calculation, "calculationPersons").getFirst().has("isValid")).isFalse();
	}

	@Test
	void keepsTheOrderOfLifecaresFields() {
		final var calculation = placed();

		final var body = CalculationBodyBuilder.create(calculation, new HouseholdSize(false, 1, 1));

		assertThat(keys(body).subList(0, 6)).containsExactly("calculationId", "serviceId", "investigationId", "normId", "normText", "date");
	}

	@Test
	void sendsTheMembersDaysInItsSubscription() {
		final var person = json("""
			{"personId":"x","normRowId":3,"deviationDays":10}""");

		final var sent = CalculationBodyBuilder.asSentPerson(person);

		assertThat(sent.path("daySubscription").path("da").intValue()).isEqualTo(10);
		assertThat(sent.path("normSubscription").path("da").intValue()).isEqualTo(3);
		assertThat(CalculationBodyBuilder.asSentPerson(json("{\"personId\":\"y\"}")).path("daySubscription").path("da").isNull()).isTrue();
	}

	@Test
	void savesAsSlutligWithTheSameUpdate() {
		final var calculation = placed();
		calculation.put("calculationId", 30);

		final var body = CalculationBodyBuilder.update(calculation, new HouseholdSize(true, 4, 3), true);

		assertThat(body.path("isFinalized").booleanValue()).isTrue();
		assertThat(objects(body, "calculationIncomes")).allSatisfy(row -> assertThat(row.path("isValid").booleanValue()).isTrue());
		assertThat(body.path("HasCustomHouseholdSize").booleanValue()).isTrue();
		assertThat(body.path("HouseholdSize").intValue()).isEqualTo(4);
		assertThat(body.path("NumberOfFamilyMembers").intValue()).isEqualTo(3);
	}

	@Test
	void sendsAChangeWithTheHouseholdSizeWhereLifecareReadItAndAtTheEnd() {
		final var calculation = placed();
		calculation.put("calculationId", 31);

		final var body = CalculationBodyBuilder.update(calculation, new HouseholdSize(true, 3, 1), false);

		assertThat(body.path("calculationId").intValue()).isEqualTo(31);
		assertThat(body.path("aktualiseringId").intValue()).isZero();
		assertThat(body.path("householdSize").intValue()).isEqualTo(3);
		assertThat(body.path("numberOfFamilyMembers").intValue()).isEqualTo(1);
		assertThat(body.path("isFinalized").booleanValue()).isFalse();
		final var keys = keys(body);
		assertThat(keys.subList(keys.size() - 3, keys.size())).containsExactly("HasCustomHouseholdSize", "HouseholdSize", "NumberOfFamilyMembers");
	}

	@Test
	void keepsASlutligBeräkningSlutlig() {
		final var calculation = placed();
		calculation.put("isFinalized", true);

		assertThat(CalculationBodyBuilder.update(calculation, new HouseholdSize(false, 1, 1), false).path("isFinalized").booleanValue()).isTrue();
	}
}
