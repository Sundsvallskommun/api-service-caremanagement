package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPlanning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.normTypeDisplayName;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.roleDisplayName;

/**
 * Turns the application (and the previous Lifecare normberäkning) into the återansökan rule warnings, delegating every
 * judgement to the {@link ApplicationRulesService} DMN tables. This is the input-gathering half of the division of
 * labour the EB DMNs are built on: it iterates (per child, per declared income, per planning, per person), gates the
 * comparisons the verksamhet only wants made under a condition, and fills the placeholders the tables leave in the
 * warning text — PERSONNUMMER, NAMN, HELTID/DELTID, the sick-leave level, the SFI study path/course, XX, ANTAL and
 * NORM.
 *
 * <p>
 * The three groups are surfaced separately so the daily prepare can fold them into the existing
 * {@code reconcileCalculationWarnings} set, which dedups on (errandId, type, sourceKey) and auto-closes a warning whose
 * cause has resolved. Everything is best-effort: an unreachable engine or Lifecare read yields fewer warnings, never a
 * failed prepare.
 * </p>
 *
 * <p>
 * The question rules run for every EB errand rather than only for {@code applicationType = RENEWAL}. Most of them
 * ("finns det lön", "vilken planering har du", "finns bilagor") are meaningful on any application, and the ones that
 * are genuinely återansökan-only ("har barnens boende ändrats", "samma utbetalningssätt som föregående ansökan") are
 * unanswered on a new application and therefore skipped on their own. The two comparison groups need a previous
 * normberäkning to compare against, which restricts them to renewals in practice.
 * </p>
 */
@Service
public class ApplicationRuleFeeder {

	/** Question keys in {@code Decision_ansokanFragor}. */
	private static final String QUESTION_CHILD_RESIDENCE_EXTENT = "BARN_BOENDE_OMFATTNING";
	private static final String QUESTION_CHILDREN_RESIDENCE_CHANGED = "BARN_BOENDE_ANDRAT";
	private static final String QUESTION_HOUSING_CHANGED = "BOENDESITUATION_ANDRAD";
	private static final String QUESTION_INCOME_TYPE = "INKOMST_TYP";
	private static final String QUESTION_PENDING_BENEFIT = "NY_ERSATTNING_SOKT";
	private static final String QUESTION_NEW_ASSETS = "NYA_TILLGANGAR";
	private static final String QUESTION_PLANNING = "PLANERING";
	private static final String QUESTION_PAYMENT_SAME_AS_PREVIOUS = "SAMMA_UTBETALNINGSSATT";
	private static final String QUESTION_ATTACHMENTS = "BILAGOR";
	private static final String QUESTION_STAY_IN_MUNICIPALITY = "VISTELSE_SUNDSVALL";

	/** Comparison keys in {@code Decision_ansokanMotBerakning}. */
	private static final String COMPARISON_CHILDREN = "BARN_PERSONNUMMER";
	private static final String COMPARISON_HOUSEHOLD_COUNT = "ANTAL_I_BOSTADEN";
	private static final String COMPARISON_NORM = "NORM";

	private static final String ANSWER_YES = "JA";
	private static final String ANSWER_NO = "NEJ";

	/** The income types {@code Decision_inkomstMotForegaende} compares, in the order the regelverk lists them. */
	private static final List<String> COMPARED_INCOME_TYPES = List.of("SALARY", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT", "RENT_SHARE_FROM_CHILD");

	private static final String NORM_NATIONAL = "NATIONAL_NORM";
	private static final String NORM_OTHER = "OTHER_NORM";
	/** FamilyCare's free-text norm starts with this when the previous calculation used the national norm. */
	private static final String NORM_NATIONAL_PREFIX = "riksnorm";

	private static final String LABEL_APPLICANT = "Sökande";

	/** Placeholders the tables leave in their warning texts for the caller to fill. */
	private static final String PLACEHOLDER_PERSONAL_NUMBER = "PERSONNUMMER";
	private static final String PLACEHOLDER_NAME = "NAMN";
	private static final String PLACEHOLDER_WORK_EXTENT = "HELTID/DELTID";
	private static final String PLACEHOLDER_SICK_LEAVE_LEVEL = "HELTID 100%/DELTID 75%/50%/25%";
	private static final String PLACEHOLDER_SFI_STUDY_PATH = "1/2/3";
	private static final String PLACEHOLDER_SFI_COURSE = "A/B/C/D";
	private static final String PLACEHOLDER_OTHER_DESCRIPTION = "XX";
	private static final String PLACEHOLDER_COUNT = "ANTAL";
	private static final String PLACEHOLDER_NORM = "NORM";

	private static final String WORK_EXTENT_FULL = "FULL";
	private static final String LABEL_WORK_EXTENT_FULL = "heltid";
	private static final String LABEL_WORK_EXTENT_PART = "deltid";

	/** The fallback message when a table flags without a text of its own. */
	private static final String RULE_FALLBACK = "Kontrollera uppgiften i ansökan";

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationRuleFeeder.class);

