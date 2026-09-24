package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * financial assistance income warnings as acknowledgeable objects. The daily prepare step reconciles the current set
 * against what is
 * stored — creating new warnings, refreshing open ones, and auto-closing ones whose cause has resolved — while never
 * re-opening a warning the caseworker has already acted on. A caseworker can acknowledge or close each warning.
 */
@Service
public class WarningService {

	public static final String TYPE_UNHANDLED_INCOME = "UNHANDLED_INCOME";
	public static final String TYPE_INCOME_CHANGE = "INCOME_CHANGE";
	public static final String TYPE_MISSING_SSBTEK = "MISSING_SSBTEK";
	public static final String TYPE_NEW_INCOME = "NEW_INCOME";
	public static final String TYPE_NEW_EXPENSE = "NEW_EXPENSE";
	public static final String TYPE_NEW_PERSON = "NEW_PERSON";
	public static final String TYPE_INCOME_DROPPED = "INCOME_DROPPED";
	public static final String TYPE_HOUSEHOLD_CHANGE = "HOUSEHOLD_CHANGE";
	public static final String TYPE_HOUSING_COST_CHANGE = "HOUSING_COST_CHANGE";
	public static final String TYPE_EXPENSE_REVIEW = "EXPENSE_REVIEW";
	public static final String TYPE_EXPENSE_CAPPED = "EXPENSE_CAPPED";
	public static final String TYPE_INCOME_DUPLICATED = "INCOME_DUPLICATED";

	// Återansökan rule warnings — the varningskod values the rakel-eb-ateransokan DMN tables emit (see
	// ApplicationRulesService). APPLICATION_REVIEW is the fallback for a rule that flags without naming a code.
	public static final String TYPE_CHILD_NOT_FULL_TIME = "CHILD_NOT_FULL_TIME";
	public static final String TYPE_CHILDREN_RESIDENCE_CHANGED = "CHILDREN_RESIDENCE_CHANGED";
	public static final String TYPE_HOUSING_SITUATION_CHANGED = "HOUSING_SITUATION_CHANGED";
	public static final String TYPE_SALARY_JOB_STIMULUS = "SALARY_JOB_STIMULUS";
	public static final String TYPE_PENDING_BENEFIT = "PENDING_BENEFIT";
	public static final String TYPE_NEW_ASSETS = "NEW_ASSETS";
	public static final String TYPE_PLANNING_REVIEW = "PLANNING_REVIEW";
	public static final String TYPE_PAYMENT_METHOD_CHANGED = "PAYMENT_METHOD_CHANGED";
	public static final String TYPE_ATTACHMENTS_PRESENT = "ATTACHMENTS_PRESENT";
	public static final String TYPE_STAY_OUTSIDE_MUNICIPALITY = "STAY_OUTSIDE_MUNICIPALITY";
	public static final String TYPE_APPLICATION_REVIEW = "APPLICATION_REVIEW";
	public static final String TYPE_INCOME_MISSING_VS_PREVIOUS_CALCULATION = "INCOME_MISSING_VS_PREVIOUS_CALCULATION";
	public static final String TYPE_INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION = "INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION";
	public static final String TYPE_CHILDREN_MISMATCH_PREVIOUS_CALCULATION = "CHILDREN_MISMATCH_PREVIOUS_CALCULATION";
	public static final String TYPE_HOUSEHOLD_COUNT_MISMATCH_PREVIOUS_CALCULATION = "HOUSEHOLD_COUNT_MISMATCH_PREVIOUS_CALCULATION";
	public static final String TYPE_NORM_MISMATCH_PREVIOUS_CALCULATION = "NORM_MISMATCH_PREVIOUS_CALCULATION";

	/**
	 * An income SSBTEK reported in the comparison period and not in the control period — verksamhetens “föregående
	 * månad = facit”. Distinct from {@link #TYPE_INCOME_MISSING_VS_PREVIOUS_CALCULATION}, which compares against the
	 * previous normberäkning in Lifecare (what a caseworker transferred) rather than against the previous SSBTEK
	 * answer (what the agencies reported).
	 */
	public static final String TYPE_INCOME_MISSING_PREVIOUS_PERIOD = "INCOME_MISSING_PREVIOUS_PERIOD";

