package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.RECIPIENT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.RECIPIENT_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_UMGANGESBARN;

/**
 * Builds the freshly computed process rows for the normberäkning sections from the errand: the income rows (one per FC
 * income type, with a sökande and medsökande side) from the operaton-classified incomes, the expense rows from the
 * application's costs — each given a process amount + bucket by the {@link ExpenseRegelverkService} — and the person
 * rows from the household (umgängesbarn = part-time children). Also compares the household against the previous
 * normberäkning in Lifecare to produce drift warnings. Every row is stamped {@code origin = SYSTEM}; the
 * {@link DraftService} merge then refreshes only the process columns.
 */
@Service
public class NormberakningFeeder {

	private static final int FULL_MONTH_DAYS = 30;
	private static final String RESIDENCE_FULL_TIME = "FULL_TIME";

	private final ExpenseRegelverkService expenseRegelverkService;

	NormberakningFeeder(final ExpenseRegelverkService expenseRegelverkService) {
		this.expenseRegelverkService = expenseRegelverkService;
	}

	/**
	 * The fresh income process rows — one per FC income type, the classified lines folded into a sökande + medsökande side.
	 */
	public List<FaNormIncomeEntity> incomeRows(final String errandId, final List<FcIncomeLine> lines) {
		final var byType = ofNullable(lines).orElseGet(List::of).stream()
			.filter(line -> line.typeId() != null)
			.collect(Collectors.groupingBy(FcIncomeLine::typeId, LinkedHashMap::new, Collectors.toList()));

		return byType.entrySet().stream().map(entry -> {
			final var group = entry.getValue();
			final var applicant = group.stream().filter(line -> RECIPIENT_APPLICANT.equals(line.recipient())).findFirst();
			final var coApplicant = group.stream().filter(line -> RECIPIENT_CO_APPLICANT.equals(line.recipient())).findFirst();
			final var any = group.getFirst();
			return FaNormIncomeEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withTypeId(entry.getKey()).withTypeName(any.typeName())
				.withApplicantProcessAmount(applicant.map(FcIncomeLine::amount).orElse(null))
				.withApplicantAmountDate(applicant.map(FcIncomeLine::date).orElse(null))
				.withCoapplicantProcessAmount(coApplicant.map(FcIncomeLine::amount).orElse(null))
				.withCoapplicantAmountDate(coApplicant.map(FcIncomeLine::date).orElse(null))
				.withNote(any.note());
		}).toList();
	}

	/** The fresh expense process rows — one per applied cost, the process amount + bucket coming from the regelverk. */
	public List<FaNormExpenseEntity> expenseRows(final String municipalityId, final String errandId, final FinancialAssistanceEntity errand) {
		final var housingForm = errand.getHousingForm();
		final var housingPersonCount = errand.getHousingPersonCount();
		final var normType = errand.getNormType();

		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.map(cost -> {
				final var verdict = expenseRegelverkService.verdict(municipalityId, cost.getCostType(), cost.getOtherSubType(),
					housingForm, housingPersonCount, normType, cost.getAppliedAmount());
				return FaNormExpenseEntity.create()
					.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
					.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
					.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(verdict.processAmount()).withBucket(verdict.bucket());
			})
			.toList();
	}

	/**
	 * The fresh person process rows — applicant + co-applicant (full month) and each child (days in the home; a part-time
	 * child becomes an umgängesbarn). All start {@code included = true}; the handläggare may later exclude one (omfattas).
	 */
	public List<FaNormPersonEntity> personRows(final String errandId, final FinancialAssistanceEntity errand) {
		final var rows = new ArrayList<FaNormPersonEntity>();

		ofNullable(errand.getPersons()).orElseGet(List::of).forEach(person -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(person.getPartyId()).withRole(person.getRole()).withProcessDays(FULL_MONTH_DAYS).withIncluded(true)));

		ofNullable(errand.getChildren()).orElseGet(List::of).forEach(child -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(child.getPartyId()).withRole(childRole(child.getResidenceExtent())).withName(childName(child.getFirstName(), child.getLastName()))
			.withProcessDays(child.getDaysInHome() != null ? child.getDaysInHome() : FULL_MONTH_DAYS).withIncluded(true)));

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

	/** A full-time child is a CHILD; a part-time / övrigt child is an umgängesbarn. */
	private static String childRole(final String residenceExtent) {
		return ((residenceExtent == null) || RESIDENCE_FULL_TIME.equals(residenceExtent)) ? ROLE_CHILD : ROLE_UMGANGESBARN;
	}

	private static String childName(final String firstName, final String lastName) {
		return Stream.of(firstName, lastName)
			.filter(part -> (part != null) && !part.isBlank())
			.collect(Collectors.joining(" "));
	}
}
