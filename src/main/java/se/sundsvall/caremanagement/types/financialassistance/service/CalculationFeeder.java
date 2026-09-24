package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousFamily;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.costDisplayName;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.roleDisplayName;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.RECIPIENT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.RECIPIENT_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_VISITATION_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_COMMON_HOUSEHOLD_COST_CHECK;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_FAMILY_DEVIATING_PERIOD;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_FAMILY_DIFFERS_FROM_APPLICATION;

/**
 * Builds the freshly computed process rows for the calculation sections from the errand: the income rows (one per
 * FamilyCare income type, with a applicant and co-applicant side) from the operaton-classified incomes, the expense
 * rows from the application's costs — each given a process amount + bucket by the {@link ExpenseRulesService} — and
 * the person rows from the household (visitation child = part-time children). Also compares the housing cost against
 * the
 * previous calculation in Lifecare to produce a drift warning. Every row is stamped {@code origin = SYSTEM}; the {@link
 * DraftService} merge then refreshes only the process columns.
 */
@Service
public class CalculationFeeder {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationFeeder.class);

	private static final int FULL_MONTH_DAYS = 30;
	private static final String RESIDENCE_FULL_TIME = "FULL_TIME";
	private static final String COST_TYPE_RENT = "RENT";
	private static final String CHANGE_HOUSING_COST = "HOUSING_COST";
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	static final String SOURCE_KEY_FAMILY_NOT_COPIED = "family-not-copied";
	static final String SOURCE_KEY_COMMON_HOUSEHOLD_COST = "common-household-cost";
	static final String WARNING_FAMILY_NOT_COPIED = "Familjen i föregående normberäkning kunde inte läsas helt – hushållet är taget från ansökan, kontrollera det mot Lifecare";
	static final String WARNING_NOT_IN_APPLICATION = "%s finns i föregående normberäkning men inte i ansökan – kontrollera om personen ska ingå i beräkningen";
	static final String WARNING_NOT_IN_PREVIOUS = "%s finns i ansökan men inte i föregående normberäkning – lägg till personen i beräkningen om den ska ingå";
	static final String WARNING_DEVIATING_PERIOD = "%s ingick i föregående normberäkning med avvikande period %s–%s – kontrollera omfattningen";
	static final String WARNING_COMMON_HOUSEHOLD_COST = "Föregående normberäkning hade gemensamma hushållskostnader på %s kronor för %d personer, utkastet har %d – kontrollera hushållsstorleken";

	private final ExpenseRulesService expenseRulesService;
	private final RenewalDeltaService renewalDeltaService;
	private final StakeholderService stakeholderService;

	CalculationFeeder(final ExpenseRulesService expenseRulesService, final RenewalDeltaService renewalDeltaService, final StakeholderService stakeholderService) {
		this.expenseRulesService = expenseRulesService;
		this.renewalDeltaService = renewalDeltaService;
		this.stakeholderService = stakeholderService;
	}

	/** The fresh expense process rows plus the expense warnings the rules raised for them. */
	public record ExpenseFeed(List<FaNormExpenseEntity> rows, List<WarningService.WarningInput> warnings) {}

	/**
	 * The fresh income process rows — one per FamilyCare income type, the classified lines folded into a applicant +
	 * co-applicant side. Within each (FamilyCare type, recipient) the amounts are summed: the SSBTEK/classified path
	 * arrives pre-summed (one line per recipient), but the application/new-application path emits one line per raw
	 * declared income, so two same-type same-recipient incomes (e.g. two OTHER_INCOME) must be added together rather than
	 * dropping all but the first — else the income is understated and the computed benefit inflated. The first line in a
	 * recipient group supplies the non-amount fields (date, type name, note).
	 */
	public List<FaNormIncomeEntity> incomeRows(final String errandId, final List<FamilyCareIncomeLine> lines) {
		final var byType = ofNullable(lines).orElseGet(List::of).stream()
			.filter(line -> line.typeId() != null)
			.collect(Collectors.groupingBy(FamilyCareIncomeLine::typeId, LinkedHashMap::new, Collectors.toList()));

		return byType.entrySet().stream().map(entry -> {
			final var group = entry.getValue();
			final var applicant = recipientLines(group, RECIPIENT_APPLICANT);
			final var coApplicant = recipientLines(group, RECIPIENT_CO_APPLICANT);
			final var any = group.getFirst();
			return FaNormIncomeEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withTypeId(entry.getKey()).withTypeName(any.typeName())
				.withApplicantProcessAmount(sumAmounts(applicant))
				.withApplicantAmountDate(firstDate(applicant))
				.withCoapplicantProcessAmount(sumAmounts(coApplicant))
				.withCoapplicantAmountDate(firstDate(coApplicant))
				.withNote(any.note());
		}).toList();
	}

	/** The lines in a type group belonging to a single recipient, in encounter order. */
	private static List<FamilyCareIncomeLine> recipientLines(final List<FamilyCareIncomeLine> group, final String recipient) {
		return group.stream().filter(line -> recipient.equals(line.recipient())).toList();
	}

	/**
	 * The summed amount across a recipient's lines, or {@code null} when the recipient has no lines at all. Null
	 * individual amounts are skipped; a recipient with only null-amount lines therefore sums to zero.
	 */
	private static BigDecimal sumAmounts(final List<FamilyCareIncomeLine> lines) {
		if (lines.isEmpty()) {
			return null;
		}
		return lines.stream()
			.map(FamilyCareIncomeLine::amount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * The first line's amount date for a recipient (the non-amount fields come from the first line), {@code null} when
	 * none.
	 */
	private static OffsetDateTime firstDate(final List<FamilyCareIncomeLine> lines) {
		return lines.stream().findFirst().map(FamilyCareIncomeLine::date).orElse(null);
	}

	/**
	 * The fresh expense process rows — one per applied cost, the process amount + bucket coming from the rules — plus
	 * the expense warnings the rules raised: a manual reasonableness assessment ({@code EXPENSE_REVIEW}) for a flagged
	 * cost, and a cap ({@code EXPENSE_CAPPED}) when the process amount lands below what the citizen applied for. The
	 * decision is evaluated once per cost; the verdict feeds both the row and its warnings.
	 */
	public ExpenseFeed expenseFeed(final String municipalityId, final String errandId, final FinancialAssistanceEntity errand,
		final Map<String, BigDecimal> previousAmounts, final Integer applicantAge) {

		final var childCount = ofNullable(errand.getChildren()).orElseGet(List::of).size();
		final var householdCount = householdSize(errand);
		final var previous = ofNullable(previousAmounts).orElseGet(Map::of);

		final var rows = new ArrayList<FaNormExpenseEntity>();
		final var warnings = new ArrayList<WarningService.WarningInput>();

		ofNullable(errand.getCosts()).orElseGet(List::of).forEach(cost -> {
			final var previousApproved = previous.get(cost.getCostType());
			final var verdict = expenseRulesService.verdict(municipalityId, cost.getCostType(), cost.getAppliedAmount(),
				previousApproved, applicantAge, childCount, householdCount);
			rows.add(FaNormExpenseEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
				.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(verdict.processAmount()).withBucket(verdict.bucket()));
			warnings.addAll(expenseWarnings(cost, verdict));
		});

		return new ExpenseFeed(List.copyOf(rows), List.copyOf(warnings));
	}

	/** The number of persons in the household — the declared housingPersonCount, else persons + children. */
	private static Integer householdSize(final FinancialAssistanceEntity errand) {
		final var declared = errand.getHousingPersonCount();
		if (declared != null) {
			return declared;
		}
		final var persons = ofNullable(errand.getPersons()).orElseGet(List::of).size();
		final var children = ofNullable(errand.getChildren()).orElseGet(List::of).size();
		return persons + children;
	}

	/** The warnings a single cost's verdict raises — a manual review flag and/or a cap below the applied amount. */
	private static List<WarningService.WarningInput> expenseWarnings(final FaCost cost, final ExpenseRulesService.ExpenseVerdict verdict) {
		final var warnings = new ArrayList<WarningService.WarningInput>();
		final var sourceKey = expenseSourceKey(cost);
		final var label = expenseLabel(cost);

		if (verdict.warning()) {
			final var reason = ofNullable(verdict.rule()).filter(text -> !text.isBlank()).orElse("Utgiften kräver en manuell skälighetsbedömning");
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, sourceKey, label + ": " + reason));
		}
		if (isCapped(cost.getAppliedAmount(), verdict.processAmount())) {
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_CAPPED, sourceKey,
				"Kapad kostnad: " + label + " – ansökt " + plain(cost.getAppliedAmount()) + " kr, beviljat " + plain(verdict.processAmount()) + " kr"));
		}
		return warnings;
	}

	private static boolean isCapped(final BigDecimal applied, final BigDecimal process) {
		return (applied != null) && (process != null) && (process.compareTo(applied) < 0);
	}

	/** Stable dedup key for the cost a warning concerns — cost type, plus the other sub-type when present. */
	private static String expenseSourceKey(final FaCost cost) {
		final var sub = cost.getOtherSubType();
		if ((sub == null) || sub.isBlank()) {
			return orEmpty(cost.getCostType());
		}
		return orEmpty(cost.getCostType()) + ":" + sub;
	}

	private static String expenseLabel(final FaCost cost) {
		final var label = ofNullable(costDisplayName(cost.getCostType())).orElseGet(() -> orEmpty(cost.getCostType()));
		final var sub = cost.getOtherSubType();
		if ((sub == null) || sub.isBlank()) {
			return label;
		}
		return label + " (" + sub + ")";
	}

	/** Plain (no scientific notation, no trailing zeros) rendering of a non-null amount for warning messages. */
	private static String plain(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	/** The value, or an empty string when it is {@code null}. */
	private static String orEmpty(final String value) {
		return ofNullable(value).orElse("");
	}

	/**
	 * The fresh person process rows — applicant + co-applicant (full month) and each child (days in the home; a
	 * part-time child becomes an visitation child). All start {@code included = true}; the caseworker may later exclude
	 * one (is included).
	 *
	 * <p>
	 * The applicant and co-applicant carry no name in the application payload, so the name is taken from the errand's
	 * stakeholders (the promoted identities). Without it the row — and every warning written from it — would have
	 * nothing but a party id to name the person by, which is an identifier, not something a handläggare reads. A
	 * stakeholder read that fails or finds nothing leaves the name null; the row then falls back to its role label.
	 * </p>
	 *
	 * <p>
	 * The Belopp column — the member's own share of the norm — is carried over from {@code previousAmounts} (the
	 * previous Lifecare calculation, keyed by party id). The norm is computed in Lifecare, so this is the only figure
	 * careM can show before the draft is committed; a member the previous calculation did not cover keeps none rather
	 * than being given one that was never calculated.
	 * </p>
	 *
	 * <p>
	 * <strong>Who is in the household</strong> follows the återansökan regelverk: “Kopiera följande från föregående
	 * normberäkning i Lifecare: Norm, Familj, Gemensamma kostnader”. With a previous calculation, the rows are its
	 * members. What FamilyCare's read model does not carry comes from the application where the application knows the
	 * person — the role, and a child's days in the home — and is otherwise left at its default (no role, the whole month)
	 * and flagged by {@link #familyWarnings}; a member in the application but not in the previous calculation is flagged
	 * there too, not added. Without a previous calculation (a first application), or when one of its members could not
	 * be identified, the household is the application's, as before: a list short of a person it cannot name must not
	 * silently become the calculation.
	 * </p>
	 *
	 * @param previousAmounts the previous calculation's norm amount per party id, empty when there is no history
	 * @param previousFamily  the previous calculation's family, empty when there is no history
	 */
	public List<FaNormPersonEntity> personRows(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceEntity errand,
		final Map<String, BigDecimal> previousAmounts, final PreviousFamily previousFamily) {

		final var names = householdNames(municipalityId, namespace, errandId);
		final var amounts = ofNullable(previousAmounts).orElseGet(Map::of);
		final var family = ofNullable(previousFamily).orElseGet(PreviousFamily::empty);
		if (family.isEmpty() || !family.complete()) {
			return applicationRows(errandId, errand, names, amounts);
		}
		return family.members().stream()
			.map(member -> familyRow(errandId, errand, names, amounts, member))
			.toList();
	}

	/** The household as the application states it — the applicant, the co-applicant and the children. */
	private static List<FaNormPersonEntity> applicationRows(final String errandId, final FinancialAssistanceEntity errand, final Map<String, String> names,
		final Map<String, BigDecimal> amounts) {

		final var rows = new ArrayList<FaNormPersonEntity>();
		ofNullable(errand.getPersons()).orElseGet(List::of).forEach(person -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(person.getPartyId()).withRole(person.getRole()).withName(nameFor(names, person.getRole()))
			.withProcessDays(FULL_MONTH_DAYS).withIncluded(true).withAmount(amountFor(amounts, person.getPartyId()))));

		ofNullable(errand.getChildren()).orElseGet(List::of).forEach(child -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(child.getPartyId()).withRole(childRole(child.getResidenceExtent())).withName(childName(child.getFirstName(), child.getLastName()))
			.withProcessDays(ofNullable(child.getDaysInHome()).orElse(FULL_MONTH_DAYS)).withIncluded(true)
			.withAmount(amountFor(amounts, child.getPartyId()))));

		return rows;
	}

	/**
	 * One member of the previous calculation as a row. The role and days come from the application when it names the
	 * same person; the name from the application too when it has one, else Lifecare's.
	 */
	private static FaNormPersonEntity familyRow(final String errandId, final FinancialAssistanceEntity errand, final Map<String, String> names,
		final Map<String, BigDecimal> amounts, final PreviousFamily.Member member) {

		final var row = FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(member.partyId()).withName(member.name())
			.withProcessDays(FULL_MONTH_DAYS).withIncluded(true).withAmount(amountFor(amounts, member.partyId()));

		applicationPerson(errand, member.partyId()).ifPresent(person -> row
			.withRole(person.getRole())
			.withName(ofNullable(nameFor(names, person.getRole())).orElse(member.name())));
		applicationChild(errand, member.partyId()).ifPresent(child -> row
			.withRole(childRole(child.getResidenceExtent()))
			.withName(Optional.of(childName(child.getFirstName(), child.getLastName())).filter(name -> hasText(name)).orElse(member.name()))
			.withProcessDays(ofNullable(child.getDaysInHome()).orElse(FULL_MONTH_DAYS)));
		return row;
	}

	/**
	 * What the caseworker has to check by hand once the family is copied from the previous calculation: a member the
	 * application does not name, a person the application names that the previous calculation did not include, a member
	 * who was only in it for a deviating period — FamilyCare's read model has no day count to carry that over with — and
	 * a family that could not be copied at all. Nothing when there is no previous calculation.
	 */
	public List<WarningService.WarningInput> familyWarnings(final FinancialAssistanceEntity errand, final PreviousFamily previousFamily) {
		final var family = ofNullable(previousFamily).orElseGet(PreviousFamily::empty);
		if (family.isEmpty()) {
			return List.of();
		}
		if (!family.complete()) {
			return List.of(new WarningService.WarningInput(TYPE_FAMILY_DIFFERS_FROM_APPLICATION, SOURCE_KEY_FAMILY_NOT_COPIED, WARNING_FAMILY_NOT_COPIED));
		}

		final var warnings = new ArrayList<WarningService.WarningInput>();
		family.members().stream()
			.filter(member -> applicationPerson(errand, member.partyId()).isEmpty() && applicationChild(errand, member.partyId()).isEmpty())
			.forEach(member -> warnings.add(new WarningService.WarningInput(TYPE_FAMILY_DIFFERS_FROM_APPLICATION, "not-in-application:" + member.partyId(),
				WARNING_NOT_IN_APPLICATION.formatted(displayName(member.name())))));

		final var previousIds = family.members().stream().map(PreviousFamily.Member::partyId).collect(Collectors.toSet());
		ofNullable(errand.getPersons()).orElseGet(List::of).stream()
			.filter(person -> !previousIds.contains(person.getPartyId()))
			.forEach(person -> warnings.add(new WarningService.WarningInput(TYPE_FAMILY_DIFFERS_FROM_APPLICATION,
				"not-in-previous:" + ofNullable(person.getPartyId()).orElse(person.getRole()),
				WARNING_NOT_IN_PREVIOUS.formatted(ofNullable(roleDisplayName(person.getRole())).orElse("En hushållsmedlem")))));
		ofNullable(errand.getChildren()).orElseGet(List::of).stream()
			.filter(child -> !previousIds.contains(child.getPartyId()))
			.forEach(child -> {
				final var name = childName(child.getFirstName(), child.getLastName());
				warnings.add(new WarningService.WarningInput(TYPE_FAMILY_DIFFERS_FROM_APPLICATION,
					"not-in-previous:" + ofNullable(child.getPartyId()).filter(id -> hasText(id)).orElse(name),
					WARNING_NOT_IN_PREVIOUS.formatted(displayName(name))));
			});

		family.members().stream()
			.filter(PreviousFamily.Member::hasDeviation)
			.forEach(member -> warnings.add(new WarningService.WarningInput(TYPE_FAMILY_DEVIATING_PERIOD, "deviation:" + member.partyId(),
				WARNING_DEVIATING_PERIOD.formatted(displayName(member.name()), dateText(member.deviationFrom()), dateText(member.deviationTo())))));
		return warnings;
	}

	/**
	 * The gemensamma hushållskostnader cannot be copied: FamilyCare's read model gives the previous calculation's amount
	 * but not whether a custom household size was set, nor which. What can be seen is a difference in head count — the
	 * previous calculation paid common costs for one number of people and the draft has another — and then the caseworker
	 * is asked to check the household size by hand.
	 */
	public List<WarningService.WarningInput> commonHouseholdCostWarnings(final PreviousFamily previousFamily, final List<FaNormPersonEntity> personRows) {
		final var family = ofNullable(previousFamily).orElseGet(PreviousFamily::empty);
		final var cost = family.commonHouseholdCost();
		final var draftCount = ofNullable(personRows).orElseGet(List::of).size();
		if (family.isEmpty() || (cost == null) || (cost.signum() <= 0) || (family.members().size() == draftCount)) {
			return List.of();
		}
		return List.of(new WarningService.WarningInput(TYPE_COMMON_HOUSEHOLD_COST_CHECK, SOURCE_KEY_COMMON_HOUSEHOLD_COST,
			WARNING_COMMON_HOUSEHOLD_COST.formatted(cost.setScale(0, RoundingMode.HALF_UP).toPlainString(), family.members().size(), draftCount)));
	}

	private static Optional<FaPerson> applicationPerson(final FinancialAssistanceEntity errand, final String partyId) {
		return ofNullable(errand.getPersons()).orElseGet(List::of).stream()
			.filter(person -> hasText(partyId) && partyId.equals(person.getPartyId()))
			.findFirst();
	}

	private static Optional<FaChild> applicationChild(final FinancialAssistanceEntity errand, final String partyId) {
		return ofNullable(errand.getChildren()).orElseGet(List::of).stream()
			.filter(child -> hasText(partyId) && partyId.equals(child.getPartyId()))
			.findFirst();
	}

	private static String displayName(final String name) {
		return Optional.ofNullable(name).filter(text -> hasText(text)).orElse("En hushållsmedlem");
	}

	private static String dateText(final LocalDate date) {
		return ofNullable(date).map(LocalDate::toString).orElse("");
	}

	/** The previous calculation's amount for a party id, tolerating a member the application left without one. */
	private static BigDecimal amountFor(final Map<String, BigDecimal> amounts, final String partyId) {
		if (!hasText(partyId)) {
			return null;
		}
		return amounts.get(partyId);
	}

	/**
	 * The household members' names by role, from the errand's stakeholders. Best-effort: a failed read reports no names
	 * rather than failing the prepare run, which must not hinge on a label.
	 */
	private Map<String, String> householdNames(final String municipalityId, final String namespace, final String errandId) {
		try {
			return stakeholderService.readAll(municipalityId, namespace, errandId).stream()
				.filter(stakeholder -> hasText(stakeholder.getRole()))
				.filter(stakeholder -> hasText(stakeholderName(stakeholder)))
				.collect(Collectors.toMap(Stakeholder::getRole, CalculationFeeder::stakeholderName, (first, _) -> first));
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the errand's stakeholders for the household member names", e);
			return Map.of();
		}
	}

	/** The name for a role, tolerating a role the application left unset — {@code Map.of()} rejects a null key outright. */
	private static String nameFor(final Map<String, String> names, final String role) {
		if (!hasText(role)) {
			return null;
		}
		return names.get(role);
	}

	/** A stakeholder's display name — the organisation name when present, otherwise the given + family name. */
	private static String stakeholderName(final Stakeholder stakeholder) {
		if (hasText(stakeholder.getOrganizationName())) {
			return stakeholder.getOrganizationName().trim();
		}
		return Stream.of(stakeholder.getFirstName(), stakeholder.getLastName())
			.filter(part -> hasText(part))
			.map(String::trim)
			.collect(Collectors.joining(" "));
	}

	/**
	 * Housing-cost drift against the previous calculation in Lifecare, classified by the
	 * {@code Decision_ateransokanDelta} DMN, which decides — by how much the cost moved — whether it is worth flagging
	 * and the note to show; a small change passes silently.
	 *
	 * <p>
	 * Household drift is <em>not</em> handled here any more. The verksamhet's återansökan regelverk replaced the
	 * delta DMN's tiered {@code HOUSEHOLD_SIZE} judgement with an exact comparison of the number of persons in the
	 * home ({@code ANTAL_I_BOSTADEN} in {@link ApplicationRulesService}), so that branch lives in
	 * {@link ApplicationRuleFeeder} now; new members are still surfaced as NEW_PERSON warnings from the merge.
	 * </p>
	 */
	public List<WarningService.WarningInput> housingDeltaWarnings(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		if ((previous == null) || (previous.memberCount() == 0)) {
			return List.of();
		}

		final var warnings = new ArrayList<WarningService.WarningInput>();
		housingCostWarning(municipalityId, errand, previous).ifPresent(warnings::add);
		return List.copyOf(warnings);
	}

	/** The housing-cost delta (previous Rent vs current applied RENT, as a signed percent), classified by the DMN. */
	private Optional<WarningService.WarningInput> housingCostWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		final var previousCost = previous.housingCost();
		if ((previousCost == null) || (previousCost.signum() <= 0)) {
			return Optional.empty();
		}

		final var currentRent = currentRent(errand);
		final var percent = currentRent.subtract(previousCost).multiply(HUNDRED).divide(previousCost, 0, RoundingMode.HALF_UP);

		final var verdict = renewalDeltaService.classify(municipalityId, CHANGE_HOUSING_COST, 0, percent);
		if (!verdict.warning()) {
			return Optional.empty();
		}

		final String sign;
		if (percent.signum() >= 0) {
			sign = "+";
		} else {
			sign = "";
		}
		final var detail = "Boendekostnaden har ändrats " + sign + percent + "% (tidigare " + plain(previousCost) + " kr → nu " + plain(currentRent) + " kr)";
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_HOUSING_COST_CHANGE, "housing-cost", withRule(detail, verdict.rule())));
	}

	/** Sum of the application's reported RENT costs (0 when none). */
	private static BigDecimal currentRent(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.filter(cost -> COST_TYPE_RENT.equals(cost.getCostType()))
			.map(FaCost::getAppliedAmount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static String withRule(final String detail, final String rule) {
		if ((rule == null) || rule.isBlank()) {
			return detail;
		}
		return detail + " — " + rule;
	}

	/** A full-time child is a CHILD; a part-time / other child is an visitation child. */
	private static String childRole(final String residenceExtent) {
		if ((residenceExtent == null) || RESIDENCE_FULL_TIME.equals(residenceExtent)) {
			return ROLE_CHILD;
		}
		return ROLE_VISITATION_CHILD;
	}

	private static String childName(final String firstName, final String lastName) {
		return Stream.of(firstName, lastName)
			.filter(part -> (part != null) && !part.isBlank())
			.collect(Collectors.joining(" "));
	}
}