	private final ApplicationRulesService applicationRulesService;
	private final AttachmentService attachmentService;
	private final CitizenService citizenService;

	ApplicationRuleFeeder(final ApplicationRulesService applicationRulesService, final AttachmentService attachmentService,
		final CitizenService citizenService) {
		this.applicationRulesService = applicationRulesService;
		this.attachmentService = attachmentService;
		this.citizenService = citizenService;
	}

	/**
	 * The warnings that follow directly from the answers in the application — one {@code Decision_ansokanFragor}
	 * evaluation per answer, iterated per child, per declared income type, per planning and per person where the
	 * question repeats. An unanswered question is skipped rather than sent as an empty answer.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  errandId       the errand the warnings land on
	 * @param  errand         the typed application data
	 * @return                the warnings the answers raised (empty when none)
	 */
	public List<WarningService.WarningInput> applicationQuestionWarnings(final String municipalityId, final String errandId,
		final FinancialAssistanceEntity errand) {

		final var warnings = new ArrayList<WarningService.WarningInput>();

		children(errand).forEach(child -> childResidenceWarning(municipalityId, child).ifPresent(warnings::add));
		yesNoWarning(municipalityId, QUESTION_CHILDREN_RESIDENCE_CHANGED, errand.getChildrenResidenceChanged(), "children-residence").ifPresent(warnings::add);
		yesNoWarning(municipalityId, QUESTION_HOUSING_CHANGED, errand.getHousingChanged(), "housing-situation").ifPresent(warnings::add);
		declaredIncomeTypes(errand).forEach(incomeType -> incomeTypeWarning(municipalityId, incomeType).ifPresent(warnings::add));
		yesNoWarning(municipalityId, QUESTION_PENDING_BENEFIT, errand.getHasPendingBenefits(), "pending-benefits").ifPresent(warnings::add);
		yesNoWarning(municipalityId, QUESTION_NEW_ASSETS, errand.getHasAssets(), "assets").ifPresent(warnings::add);
		plannings(errand).forEach(planning -> planningWarning(municipalityId, planning).ifPresent(warnings::add));
		persons(errand).forEach(person -> paymentMethodWarning(municipalityId, person).ifPresent(warnings::add));
		yesNoWarning(municipalityId, QUESTION_ATTACHMENTS, hasApplicationAttachments(errandId), "attachments").ifPresent(warnings::add);
		yesNoWarning(municipalityId, QUESTION_STAY_IN_MUNICIPALITY, errand.getStaysInMunicipality(), "stay-municipality").ifPresent(warnings::add);

		return List.copyOf(warnings);
	}

	/**
	 * The income comparisons against the previous normberäkning — one {@code Decision_inkomstMotForegaende} evaluation
	 * per compared income type ({@code SALARY}, {@code OCCUPATIONAL_PENSION_INSURANCE}, {@code CHILD_SUPPORT},
	 * {@code RENT_SHARE_FROM_CHILD}). A type neither side carries is not evaluated at all — the table would coalesce
	 * both sums to zero and find nothing.
	 *
	 * @param  municipalityId        the municipality the errand belongs to
	 * @param  errand                the typed application data supplying the declared incomes
	 * @param  previousIncomeAmounts the previous normberäkning's summed amount per financial assistance income type
	 * @return                       the warnings the comparisons raised (empty when none)
	 */
	public List<WarningService.WarningInput> incomeComparisonWarnings(final String municipalityId, final FinancialAssistanceEntity errand,
		final Map<String, BigDecimal> previousIncomeAmounts) {

		final var previous = ofNullable(previousIncomeAmounts).orElseGet(Map::of);

		return COMPARED_INCOME_TYPES.stream()
			.map(incomeType -> incomeComparisonWarning(municipalityId, errand, incomeType, previous.get(incomeType)))
			.flatMap(Optional::stream)
			.toList();
	}