	/**
	 * A comparison-period income the previous month's normberäkning did not contain, and which this month's transfer
	 * therefore picks up — verksamhetens regelverk (revision 2026-09-22): <em>”Finns inkomster i jämförelseperioden
	 * som inte är överförda? Ja = för över till normberäkning och generera varning”</em>.
	 * <p>
	 * The only rule in the regelverk that both changes the normberäkning and warns about it, which is exactly why it
	 * warns: the money moves whether or not anyone looks, so the handläggare has to be told it moved.
	 * <p>
	 * Distinct from {@link #TYPE_INCOME_MISSING_PREVIOUS_PERIOD}, which fires for every comparison-period income
	 * (SSBTEK reported it last month, not this month). This one fires for the subset of those that were also absent
	 * from last month's calculation — the two overlap, and say different things: “the agencies stopped reporting it”
	 * versus “it was never taken, and now it has been”.
	 */
	public static final String TYPE_INCOME_TRANSFERRED_LATE = "INCOME_TRANSFERRED_LATE";

	/** The rakel-eb-periodkontroll tables: the table's own text says which branch fired, so one type per decision. */
	public static final String TYPE_SSBTEK_DAY_CHECK = "SSBTEK_DAY_CHECK";
	public static final String TYPE_PARENTAL_BENEFIT_PERIOD_CHECK = "PARENTAL_BENEFIT_PERIOD_CHECK";

	/**
	 * The family copied from the previous normberäkning (NORM-04): a member the application does not name, an application
	 * member the previous calculation did not include, or a family that could not be copied; a member who was only in the
	 * previous calculation for a deviating period; and gemensamma hushållskostnader paid for a different head count.
	 */
	public static final String TYPE_FAMILY_DIFFERS_FROM_APPLICATION = "FAMILY_DIFFERS_FROM_APPLICATION";
	public static final String TYPE_FAMILY_DEVIATING_PERIOD = "FAMILY_DEVIATING_PERIOD";
	public static final String TYPE_COMMON_HOUSEHOLD_COST_CHECK = "COMMON_HOUSEHOLD_COST_CHECK";
	/** The previous normberäkning's norm could not be found among the norms for the application month. */
	public static final String TYPE_PREVIOUS_NORM_NOT_AVAILABLE = "PREVIOUS_NORM_NOT_AVAILABLE";

	// Section warnings — raised by the decision proposal (DECISION tab, recomputed on every read and on CALCULATION
	// approval) and the payment warnings (PAYMENT tab, recomputed on DECISION approval). They are reconciled per owning
	// section
	// ({@link #reconcileByTypes}), so the daily calculation reconcile never touches them and vice versa.
	public static final String TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT = "PREVIOUS_DECISION_ADVANCE_ON_BENEFIT";
	public static final String TYPE_EXPENSE_PARTIALLY_REJECTED = "EXPENSE_PARTIALLY_REJECTED";
	public static final String TYPE_CO_APPLICANT_SPLIT_PAYMENT = "CO_APPLICANT_SPLIT_PAYMENT";
	/** FLAG-05: the applicant has a Lifecare decision mot återbetalning (återkrav) — shown on the DECISION tab. */
	public static final String TYPE_RECOVERY_CLAIM = "RECOVERY_CLAIM";

	/**
	 * SSBTEK could not be read on this run, so the income rules were deliberately not evaluated. Distinct from
	 * {@link #TYPE_MISSING_SSBTEK}, which says SSBTEK answered and a specific income the previous normberäkning had was
	 * not in the answer. This one says we have no answer to judge at all.
	 */
	public static final String TYPE_SSBTEK_READ_FAILED = "SSBTEK_READ_FAILED";

	/**
	 * A Lifecare read that other warnings depend on failed on this run — the återkrav, the previous decision or the
	 * previous normberäkning's family. Without it a failed read would look like "nothing to warn about": the dependent
	 * warnings would be auto-closed, and a closed warning is never re-opened, so a real återkrav could stay hidden after
	 * the next successful read. One warning per failed read (see the {@code SOURCE_KEY_LIFECARE_*} keys), closed by the
	 * next run in which that read succeeds; the warnings that depend on the read are left as they were in the failed run.
	 */
	public static final String TYPE_LIFECARE_READ_FAILED = "LIFECARE_READ_FAILED";

	/** Source keys of {@link #TYPE_LIFECARE_READ_FAILED} — one per Lifecare read whose dependent warnings it guards. */
	public static final String SOURCE_KEY_LIFECARE_RECOVERY_CLAIMS = "lifecare-read:recovery-claims";
	public static final String SOURCE_KEY_LIFECARE_PREVIOUS_DECISION = "lifecare-read:previous-decision";
	public static final String SOURCE_KEY_LIFECARE_PREVIOUS_FAMILY = "lifecare-read:previous-family";

