package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.DraftPerson;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lifecare trees and careM drafts the tests share, modelled on the captures (2026-09-24) Drakel's BFF tests use. No
 * real personal data: the personnummer are Skatteverket-style test numbers with T.
 */
final class CalculationFixtures {

	static final JsonMapper JSON = JsonMapper.builder().build();

	private CalculationFixtures() {}

	static ObjectNode json(final String text) {
		return (ObjectNode) JSON.readTree(text);
	}

	static JsonNode tree(final String text) {
		return JSON.readTree(text);
	}

	static String applicantPerson(final boolean included, final int normRowId, final int amount) {
		return """
			{"calculationId":0,"personId":"19880209T050","personKey":0,"name":"Testsson, Test","normRowId":%d,"normRow":null,"amount":%d,
			 "deviationFromDate":"","deviationToDate":"","deviationDays":null,"lunchDeduction":0,"included":%s,"birthDate":"","relationType":0,
			 "isBonusChild":"","personIdFormatted":"880209-T050"}""".formatted(normRowId, amount, included);
	}

	static String childPerson(final boolean included) {
		return """
			{"calculationId":0,"personId":"20141201T010","personKey":0,"name":"Testbarn Test, Testar","normRowId":0,"normRow":null,"amount":0,
			 "deviationFromDate":"","deviationToDate":"","deviationDays":null,"lunchDeduction":0,"included":%s,"birthDate":"","relationType":0,
			 "isBonusChild":"","personIdFormatted":"141201-T010"}""".formatted(included);
	}

	/** Calculation/GetProposalService?businessType=8&businessId=1, trimmed to what matters. */
	static ObjectNode blank() {
		return json("""
			{"calculationId":0,"serviceId":1,"investigationId":0,"aktualiseringId":0,"normId":1,"normText":null,"date":"2026-09-24",
			 "startDate":"","endDate":"","calculationSummary":null,
			 "calculationPersons":[%s,%s],
			 "calculationIncomes":[{"calculationId":0,"serialNumber":0,"incomeType":"Lön efter skatt","amountApplicant":0,"amountCoApplicant":0,"incomeCode":1,"changeable":false}],
			 "calculationExpenses":[{"calculationId":0,"serialNumber":0,"expenseCode":8,"expenseType":"A-kasseavgift","appliedAmount":0,"approvedAmount":0,"note":null,
			   "changeable":false,"markForCopy":false,"showMarkForCopy":false}],
			 "calculationSpecialExpenses":[],"householdSize":0,"hasCustomHouseholdSize":false,"isFinalized":false,"numberOfFamilyMembers":0,"updateTimestamp":""}"""
			.formatted(applicantPerson(false, 0, 0), childPerson(false)));
	}

	static ObjectNode catalogues() {
		return json("""
			{"norms":[{"normId":1,"name":"Riksnorm 2026"},{"normId":3,"name":"Nettonorm 2026"}],
			 "incomeTypes":[{"id":1,"text":"Lön efter skatt","isActive":true},{"id":27,"text":"Efterlevandestöd","isActive":true}],
			 "expenseTypes":[{"id":8,"text":"A-kasseavgift","isActive":true},{"id":3,"text":"Boendekostnad","isActive":true}],
			 "specialExpenseTypes":[{"id":7,"text":"Tandvård","isActive":true}]}""");
	}

	static CalculationDraft draft() {
		return CalculationDraft.create()
			.withNormId(1)
			.withCalculationFromDate(LocalDate.of(2026, 9, 1))
			.withCalculationToDate(LocalDate.of(2026, 9, 30))
			.withHasCustomHouseholdSize(false)
			.withPersons(new ArrayList<>(List.of(
				NormPersonRow.create().withRole("APPLICANT").withName("Test Testsson").withIncluded(true).withPartyId("p1"),
				NormPersonRow.create().withRole("CHILD").withName("Testar").withIncluded(false).withPartyId("p2"))))
			.withIncomes(new ArrayList<>(List.of(NormIncomeRow.create().withTypeId(27).withTypeName("Efterlevandestöd").withApplicantEffectiveAmount(BigDecimal.valueOf(1500)))))
			.withExpenses(new ArrayList<>(List.of(
				NormExpenseRow.create().withBucket("EXPENSE").withCostTypeDisplayName("Boendekostnad").withAppliedAmount(BigDecimal.valueOf(6000)).withEffectiveAmount(BigDecimal.valueOf(5500)),
				NormExpenseRow.create().withBucket("EXPENSE").withCostTypeDisplayName("A-kasseavgift").withEffectiveAmount(BigDecimal.valueOf(454)).withSpecification("jkljkl"))))
			.withSpecialExpenses(new ArrayList<>(List.of(
				NormExpenseRow.create().withBucket("SPECIAL_EXPENSE").withCostTypeDisplayName("Tandvård").withEffectiveAmount(BigDecimal.valueOf(300)))));
	}

	static List<DraftPerson> personsOf(final CalculationDraft draft, final String... personalNumbers) {
		final var persons = new ArrayList<DraftPerson>();
		for (var index = 0; index < draft.getPersons().size(); index++) {
			String number = null;
			if (index < personalNumbers.length) {
				number = personalNumbers[index];
			}
			persons.add(new DraftPerson(draft.getPersons().get(index), number));
		}
		return persons;
	}

	/** Beräkning 30 as read for edit: Lön efter skatt 5 000 gross, counted 3 750 with 25 % jobbstimulans. */
	static ObjectNode savedCalculation() {
		return json("""
			{"calculationId":30,"normId":1,"normText":"Riksnorm 2026","date":"2026-09-24","startDate":"2026-09-01","endDate":"2026-09-30",
			 "calculationSummary":null,
			 "calculationPersons":[{"personId":"19880209T050","personKey":1,"name":"Testsson, Test","normRowId":2,"normRow":"Ensamstående","amount":3940,
			   "included":true,"deviationFromDate":"","deviationToDate":"","personIdFormatted":"880209-T050"}],
			 "calculationIncomes":[{"incomeCode":1,"incomeType":"Lön efter skatt","amountApplicant":3750,"grossAmountApplicant":5000,"amountCoApplicant":0}],
			 "calculationExpenses":[{"expenseCode":3,"expenseType":"Boendekostnad","appliedAmount":5000,"approvedAmount":5000,"note":""}],
			 "calculationSpecialExpenses":[],"hasCustomHouseholdSize":false,"isFinalized":false,"updateTimestamp":"2026-09-24","hasApplicantJobStimuli":true}""");
	}

	static ObjectNode forEdit(final ObjectNode calculation) {
		final var forEdit = json("""
			{"norms":[{"normId":1,"name":"Riksnorm 2026"}],
			 "incomeTypes":[{"id":1,"text":"Lön efter skatt","isActive":true,"isJobStimulus":true,"jobStimulusPercent":25}],
			 "expenseTypes":[{"id":3,"text":"Boendekostnad","isActive":true}],
			 "specialExpenseTypes":[]}""");
		forEdit.set("calculation", calculation);
		return forEdit;
	}
}