	/**
	 * The application-versus-previous-normberäkning comparisons — children, the number of persons in the home and the
	 * norm, each evaluated through {@code Decision_ansokanMotBerakning} and each behind the verksamhet's own gate:
	 * children only when the applicant states children under 21, the household count only when the housing situation is
	 * unchanged (a changed one is already flagged by the question rules), the norm always.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  errand         the typed application data
	 * @param  previous       the previous normberäkning's household (empty when there is none)
	 * @return                the warnings the comparisons raised (empty when none)
	 */
	public List<WarningService.WarningInput> previousCalculationWarnings(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		if ((previous == null) || (previous.memberCount() == 0)) {
			return List.of();
		}

		final var warnings = new ArrayList<WarningService.WarningInput>();
		childrenComparisonWarning(municipalityId, errand, previous).ifPresent(warnings::add);
		householdCountWarning(municipalityId, errand, previous).ifPresent(warnings::add);
		normWarning(municipalityId, errand, previous).ifPresent(warnings::add);
		return List.copyOf(warnings);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_ansokanFragor
	// ----------------------------------------------------------------------------------------------------------------

	/** How much a child lives in the home — the child's own warning, keyed on the child so each is acknowledgeable. */
	private Optional<WarningService.WarningInput> childResidenceWarning(final String municipalityId, final FaChild child) {
		if (!hasText(child.getResidenceExtent())) {
			return Optional.empty();
		}
		final var verdict = applicationRulesService.question(municipalityId, QUESTION_CHILD_RESIDENCE_EXTENT, child.getResidenceExtent());
		// The child's name, not its personnummer: the application carries a party id, and a personnummer has no business
		// being written into a warning message.
		return toWarning(verdict, childSourceKey(child), replaceFirst(ruleText(verdict), PLACEHOLDER_PERSONAL_NUMBER, childLabel(child)));
	}

	/** A yes/no question — unanswered ({@code null}) is not a "no", so it is not asked at all. */
	private Optional<WarningService.WarningInput> yesNoWarning(final String municipalityId, final String question, final Boolean answer,
		final String sourceKey) {

		if (answer == null) {
			return Optional.empty();
		}
		final var verdict = applicationRulesService.question(municipalityId, question, yesNo(answer));
		return toWarning(verdict, sourceKey, ruleText(verdict));
	}

	/** One warning per declared income type (the applicant may declare several incomes of the same type). */
	private Optional<WarningService.WarningInput> incomeTypeWarning(final String municipalityId, final String incomeType) {
		final var verdict = applicationRulesService.question(municipalityId, QUESTION_INCOME_TYPE, incomeType);
		return toWarning(verdict, incomeType, ruleText(verdict));
	}

	/** One warning per planning, its text filled with the person, the extent/level and the free-text description. */
	private Optional<WarningService.WarningInput> planningWarning(final String municipalityId, final FaPlanning planning) {
		if (!hasText(planning.getPlanningType())) {
			return Optional.empty();
		}
		final var verdict = applicationRulesService.question(municipalityId, QUESTION_PLANNING, planning.getPlanningType());
		final var sourceKey = roleLabel(planning.getPerson()) + ":" + planning.getPlanningType();
		return toWarning(verdict, sourceKey, planningText(ruleText(verdict), planning));
	}

	/** Whether the person uses the same payment method as on the previous application. */
	private Optional<WarningService.WarningInput> paymentMethodWarning(final String municipalityId, final FaPerson person) {
		if (person.getPaymentSameAsPrevious() == null) {
			return Optional.empty();
		}
		final var verdict = applicationRulesService.question(municipalityId, QUESTION_PAYMENT_SAME_AS_PREVIOUS, yesNo(person.getPaymentSameAsPrevious()));
		return toWarning(verdict, roleLabel(person.getRole()), ruleText(verdict));
	}

	/** Fill the planning text's placeholders from the planning row; a placeholder with no value is left as it is. */
	private static String planningText(final String rule, final FaPlanning planning) {
		var text = replaceFirst(rule, PLACEHOLDER_NAME, roleLabel(planning.getPerson()));
		text = replaceFirst(text, PLACEHOLDER_SICK_LEAVE_LEVEL, percent(planning.getSickLeaveLevel()));
		text = replaceFirst(text, PLACEHOLDER_WORK_EXTENT, workExtentLabel(planning.getWorkExtent()));
		text = replaceFirst(text, PLACEHOLDER_SFI_STUDY_PATH, planning.getSfiStudyPath());
		text = replaceFirst(text, PLACEHOLDER_SFI_COURSE, planning.getSfiCourse());
		return replaceFirst(text, PLACEHOLDER_OTHER_DESCRIPTION, planning.getOtherDescription());
	}

	/**
	 * Whether the application carries any of the citizen's own uploaded files. Best-effort: a failed attachment read
	 * reports "unknown" ({@code null}), which skips the question rather than claiming there are no attachments.
	 */
	private Boolean hasApplicationAttachments(final String errandId) {
		try {
			return attachmentService.applicationAttachmentsExist(errandId);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the errand's attachments — skipping the attachment rule", e);
			return null;
		}
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_inkomstMotForegaende
	// ----------------------------------------------------------------------------------------------------------------

	private Optional<WarningService.WarningInput> incomeComparisonWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final String incomeType, final BigDecimal previousAmount) {

		final var applicationSum = declaredIncomeSum(errand, incomeType);
		if ((previousAmount == null) && (applicationSum == null)) {
			return Optional.empty();
		}

		final var verdict = applicationRulesService.incomeAgainstPrevious(municipalityId, incomeType, previousAmount != null,
			applicationSum != null, previousAmount, applicationSum);
		return toWarning(verdict, incomeType, ruleText(verdict));
	}

	/**
	 * The application's summed amount for an income type, or {@code null} when it declares none of that type at all —
	 * the distinction the table's {@code finnsIAnsokan} input rests on. Null amounts count as zero.
	 */
	private static BigDecimal declaredIncomeSum(final FinancialAssistanceEntity errand, final String incomeType) {
		final var matching = incomes(errand).filter(income -> incomeType.equals(income.getIncomeType())).toList();
		if (matching.isEmpty()) {
			return null;
		}
		return matching.stream()
			.map(FaIncome::getAmount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_ansokanMotBerakning
	// ----------------------------------------------------------------------------------------------------------------

	/**
	 * The children in the application against the children in the previous normberäkning. Only made when the applicant
	 * states children under 21, and only when every household member's party id resolves to the personal identity
	 * number the previous calculation is keyed on — an unresolvable member would make every renewal look like a
	 * mismatch, so the comparison is skipped instead.
	 */
	private Optional<WarningService.WarningInput> childrenComparisonWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		if (!TRUE.equals(errand.getHasChildrenUnder21())) {
			return Optional.empty();
		}

		final var childIds = resolvedPersonIds(municipalityId, children(errand).map(FaChild::getPartyId).toList());
		final var adultIds = resolvedPersonIds(municipalityId, persons(errand).map(FaPerson::getPartyId).toList());
		if (childIds.isEmpty() || adultIds.isEmpty()) {
			LOG.warn("Could not resolve every household member to a personal identity number — skipping the children comparison");
			return Optional.empty();
		}

		final var previousChildIds = previous.personIds().stream()
			.map(ApplicationRuleFeeder::personIdKey)
			.filter(StringUtils::hasText)
			.filter(personId -> !adultIds.get().contains(personId))
			.collect(toCollection(LinkedHashSet::new));

		final var verdict = applicationRulesService.againstPreviousCalculation(municipalityId, COMPARISON_CHILDREN,
			childIds.get().equals(previousChildIds));
		return toWarning(verdict, "children", ruleText(verdict));
	}

	/**
	 * The number of persons living in the home against the previous normberäkning's member count — an exact comparison,
	 * and only when the housing situation is unchanged (a changed one is the question rules' business).
	 */
	private Optional<WarningService.WarningInput> householdCountWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		final var declared = errand.getHousingPersonCount();
		if (TRUE.equals(errand.getHousingChanged()) || (declared == null)) {
			return Optional.empty();
		}

		final var verdict = applicationRulesService.againstPreviousCalculation(municipalityId, COMPARISON_HOUSEHOLD_COUNT,
			declared.intValue() == previous.memberCount());
		final var text = replaceFirst(replaceFirst(ruleText(verdict), PLACEHOLDER_COUNT, String.valueOf(declared)),
			PLACEHOLDER_COUNT, String.valueOf(previous.memberCount()));
		return toWarning(verdict, "household-count", text);
	}

	/**
	 * The norm applied for against the previous normberäkning's norm. FamilyCare carries the norm as free text, so it
	 * is read as the national norm when it starts with "Riksnorm" and as another norm when it says anything else; a
	 * blank norm carries no information and skips the comparison.
	 */
	private Optional<WarningService.WarningInput> normWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		final var applicationNorm = applicationNorm(errand);
		final var previousNorm = previousNorm(previous.norm());
		if (applicationNorm.isEmpty() || previousNorm.isEmpty()) {
			return Optional.empty();
		}

		final var verdict = applicationRulesService.againstPreviousCalculation(municipalityId, COMPARISON_NORM,
			applicationNorm.get().equals(previousNorm.get()));
		final var text = replaceFirst(replaceFirst(ruleText(verdict), PLACEHOLDER_NORM, normLabel(applicationNorm.get())),
			PLACEHOLDER_NORM, previous.norm().trim());
		return toWarning(verdict, "norm", text);
	}