	/** The handläggare-facing text per failed Lifecare read, in the same voice as the SSBTEK read failure. */
	private static final Map<String, String> LIFECARE_READ_FAILED_MESSAGES = Map.of(
		SOURCE_KEY_LIFECARE_RECOVERY_CLAIMS, "Återkrav i Lifecare kunde inte läsas – eventuella återkrav visas inte, kontrollera dem i Lifecare. Nytt försök görs vid nästa uppdatering",
		SOURCE_KEY_LIFECARE_PREVIOUS_DECISION, "Föregående beslut i Lifecare kunde inte läsas – beslutsförslaget saknar föregående beslut, kontrollera det i Lifecare. Nytt försök görs vid nästa uppdatering",
		SOURCE_KEY_LIFECARE_PREVIOUS_FAMILY,
		"Familjen i föregående normberäkning kunde inte läsas från Lifecare – hushållet är taget från ansökan och familjevarningarna är inte kontrollerade, kontrollera mot Lifecare. Nytt försök görs vid nästa uppdatering");

	/**
	 * Verksamhetens own wording for a failed read — the handläggare is told a retry is coming, not that data is missing
	 * — carrying the time of the attempt, so “nytt försök görs snart igen” can be judged against a clock rather than
	 * taken on faith. Each failed run refreshes the text, so the time is always the most recent attempt; the warning's
	 * {@code created} still says when the run of failures started.
	 */
	static final String MESSAGE_SSBTEK_READ_FAILED_PREFIX = "Fel att läsa SSBTEK ";
	static final String MESSAGE_SSBTEK_READ_FAILED_SUFFIX = ", nytt försök görs snart igen och ärendet kommer uppdateras med ny information";

	/**
	 * The failed read is timestamped in Swedish wall-clock time: the services run on UTC containers, and a handläggare
	 * reading “14:53” off the screen at 14:55 must not be told 12:53. Same reason {@code ErrandNumberGenerator} pins
	 * the zone rather than taking the system default.
	 */
	private static final ZoneId SSBTEK_READ_FAILED_ZONE = ZoneId.of("Europe/Stockholm");
	private static final DateTimeFormatter SSBTEK_READ_FAILED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	/** Verksamhetens wording for a read that failed at the given moment. */
	public static String ssbtekReadFailureMessage(final OffsetDateTime failedAt) {
		return MESSAGE_SSBTEK_READ_FAILED_PREFIX
			+ SSBTEK_READ_FAILED_TIME.format(failedAt.atZoneSameInstant(SSBTEK_READ_FAILED_ZONE))
			+ MESSAGE_SSBTEK_READ_FAILED_SUFFIX;
	}

	/**
	 * One warning per errand regardless of how many agencies failed — the handläggare cannot act per agency, and a
	 * stable key is what lets the next successful run close exactly this row.
	 */
	private static final String SOURCE_KEY_SSBTEK = "SSBTEK";

	/** The warning types the decision proposal owns (shown on the DECISION tab). */
	public static final Set<String> DECISION_PROPOSAL_TYPES = Set.of(TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT, TYPE_EXPENSE_PARTIALLY_REJECTED, TYPE_RECOVERY_CLAIM);
	/** The warning types the payment warnings own (shown on the PAYMENT tab). */
	public static final Set<String> PAYMENT_PROPOSAL_TYPES = Set.of(TYPE_CO_APPLICANT_SPLIT_PAYMENT);
	/**
	 * The read-failure warning is reconciled on its own, because it is the one warning raised on a run where the
	 * calculation reconcile does not happen at all. Leaving it to the calculation reconcile would auto-close it on the
	 * very next run — including a run that failed the same way.
	 */
	public static final Set<String> SSBTEK_READ_FAILURE_TYPES = Set.of(TYPE_SSBTEK_READ_FAILED);
	/**
	 * The Lifecare read-failure warning is reconciled on its own too, per read (type + source key): each read raises or
	 * closes its own row, and no other reconcile creates or auto-closes one.
	 */
	public static final Set<String> LIFECARE_READ_FAILURE_TYPES = Set.of(TYPE_LIFECARE_READ_FAILED);
	/** The warning types that depend on the previous Lifecare decision read. */
	public static final Set<String> PREVIOUS_DECISION_TYPES = Set.of(TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT);
	/** The warning types that depend on the återkrav read. */
	public static final Set<String> RECOVERY_CLAIM_TYPES = Set.of(TYPE_RECOVERY_CLAIM);
	/** The NORM-04 warning types that depend on the previous normberäkning's family read. */
	public static final Set<String> PREVIOUS_FAMILY_TYPES = Set.of(TYPE_FAMILY_DIFFERS_FROM_APPLICATION, TYPE_FAMILY_DEVIATING_PERIOD, TYPE_COMMON_HOUSEHOLD_COST_CHECK);
	/**
	 * The warning types that describe careM's calculation draft — raised by refreshing it: the rows the refresh added or
	 * saw disappear, the expense feed (reasonableness review + cap), the NORM-04 family copied from the previous
	 * normberäkning, the late comparison-period transfer and the duplicate incomes read from the merged draft — plus the
	 * housing-cost change, which is about the calculation's boendekostnad even though it compares the application with
	 * the previous normberäkning. Once the caseworker has saved the normberäkning in Lifecare the draft is no longer
	 * refreshed, so these stay as they last were: {@link #reconcileRuleWarnings} neither creates nor auto-closes them.
	 */
	public static final Set<String> DRAFT_REFRESH_TYPES = Set.of(TYPE_NEW_INCOME, TYPE_NEW_EXPENSE, TYPE_NEW_PERSON, TYPE_INCOME_DROPPED,
		TYPE_EXPENSE_REVIEW, TYPE_EXPENSE_CAPPED, TYPE_INCOME_DUPLICATED, TYPE_INCOME_TRANSFERRED_LATE, TYPE_FAMILY_DIFFERS_FROM_APPLICATION,
		TYPE_FAMILY_DEVIATING_PERIOD, TYPE_COMMON_HOUSEHOLD_COST_CHECK, TYPE_PREVIOUS_NORM_NOT_AVAILABLE, TYPE_HOUSING_COST_CHANGE);

