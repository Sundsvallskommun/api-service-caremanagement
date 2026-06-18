package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;

/**
 * Builds the freshly computed process rows for the three normberäkning sections from the errand: the income rows from
 * the operaton-classified incomes, the expense rows from the application's costs (each capped by the
 * {@link ExpenseRegelverkService}) and the person rows from the household. Also compares the household against the
 * previous normberäkning in Lifecare to produce drift warnings. Every row is stamped {@code origin = SYSTEM}; the
 * {@link DraftService} merge then refreshes only the process columns.
 */
@Service
public class NormberakningFeeder {

	private static final int FULL_MONTH_DAYS = 30;

	private final ExpenseRegelverkService expenseRegelverkService;

	NormberakningFeeder(final ExpenseRegelverkService expenseRegelverkService) {
		this.expenseRegelverkService = expenseRegelverkService;
	}

	/** The fresh income process rows — one per (FC income type, recipient). */
	public List<FaNormIncomeEntity> incomeRows(final String errandId, final List<FcIncomeLine> lines) {
		return ofNullable(lines).orElseGet(List::of).stream()
			.map(line -> FaNormIncomeEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withTypeId(line.typeId()).withTypeName(line.typeName()).withRecipient(line.recipient())
				.withProcessAmount(line.amount()).withProcessAmountDate(line.date()))
			.toList();
	}

	/** The fresh expense process rows — one per applied cost, the process amount being the regelverk cap. */
	public List<FaNormExpenseEntity> expenseRows(final String municipalityId, final String errandId, final FinancialAssistanceEntity errand) {
		final var housingForm = errand.getHousingForm();
		final var housingPersonCount = errand.getHousingPersonCount();
		final var normType = errand.getNormType();

		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.map(cost -> {
				final var cap = expenseRegelverkService.cap(municipalityId, cost.getCostType(), cost.getOtherSubType(),
					housingForm, housingPersonCount, normType, cost.getAppliedAmount());
				return FaNormExpenseEntity.create()
					.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
					.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
					.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(cap);
			})
			.toList();
	}

	/** The fresh person process rows — applicant + co-applicant (full month) and each child (days in the home). */
	public List<FaNormPersonEntity> personRows(final String errandId, final FinancialAssistanceEntity errand) {
		final var rows = new ArrayList<FaNormPersonEntity>();

		ofNullable(errand.getPersons()).orElseGet(List::of).forEach(person -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(person.getPartyId()).withRole(person.getRole()).withProcessDays(FULL_MONTH_DAYS)));

		ofNullable(errand.getChildren()).orElseGet(List::of).forEach(child -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(child.getPartyId()).withRole(ROLE_CHILD).withName(childName(child.getFirstName(), child.getLastName()))
			.withProcessDays(child.getDaysInHome() != null ? child.getDaysInHome() : FULL_MONTH_DAYS)));

		return rows;
	}

	/**
	 * Household-drift warnings against the previous normberäkning in Lifecare — members that were present then but not now,
	 * and a changed member count. New members are surfaced separately as NEW_PERSON warnings from the merge.
	 */
	public List<String> householdWarnings(final List<FaNormPersonEntity> currentPersons, final PreviousHousehold previous) {
		if ((previous == null) || (previous.memberCount() == 0)) {
			return List.of();
		}

		final var currentIds = ofNullable(currentPersons).orElseGet(List::of).stream()
			.map(FaNormPersonEntity::getPartyId).filter(id -> (id != null) && !id.isBlank()).collect(Collectors.toSet());

		final var warnings = new ArrayList<String>();
		previous.personIds().stream()
			.filter(id -> !currentIds.contains(id))
			.forEach(id -> warnings.add("Hushållsmedlem från föregående normberäkning saknas nu: " + id));

		if (previous.memberCount() != currentIds.size()) {
			warnings.add("Antal hushållsmedlemmar har ändrats sedan föregående normberäkning (föregående "
				+ previous.memberCount() + ", nu " + currentIds.size() + ")");
		}
		return List.copyOf(warnings);
	}

	private static String childName(final String firstName, final String lastName) {
		return Stream.of(firstName, lastName)
			.filter(part -> (part != null) && !part.isBlank())
			.collect(Collectors.joining(" "));
	}
}