	/** The norm the application asks for — the national norm wins when both are ticked. */
	private static Optional<String> applicationNorm(final FinancialAssistanceEntity errand) {
		final var normTypes = ofNullable(errand.getNormType()).orElseGet(List::of);
		if (normTypes.contains(NORM_NATIONAL)) {
			return Optional.of(NORM_NATIONAL);
		}
		if (normTypes.contains(NORM_OTHER)) {
			return Optional.of(NORM_OTHER);
		}
		return Optional.empty();
	}

	/** The previous calculation's free-text norm read as one of the application's norm types. */
	private static Optional<String> previousNorm(final String norm) {
		if (!hasText(norm)) {
			return Optional.empty();
		}
		if (norm.trim().toLowerCase().startsWith(NORM_NATIONAL_PREFIX)) {
			return Optional.of(NORM_NATIONAL);
		}
		return Optional.of(NORM_OTHER);
	}

	private static String normLabel(final String normType) {
		return ofNullable(normTypeDisplayName(normType)).orElseGet(() -> normTypeDisplayName(NORM_OTHER));
	}

	/**
	 * The party ids resolved to the personal identity numbers the previous calculation is keyed on, or empty when any
	 * of them cannot be resolved. Resolved numbers are compared and discarded here — never logged or persisted.
	 */
	private Optional<Set<String>> resolvedPersonIds(final String municipalityId, final List<String> partyIds) {
		final var resolved = new LinkedHashSet<String>();
		for (final var partyId : partyIds) {
			if (!hasText(partyId)) {
				return Optional.empty();
			}
			final var personId = citizenPersonId(municipalityId, partyId);
			if (personId.isEmpty()) {
				return Optional.empty();
			}
			resolved.add(personId.get());
		}
		return Optional.of(resolved);
	}