	public static final String SECTION_CALCULATION = "CALCULATION";
	public static final String SECTION_DECISION = "DECISION";
	public static final String SECTION_PAYMENT = "PAYMENT";

	/** The Draken tab each Lifecare read failure is shown on — the tab whose warnings depend on that read. */
	private static final Map<String, String> LIFECARE_READ_FAILED_SECTIONS = Map.of(
		SOURCE_KEY_LIFECARE_RECOVERY_CLAIMS, SECTION_DECISION,
		SOURCE_KEY_LIFECARE_PREVIOUS_DECISION, SECTION_DECISION,
		SOURCE_KEY_LIFECARE_PREVIOUS_FAMILY, SECTION_CALCULATION);

	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
	public static final String STATUS_CLOSED = "CLOSED";

	/** Warning type → Swedish display name for the frontend (the machine {@code type} stays for logic). */
	private static final Map<String, String> TYPE_DISPLAY_NAME = Map.ofEntries(
		Map.entry(TYPE_UNHANDLED_INCOME, "Ej hanterad inkomst"),
		Map.entry(TYPE_INCOME_CHANGE, "Inkomständring"),
		Map.entry(TYPE_MISSING_SSBTEK, "Saknas i SSBTEK"),
		Map.entry(TYPE_NEW_INCOME, "Ny inkomst"),
		Map.entry(TYPE_NEW_EXPENSE, "Ny utgift"),
		Map.entry(TYPE_NEW_PERSON, "Ny hushållsmedlem"),
		Map.entry(TYPE_INCOME_DROPPED, "Inkomst borttagen"),
		Map.entry(TYPE_HOUSEHOLD_CHANGE, "Förändrat hushåll"),
		Map.entry(TYPE_HOUSING_COST_CHANGE, "Förändrad boendekostnad"),
		Map.entry(TYPE_EXPENSE_REVIEW, "Manuell skälighetsbedömning"),
		Map.entry(TYPE_EXPENSE_CAPPED, "Kapad kostnad"),
		Map.entry(TYPE_INCOME_DUPLICATED, "Möjlig dubbelförd inkomst"),
		Map.entry(TYPE_CHILD_NOT_FULL_TIME, "Barn bor inte heltid"),
		Map.entry(TYPE_CHILDREN_RESIDENCE_CHANGED, "Barns boende ändrat"),
		Map.entry(TYPE_HOUSING_SITUATION_CHANGED, "Boendesituation ändrad"),
		Map.entry(TYPE_SALARY_JOB_STIMULUS, "Lön – jobbstimulans"),
		Map.entry(TYPE_PENDING_BENEFIT, "Väntar på annan ersättning"),
		Map.entry(TYPE_NEW_ASSETS, "Nya tillgångar"),
		Map.entry(TYPE_PLANNING_REVIEW, "Kontrollera planering"),
		Map.entry(TYPE_PAYMENT_METHOD_CHANGED, "Nytt utbetalningssätt"),
		Map.entry(TYPE_ATTACHMENTS_PRESENT, "Bilagor att kontrollera"),
		Map.entry(TYPE_STAY_OUTSIDE_MUNICIPALITY, "Vistelse utanför kommunen"),
		Map.entry(TYPE_APPLICATION_REVIEW, "Kontrollera ansökan"),
		Map.entry(TYPE_INCOME_MISSING_VS_PREVIOUS_CALCULATION, "Inkomst saknas mot föregående beräkning"),
		Map.entry(TYPE_INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION, "Inkomstbelopp skiljer mot föregående beräkning"),
		Map.entry(TYPE_CHILDREN_MISMATCH_PREVIOUS_CALCULATION, "Barn stämmer inte mot föregående beräkning"),
		Map.entry(TYPE_HOUSEHOLD_COUNT_MISMATCH_PREVIOUS_CALCULATION, "Antal i bostaden stämmer inte mot föregående beräkning"),
		Map.entry(TYPE_SSBTEK_DAY_CHECK, "Kontrollera antal dagar"),
		Map.entry(TYPE_PARENTAL_BENEFIT_PERIOD_CHECK, "Kontrollera föräldrapenningperiod"),
		Map.entry(TYPE_NORM_MISMATCH_PREVIOUS_CALCULATION, "Norm stämmer inte mot föregående beräkning"),
		Map.entry(TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT, "Föregående beslut var förskott på förmån"),
		Map.entry(TYPE_EXPENSE_PARTIALLY_REJECTED, "Utgift delvis ej godkänd – delavslag"),
		Map.entry(TYPE_CO_APPLICANT_SPLIT_PAYMENT, "Medsökande – kontrollera delad utbetalning"),
		Map.entry(TYPE_SSBTEK_READ_FAILED, "SSBTEK kunde inte läsas"),
		Map.entry(TYPE_LIFECARE_READ_FAILED, "Lifecare kunde inte läsas"),
		Map.entry(TYPE_INCOME_MISSING_PREVIOUS_PERIOD, "Inkomst saknas mot föregående SSBTEK-period"),
		Map.entry(TYPE_INCOME_TRANSFERRED_LATE, "Inkomst överförd i efterhand"),
		Map.entry(TYPE_FAMILY_DIFFERS_FROM_APPLICATION, "Familjen skiljer mot ansökan"),
		Map.entry(TYPE_FAMILY_DEVIATING_PERIOD, "Kontrollera omfattning"),
		Map.entry(TYPE_COMMON_HOUSEHOLD_COST_CHECK, "Kontrollera gemensamma hushållskostnader"),
		Map.entry(TYPE_PREVIOUS_NORM_NOT_AVAILABLE, "Föregående norm saknas för månaden"),
		Map.entry(TYPE_RECOVERY_CLAIM, "Återkrav i Lifecare"));