	/** A party id's personal identity number as a comparison key, best-effort (a failed lookup reports empty). */
	private Optional<String> citizenPersonId(final String municipalityId, final String partyId) {
		try {
			return citizenService.getPersonalNumber(municipalityId, partyId).map(ApplicationRuleFeeder::personIdKey).filter(StringUtils::hasText);
		} catch (final RuntimeException e) {
			LOG.warn("Could not resolve a household member's personal identity number", e);
			return Optional.empty();
		}
	}

	/**
	 * A personal identity number reduced to a comparison key — its last ten digits, so a 12-digit number from the
	 * citizen register and a 10-digit one from FamilyCare still match.
	 */
	private static String personIdKey(final String personalNumber) {
		final var digits = ofNullable(personalNumber).orElse("").replaceAll("\\D", "");
		if (digits.length() <= 10) {
			return digits;
		}
		return digits.substring(digits.length() - 10);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Shared helpers
	// ----------------------------------------------------------------------------------------------------------------

	/** A flagged verdict as a warning input; an unflagged one produces nothing. */
	private static Optional<WarningService.WarningInput> toWarning(final ApplicationRulesService.RuleVerdict verdict,
		final String sourceKey, final String message) {

		if (!verdict.warning()) {
			return Optional.empty();
		}
		return Optional.of(new WarningService.WarningInput(warningType(verdict.code()), sourceKey, message));
	}

	/** The table's own warning code, falling back to the generic review type when it named none. */
	private static String warningType(final String code) {
		return ofNullable(code).filter(StringUtils::hasText).orElse(WarningService.TYPE_APPLICATION_REVIEW);
	}

	/** The table's warning text, falling back to a generic prompt when it flagged without one. */
	private static String ruleText(final ApplicationRulesService.RuleVerdict verdict) {
		return ofNullable(verdict.rule()).filter(StringUtils::hasText).orElse(RULE_FALLBACK);
	}

	/** Replace the first literal occurrence of a placeholder; a value with no text leaves the text untouched. */
	private static String replaceFirst(final String text, final String placeholder, final String value) {
		if (!hasText(value)) {
			return text;
		}
		final var index = text.indexOf(placeholder);
		if (index < 0) {
			return text;
		}
		return text.substring(0, index) + value + text.substring(index + placeholder.length());
	}

	private static String yesNo(final Boolean answer) {
		if (TRUE.equals(answer)) {
			return ANSWER_YES;
		}
		return ANSWER_NO;
	}

	private static String roleLabel(final String role) {
		return ofNullable(roleDisplayName(role)).orElse(LABEL_APPLICANT);
	}

	private static String workExtentLabel(final String workExtent) {
		if (!hasText(workExtent)) {
			return null;
		}
		if (WORK_EXTENT_FULL.equals(workExtent)) {
			return LABEL_WORK_EXTENT_FULL;
		}
		return LABEL_WORK_EXTENT_PART;
	}

	private static String percent(final String level) {
		if (!hasText(level)) {
			return null;
		}
		return level + "%";
	}

	/** The child's name for the warning text, falling back to its party id when the application carried no name. */
	private static String childLabel(final FaChild child) {
		final var name = Stream.of(child.getFirstName(), child.getLastName())
			.filter(StringUtils::hasText)
			.reduce((first, second) -> first + " " + second)
			.orElse(null);
		return ofNullable(name).filter(StringUtils::hasText).orElseGet(child::getPartyId);
	}

	/** Stable dedup key for the child a warning concerns — its party id, falling back to its name. */
	private static String childSourceKey(final FaChild child) {
		return ofNullable(child.getPartyId()).filter(StringUtils::hasText).orElseGet(() -> childLabel(child));
	}

	/** The distinct income types the application declares, in encounter order. */
	private static Set<String> declaredIncomeTypes(final FinancialAssistanceEntity errand) {
		return incomes(errand)
			.map(FaIncome::getIncomeType)
			.filter(StringUtils::hasText)
			.collect(toCollection(LinkedHashSet::new));
	}

	private static Stream<FaIncome> incomes(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getIncomes()).orElseGet(List::of).stream().filter(Objects::nonNull);
	}

	private static Stream<FaChild> children(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getChildren()).orElseGet(List::of).stream().filter(Objects::nonNull);
	}

	private static Stream<FaPerson> persons(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getPersons()).orElseGet(List::of).stream().filter(Objects::nonNull);
	}

	private static Stream<FaPlanning> plannings(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getPlannings()).orElseGet(List::of).stream().filter(Objects::nonNull);
	}
}