	/** Warning status → Swedish display name. */
	private static final Map<String, String> STATUS_DISPLAY_NAME = Map.ofEntries(
		Map.entry(STATUS_OPEN, "Öppen"),
		Map.entry(STATUS_ACKNOWLEDGED, "Kvitterad"),
		Map.entry(STATUS_CLOSED, "Stängd"));

	private final FaWarningRepository warningRepository;

	WarningService(final FaWarningRepository warningRepository) {
		this.warningRepository = warningRepository;
	}

	/** A computed warning before persistence — the dedup key is {@code type + sourceKey}. */
	public record WarningInput(String type, String sourceKey, String message) {
	}

	/**
	 * Reconcile the full calculation warnings into the errand's warning objects: the rules income warnings
	 * (unhandled / changed / still-missing), the rows the daily refresh newly added (NEW_*) or saw disappear, and the
	 * section warnings the feeder pre-typed and DMN-classified — the expense rules (reasonableness review + cap) and the
	 * renewal delta (household-size + housing drift).
	 */
	@Transactional
	public void reconcileCalculationWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final DraftChanges draftChanges, final List<WarningInput> sectionWarnings) {
		reconcileCalculationWarnings(errandId, unhandled, changes, missing, draftChanges, sectionWarnings, Set.of());
	}

	/**
	 * {@link #reconcileCalculationWarnings(String, List, List, List, DraftChanges, List)}, leaving the
	 * {@code unverifiedTypes} exactly as they were: a Lifecare read those warnings depend on failed on this run, so their
	 * absence proves nothing and must not auto-close them. No {@code sectionWarnings} input may carry one of them.
	 */
	@Transactional
	public void reconcileCalculationWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final DraftChanges draftChanges, final List<WarningInput> sectionWarnings, final Set<String> unverifiedTypes) {

		final var unverified = ofNullable(unverifiedTypes).orElseGet(Set::of);
		final var inputs = ssbtekIncomeWarnings(unhandled, changes, missing);

		if (draftChanges != null) {
			ofList(draftChanges.addedIncomes()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_INCOME, sourceKey(text), "Ny inkomst i SSBTEK, ej införd i beräkningen: " + text)));
			ofList(draftChanges.addedExpenses()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_EXPENSE, sourceKey(text), "Ny utgift i ansökan: " + text)));
			ofList(draftChanges.addedPersons()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_PERSON, sourceKey(text), "Ny hushållsmedlem: " + text)));
			ofList(draftChanges.droppedIncomes()).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_DROPPED, sourceKey(text), "Inkomst inte längre i SSBTEK: " + text)));
		}

		ofNullable(sectionWarnings).ifPresent(inputs::addAll);
		inputs.stream()
			.filter(input -> unverified.contains(input.type()))
			.findFirst()
			.ifPresent(input -> {
				throw new IllegalArgumentException("warning type " + input.type() + " is unverified on this run");
			});
		// The calculation owns every type except the separately reconciled ones — those live and die with their own
		// reconcile, so the daily prepare must neither create nor auto-close them — and the unverified ones.
		reconcile(errandId, inputs, type -> !isSeparatelyReconciled(type) && !unverified.contains(type));
	}

	/**
	 * Reconcile the calculation warnings of a run that did <strong>not</strong> refresh the draft — the caseworker has
	 * saved the normberäkning in Lifecare, and that is now the truth. The SSBTEK income warnings (unhandled / changed /
	 * still-missing) and the rule warnings that do not depend on the draft are reconciled as usual; the
	 * {@link #DRAFT_REFRESH_TYPES} are left exactly as they last were — neither created, refreshed nor auto-closed. Every
	 * {@code ruleWarnings} input must carry a type outside {@link #DRAFT_REFRESH_TYPES}.
	 */
	@Transactional
	public void reconcileRuleWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final List<WarningInput> ruleWarnings) {

		final var rules = ofNullable(ruleWarnings).orElseGet(List::of);
		rules.stream()
			.filter(input -> DRAFT_REFRESH_TYPES.contains(input.type()))
			.findFirst()
			.ifPresent(input -> {
				throw new IllegalArgumentException("warning type " + input.type() + " belongs to the draft refresh");
			});

		final var inputs = ssbtekIncomeWarnings(unhandled, changes, missing);
		inputs.addAll(rules);
		reconcile(errandId, inputs, type -> !isSeparatelyReconciled(type) && !DRAFT_REFRESH_TYPES.contains(type));
	}

	/** The rules income warnings the process sent: unhandled, significantly changed and still-missing incomes. */
	private static List<WarningInput> ssbtekIncomeWarnings(final List<String> unhandled, final List<String> changes, final List<String> missing) {
		final List<WarningInput> inputs = new ArrayList<>();
		ofList(unhandled).forEach(text -> inputs.add(new WarningInput(TYPE_UNHANDLED_INCOME, sourceKey(text), text)));
		ofList(changes).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_CHANGE, sourceKey(text), text)));
		ofList(missing).forEach(text -> inputs.add(new WarningInput(TYPE_MISSING_SSBTEK, text, "Saknas fortfarande i SSBTEK: " + text)));
		return inputs;
	}

	/**
	 * Reconcile one section proposal's warnings — only the rows whose {@code type} is in {@code ownedTypes} are
	 * created, refreshed or auto-closed; every other warning on the errand is left untouched. The same semantics as the
	 * calculation reconcile otherwise: insert OPEN when new, refresh the message when still OPEN/ACKNOWLEDGED, never
	 * re-open a CLOSED one, auto-close when absent. Every {@code current} input must carry one of the owned types.
	 * Returns the errand's warnings of the owned types after the reconcile, oldest first.
	 */
	@Transactional
	public List<Warning> reconcileByTypes(final String errandId, final Set<String> ownedTypes, final List<WarningInput> current) {
		return reconcileByTypes(errandId, ownedTypes, Set.of(), current);
	}

	/**
	 * {@link #reconcileByTypes(String, Set, List)}, leaving the {@code unverifiedTypes} (a subset of {@code ownedTypes})
	 * exactly as they were: a Lifecare read those warnings depend on failed on this run, so their absence proves nothing
	 * and must not auto-close them. They are still returned. No {@code current} input may carry one of them.
	 */
	@Transactional
	public List<Warning> reconcileByTypes(final String errandId, final Set<String> ownedTypes, final Set<String> unverifiedTypes, final List<WarningInput> current) {
		final var unverified = ofNullable(unverifiedTypes).orElseGet(Set::of);
		current.stream()
			.filter(input -> !ownedTypes.contains(input.type()) || unverified.contains(input.type()))
			.findFirst()
			.ifPresent(input -> {
				throw new IllegalArgumentException("warning type " + input.type() + " is not owned by this reconcile");
			});
		reconcile(errandId, current, type -> ownedTypes.contains(type) && !unverified.contains(type));
		return warningRepository.findByErrandId(errandId).stream()
			.filter(entity -> ownedTypes.contains(entity.getType()))
			.sorted(comparing(FaWarningEntity::getCreated, nullsLast(naturalOrder())))
			.map(WarningService::toWarning)
			.toList();
	}

	/**
	 * Raise or clear the SSBTEK read-failure warning for an errand. Called on every daily run: {@code true} on a run
	 * where SSBTEK could not be read, {@code false} on one that succeeded — the reconcile then auto-closes the warning
	 * without the caseworker having to do anything.
	 */
	@Transactional
	public void reconcileSsbtekReadFailure(final String errandId, final boolean readFailed) {
		final List<WarningInput> current;
		if (readFailed) {
			current = List.of(new WarningInput(TYPE_SSBTEK_READ_FAILED, SOURCE_KEY_SSBTEK, ssbtekReadFailureMessage(OffsetDateTime.now(SSBTEK_READ_FAILED_ZONE))));
		} else {
			current = List.of();
		}
		reconcile(errandId, current, SSBTEK_READ_FAILURE_TYPES::contains);
	}

	/**
	 * Raise or clear the read-failure warning of one Lifecare read ({@code sourceKey}, one of the
	 * {@code SOURCE_KEY_LIFECARE_*} keys): {@code true} on a run where the read failed, {@code false} on one where it
	 * succeeded — the reconcile then auto-closes the warning. Only that read's row is touched. Returns that row, when
	 * there is one, so a section proposal can show it with its own warnings.
	 */
	@Transactional
	public List<Warning> reconcileLifecareReadFailure(final String errandId, final String sourceKey, final boolean readFailed) {
		final var message = ofNullable(LIFECARE_READ_FAILED_MESSAGES.get(sourceKey))
			.orElseThrow(() -> new IllegalArgumentException("unknown Lifecare read " + sourceKey));
		final List<WarningInput> current;
		if (readFailed) {
			current = List.of(new WarningInput(TYPE_LIFECARE_READ_FAILED, sourceKey, message));
		} else {
			current = List.of();
		}
		final Predicate<FaWarningEntity> owned = entity -> TYPE_LIFECARE_READ_FAILED.equals(entity.getType()) && sourceKey.equals(entity.getSourceKey());
		reconcileEntities(errandId, current, owned);
		return warningRepository.findByErrandId(errandId).stream()
			.filter(owned)
			.sorted(comparing(FaWarningEntity::getCreated, nullsLast(naturalOrder())))
			.map(WarningService::toWarning)
			.toList();
	}

	/**
	 * Types reconciled by something other than the daily calculation reconcile — the two section proposals and the
	 * SSBTEK and Lifecare read failures. The calculation reconcile must neither create nor auto-close these.
	 */
	private static boolean isSeparatelyReconciled(final String type) {
		return DECISION_PROPOSAL_TYPES.contains(type) || PAYMENT_PROPOSAL_TYPES.contains(type) || SSBTEK_READ_FAILURE_TYPES.contains(type)
			|| LIFECARE_READ_FAILURE_TYPES.contains(type);
	}

	/**
	 * The Draken tab a warning belongs to — a Lifecare read failure is shown on the tab whose warnings depend on that
	 * read; otherwise by type.
	 */
	static String sectionOf(final String type, final String sourceKey) {
		if (TYPE_LIFECARE_READ_FAILED.equals(type)) {
			return ofNullable(sourceKey).map(LIFECARE_READ_FAILED_SECTIONS::get).orElse(SECTION_CALCULATION);
		}
		return sectionOf(type);
	}

	/** The Draken tab a warning type belongs to — the section proposals own theirs, everything else is the calculation. */
	static String sectionOf(final String type) {
		if (DECISION_PROPOSAL_TYPES.contains(type)) {
			return SECTION_DECISION;
		}
		if (PAYMENT_PROPOSAL_TYPES.contains(type)) {
			return SECTION_PAYMENT;
		}
		return SECTION_CALCULATION;
	}

	/**
	 * Reconcile the errand's warnings of the types {@code owned} accepts against {@code current}: create the ones that
	 * are new, refresh the message of ones still OPEN/ACKNOWLEDGED, and auto-close ones whose cause has resolved (no
	 * longer in {@code current}). A CLOSED warning is never re-opened. Existing warnings of a type outside {@code owned}
	 * are invisible to the reconcile — neither refreshed nor closed.
	 *
	 * <p>
	 * Package-private and intentionally not {@code @Transactional}: it is only ever invoked by the public
	 * {@code reconcile*} entry points above, which carry the transaction — a self-invoked {@code @Transactional}
	 * method would bypass the Spring proxy and silently run without one.
	 */
	void reconcile(final String errandId, final List<WarningInput> current, final Predicate<String> owned) {
		reconcileEntities(errandId, current, entity -> owned.test(entity.getType()));
	}

	/** {@link #reconcile}, with ownership decided per stored warning rather than per type. */
	private void reconcileEntities(final String errandId, final List<WarningInput> current, final Predicate<FaWarningEntity> owned) {
		final var existing = warningRepository.findByErrandId(errandId).stream()
			.filter(owned)
			.toList();
		final var currentKeys = current.stream().map(input -> key(input.type(), input.sourceKey())).collect(toSet());

		for (final var input : current) {
			final var match = existing.stream()
				.filter(entity -> key(entity.getType(), entity.getSourceKey()).equals(key(input.type(), input.sourceKey())))
				.findFirst();
			if (match.isEmpty()) {
				warningRepository.save(FaWarningEntity.create()
					.withErrandId(errandId)
					.withType(input.type())
					.withSourceKey(input.sourceKey())
					.withMessage(input.message())
					.withStatus(STATUS_OPEN)
					.withAutoResolved(false));
			} else if (!STATUS_CLOSED.equals(match.get().getStatus())) { // never re-open a closed warning
				warningRepository.save(match.get().withMessage(input.message()));
			}
		}

		// Cause resolved (no longer computed) → auto-close the ones still open/acknowledged.
		existing.stream()
			.filter(entity -> !currentKeys.contains(key(entity.getType(), entity.getSourceKey())))
			.filter(entity -> !STATUS_CLOSED.equals(entity.getStatus()))
			.forEach(entity -> warningRepository.save(entity.withStatus(STATUS_CLOSED).withAutoResolved(true)));
	}

	@Transactional(readOnly = true)
	public List<Warning> list(final String errandId) {
		return warningRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaWarningEntity::getCreated, nullsLast(naturalOrder())))
			.map(WarningService::toWarning)
			.toList();
	}

	/** How many warnings on the errand are still active (OPEN or ACKNOWLEDGED — i.e. not CLOSED). */
	@Transactional(readOnly = true)
	public long countActive(final String errandId) {
		return warningRepository.countByErrandIdAndStatusNot(errandId, STATUS_CLOSED);
	}

	/**
	 * Create a warning directly on an errand — the careM temp stage, with no Lifecare round-trip. The warning is born
	 * {@code OPEN}; the {@code sourceKey} is derived from the message when not supplied (the same rule reconcile uses).
	 */
	@Transactional
	public Warning create(final String errandId, final String type, final String sourceKey, final String message) {
		return toWarning(warningRepository.save(FaWarningEntity.create()
			.withErrandId(errandId)
			.withType(type)
			.withSourceKey(ofNullable(sourceKey).filter(StringUtils::hasText).orElseGet(() -> sourceKey(message)))
			.withMessage(message)
			.withStatus(STATUS_OPEN)
			.withAutoResolved(false)));
	}

	/**
	 * Set a warning's status (a caseworker action) — acknowledge, close, or re-open to {@code OPEN} (undo an earlier
	 * acknowledge/close). Re-opening clears the auto-resolved flag, since it is a manual action.
	 */
	@Transactional
	public Warning updateStatus(final String errandId, final String warningId, final String status) {
		final var target = validateTargetStatus(status);
		final var entity = warningRepository.findByIdAndErrandId(warningId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Warning not found on errand"));
		return toWarning(warningRepository.save(entity.withStatus(target).withAutoResolved(false)));
	}

	private static String validateTargetStatus(final String status) {
		if (!STATUS_OPEN.equals(status) && !STATUS_ACKNOWLEDGED.equals(status) && !STATUS_CLOSED.equals(status)) {
			throw Problem.valueOf(BAD_REQUEST, "status must be OPEN, ACKNOWLEDGED or CLOSED");
		}
		return status;
	}

	/** A stable dedup/grouping key for the income a warning concerns — the benefit/type before any “ (…” or “: …”. */
	private static String sourceKey(final String text) {
		if (text == null) {
			return "";
		}
		return text.split("[(:]", 2)[0].trim();
	}

	private static String key(final String type, final String sourceKey) {
		return type + "::" + ofNullable(sourceKey).orElse("");
	}

	private static List<String> ofList(final List<String> list) {
		return ofNullable(list).orElseGet(List::of);
	}

	private static Warning toWarning(final FaWarningEntity entity) {
		return Warning.create()
			.withId(entity.getId())
			.withType(entity.getType())
			.withTypeDisplayName(TYPE_DISPLAY_NAME.get(entity.getType()))
			.withSection(sectionOf(entity.getType(), entity.getSourceKey()))
			.withSourceKey(entity.getSourceKey())
			.withMessage(entity.getMessage())
			.withStatus(entity.getStatus())
			.withStatusDisplayName(STATUS_DISPLAY_NAME.get(entity.getStatus()))
			.withAutoResolved(entity.isAutoResolved())
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}
}
