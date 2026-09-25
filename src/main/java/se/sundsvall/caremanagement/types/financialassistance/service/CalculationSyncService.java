package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationSyncRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationSyncEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_APPLICATION;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Keeps the normberäkning saved in Lifecare in step with SSBTEK after it has been created. FamilyCare, careM's route to
 * Lifecare, can create a calculation but not change one; only Draken's BFF can, through ProfessionalWeb. So careM works
 * out what differs and the BFF writes it:
 *
 * <ul>
 * <li>the daily prepare {@link #recordSsbtek records} the latest SSBTEK amount per income, compares it with the
 * calculation read from Lifecare and raises one {@link WarningService#TYPE_SSBTEK_CALCULATION_DIFF} warning per
 * disagreement that SSBTEK caused — see {@link #warnings};</li>
 * <li>Draken's BFF {@link #changes reads} the disagreements, writes the {@code AUTO} ones into the calculation (and the
 * {@code CONFIRM} ones the caseworker accepts), and {@link #applied acknowledges} what it wrote.</li>
 * </ul>
 *
 * An income may be written without asking only when the calculation still holds exactly what the system last wrote
 * there — the proposal the prepare step created ({@link #seedFromProposal}), or an earlier acknowledged write. Anything
 * else is a caseworker's edit, and a caseworker's edit is never overwritten without them saying so.
 */
@Service
public class CalculationSyncService {

	private static final Logger LOG = LoggerFactory.getLogger(CalculationSyncService.class);

	public static final String ROLE_APPLICANT = "APPLICANT";
	public static final String ROLE_CO_APPLICANT = "CO_APPLICANT";

	static final String KIND_ADD = "ADD";
	static final String KIND_CHANGE = "CHANGE";
	static final String KIND_GONE = "GONE";

	static final String MODE_AUTO = "AUTO";
	static final String MODE_CONFIRM = "CONFIRM";

	/** The calculation is saved as final in Lifecare, which allows no change. */
	static final String REASON_FINAL = "FINAL";
	/** careM has no record of what the system wrote to this calculation — created by Draken before this existed. */
	static final String REASON_NO_BASELINE = "NO_BASELINE";
	/** The calculation holds something other than what the system last wrote: a caseworker has changed it. */
	static final String REASON_EDITED = "EDITED";
	/** The system wrote the income, or chose not to, and it is not in the calculation: the caseworker decided that. */
	static final String REASON_REMOVED = "REMOVED";
	/** The type has several rows in the calculation, so there is no one row to change. */
	static final String REASON_MULTIPLE_ROWS = "MULTIPLE_ROWS";
	/** SSBTEK not reporting an income does not prove it has stopped, so taking one out is always the caseworker's call. */
	static final String REASON_GONE_FROM_SSBTEK = "GONE_FROM_SSBTEK";

	private static final Set<String> OWNED_WARNING_TYPES = Set.of(WarningService.TYPE_SSBTEK_CALCULATION_DIFF);
	private static final String ERROR_NO_CALCULATION = "No normberäkning saved in Lifecare is linked to errand %s";

	private final FaCalculationSyncRepository syncRepository;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final DraftService draftService;
	private final HouseholdPartyService householdPartyService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final WarningService warningService;
	private final ErrandService errandService;

	CalculationSyncService(final FaCalculationSyncRepository syncRepository, final FinancialAssistanceRepository financialAssistanceRepository,
		final DraftService draftService, final HouseholdPartyService householdPartyService, final LifecareCaseHistoryService lifecareCaseHistoryService,
		final WarningService warningService, final ErrandService errandService) {
		this.syncRepository = syncRepository;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.draftService = draftService;
		this.householdPartyService = householdPartyService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.warningService = warningService;
		this.errandService = errandService;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Prepare path
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Record what the prepare step posted to Lifecare as the proposal: the SSBTEK amount of every draft income, and — as
	 * what the system wrote — the process amount of every side a caseworker left alone. A side the caseworker changed,
	 * and a row the caseworker soft-deleted, is recorded as written with no amount: the calculation's value there is the
	 * caseworker's, so a later SSBTEK change to it is theirs to confirm. The incomes declared in the application
	 * ({@code origin = APPLICATION}) are left out: SSBTEK never reported them, so they are no SSBTEK baseline.
	 *
	 * @param draftIncomes every income row of the draft, soft-deleted ones included
	 */
	@Transactional
	public void seedFromProposal(final String errandId, final List<FaNormIncomeEntity> draftIncomes) {
		final var now = now();
		final var rows = new LinkedHashMap<String, FaCalculationSyncEntity>();
		ofNullable(draftIncomes).orElseGet(List::of).stream()
			.filter(income -> !ORIGIN_APPLICATION.equals(income.getOrigin()))
			.filter(income -> StringUtils.hasText(income.getTypeName()))
			.forEach(income -> {
				seedSide(rows, errandId, income, ROLE_APPLICANT, income.getApplicantProcessAmount(), income.getApplicantCaseworkerAmount(), now);
				seedSide(rows, errandId, income, ROLE_CO_APPLICANT, income.getCoapplicantProcessAmount(), income.getCoapplicantCaseworkerAmount(), now);
			});
		syncRepository.saveAll(rows.values());
	}

	private static void seedSide(final Map<String, FaCalculationSyncEntity> rows, final String errandId, final FaNormIncomeEntity income, final String role,
		final BigDecimal processAmount, final BigDecimal caseworkerAmount, final OffsetDateTime now) {
		final var process = nonZero(processAmount);
		final var caseworker = nonZero(caseworkerAmount);
		if ((process == null) && (caseworker == null)) {
			return;
		}
		final var typeKey = normalize(income.getTypeName());
		final var existing = rows.get(key(typeKey, role));
		if (existing != null) {
			// Two draft rows of one type: the calculation sums them, and neither amount alone is what the system wrote.
			existing.setSsbtekAmount(sum(existing.getSsbtekAmount(), process));
			existing.setSsbtekBaselineAmount(sum(existing.getSsbtekBaselineAmount(), process));
			existing.setSystemWrittenAmount(null);
			return;
		}
		final var systemUntouched = !income.isDeleted() && (caseworker == null);
		final BigDecimal written;
		if (systemUntouched) {
			written = process;
		} else {
			written = null;
		}
		rows.put(key(typeKey, role), FaCalculationSyncEntity.create()
			.withErrandId(errandId)
			.withIncomeTypeKey(typeKey)
			.withIncomeTypeId(income.getTypeId())
			.withIncomeTypeName(income.getTypeName())
			.withRole(role)
			.withSsbtekAmount(process)
			.withSsbtekBaselineAmount(process)
			.withSsbtekReadAt(now)
			.withSystemWrittenAmount(written)
			.withSystemWrittenAt(now));
	}

	/**
	 * Record this run's SSBTEK amounts — one per income type and side — and mark every income SSBTEK no longer reports.
	 * Only the process amounts count: a caseworker's figure is not what SSBTEK said.
	 *
	 * @param processIncomes the income rows the process computed on this run (not merged into any draft)
	 */
	@Transactional
	public void recordSsbtek(final String errandId, final List<FaNormIncomeEntity> processIncomes) {
		final var now = now();
		final var existing = new HashMap<String, FaCalculationSyncEntity>();
		syncRepository.findByErrandId(errandId).forEach(row -> existing.put(key(row.getIncomeTypeKey(), row.getRole()), row));

		final var fresh = new LinkedHashMap<String, FaCalculationSyncEntity>();
		ofNullable(processIncomes).orElseGet(List::of).stream()
			.filter(income -> StringUtils.hasText(income.getTypeName()))
			.forEach(income -> {
				recordSide(existing, fresh, errandId, income, ROLE_APPLICANT, income.getApplicantProcessAmount(), now);
				recordSide(existing, fresh, errandId, income, ROLE_CO_APPLICANT, income.getCoapplicantProcessAmount(), now);
			});

		existing.entrySet().stream()
			.filter(entry -> !fresh.containsKey(entry.getKey()))
			.map(Map.Entry::getValue)
			.forEach(row -> fresh.put(key(row.getIncomeTypeKey(), row.getRole()), row.withSsbtekAmount(null).withSsbtekReadAt(now)));
		syncRepository.saveAll(fresh.values());
	}

	private static void recordSide(final Map<String, FaCalculationSyncEntity> existing, final Map<String, FaCalculationSyncEntity> fresh, final String errandId,
		final FaNormIncomeEntity income, final String role, final BigDecimal amount, final OffsetDateTime now) {
		final var ssbtek = nonZero(amount);
		if (ssbtek == null) {
			return;
		}
		final var typeKey = normalize(income.getTypeName());
		final var rowKey = key(typeKey, role);
		final var row = ofNullable(fresh.get(rowKey)).or(() -> ofNullable(existing.get(rowKey)))
			.orElseGet(() -> FaCalculationSyncEntity.create().withErrandId(errandId).withIncomeTypeKey(typeKey).withRole(role));
		final BigDecimal total;
		if (fresh.containsKey(rowKey)) {
			total = sum(row.getSsbtekAmount(), ssbtek);
		} else {
			total = ssbtek;
		}
		fresh.put(rowKey, row
			.withIncomeTypeId(ofNullable(income.getTypeId()).orElse(row.getIncomeTypeId()))
			.withIncomeTypeName(income.getTypeName())
			.withSsbtekAmount(total)
			.withSsbtekReadAt(now));
	}

	/**
	 * Compare the recorded SSBTEK amounts with the linked calculation and reconcile the warnings. Best-effort: when the
	 * calculation cannot be read, or is not found in the period, the warnings are left exactly as they were — an
	 * unread calculation is not evidence that it matches.
	 */
	@Transactional
	public void reconcileWarnings(final String municipalityId, final String errandId, final String applicantPartyId, final Integer calculationId,
		final LocalDate fromDate, final LocalDate toDate) {
		final Optional<CalculationView> calculation;
		try {
			calculation = findCalculation(municipalityId, applicantPartyId, calculationId, fromDate, toDate);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read Lifecare calculation {} for errand {} ({}) — the SSBTEK comparison is skipped", calculationId,
				sanitizeForLogging(errandId), e.getClass().getSimpleName());
			return;
		}
		if (calculation.isEmpty()) {
			LOG.warn("Lifecare calculation {} for errand {} was not found between {} and {} — the SSBTEK comparison is skipped", calculationId,
				sanitizeForLogging(errandId), fromDate, toDate);
			return;
		}
		warningService.reconcileByTypes(errandId, OWNED_WARNING_TYPES, warnings(syncRepository.findByErrandId(errandId), calculation.get()));
		// The draft is frozen now; the caseworker deals with what its warnings named in the calculation itself.
		warningService.closeResolvedDraftWarnings(errandId, calculation.get());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Draken path
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * The disagreements between SSBTEK and the linked calculation, read live from Lifecare. Scoped: 404 when the errand
	 * is missing, has no linked calculation or no draft period, or the calculation is not found; 502 when Lifecare
	 * cannot be read. Has no side effects — the warnings are the daily prepare's and {@link #applied}'s business.
	 */
	@Transactional(readOnly = true)
	public SsbtekChanges changes(final String municipalityId, final String namespace, final String errandId) {
		final var context = context(municipalityId, namespace, errandId);
		final Optional<CalculationView> found;
		try {
			found = findCalculation(municipalityId, context.applicantPartyId(), context.calculationId(), context.fromDate(), context.toDate());
		} catch (final RuntimeException e) {
			LOG.warn("Could not read Lifecare calculation {} for errand {} ({})", context.calculationId(), sanitizeForLogging(errandId), e.getClass().getSimpleName());
			throw Problem.valueOf(BAD_GATEWAY, "The normberäkning could not be read from Lifecare");
		}
		final var calculation = found
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Lifecare calculation %s is not found in the calculation period".formatted(context.calculationId())));
		final var rows = syncRepository.findByErrandId(errandId);
		final var ssbtekReadAt = rows.stream().map(FaCalculationSyncEntity::getSsbtekReadAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
		return new SsbtekChanges(context.calculationId(), Boolean.TRUE.equals(calculation.isFinal()), now(), ssbtekReadAt, compare(rows, calculation));
	}

	/**
	 * Record what Draken's BFF wrote into the calculation, as what the system last wrote there, then bring the warnings
	 * up to date (best-effort). 409 when the calculation written to is not the one linked to the errand.
	 */
	@Transactional
	public void applied(final String municipalityId, final String namespace, final String errandId, final AppliedSsbtekChanges request) {
		final var context = context(municipalityId, namespace, errandId);
		if (!Objects.equals(context.calculationId(), request.calculationId())) {
			throw Problem.valueOf(CONFLICT, "Calculation %s is not the one linked to errand %s (%s)".formatted(request.calculationId(), errandId, context.calculationId()));
		}
		final var now = now();
		final var rows = new HashMap<String, FaCalculationSyncEntity>();
		syncRepository.findByErrandId(errandId).forEach(row -> rows.put(key(row.getIncomeTypeKey(), row.getRole()), row));
		request.applied().forEach(change -> {
			final var typeKey = normalize(change.incomeType());
			final var row = rows.computeIfAbsent(key(typeKey, change.role()), ignored -> FaCalculationSyncEntity.create()
				.withErrandId(errandId).withIncomeTypeKey(typeKey).withIncomeTypeName(change.incomeType()).withRole(change.role()));
			// The calculation now holds what the BFF wrote, in view of what SSBTEK says now: that is the new baseline.
			row.withSystemWrittenAmount(nonZero(change.amount())).withSystemWrittenAt(now).withSsbtekBaselineAmount(row.getSsbtekAmount());
		});
		syncRepository.saveAll(rows.values());
		reconcileWarnings(municipalityId, errandId, context.applicantPartyId(), context.calculationId(), context.fromDate(), context.toDate());
	}

	private record SyncContext(Integer calculationId, String applicantPartyId, LocalDate fromDate, LocalDate toDate) {}

	private SyncContext context(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var calculationId = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getLifecareCalculationId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_NO_CALCULATION.formatted(errandId)));
		final var header = draftService.header(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No calculation draft for errand %s — it is purged once the errand is decided".formatted(errandId)));
		final var applicant = householdPartyService.household(municipalityId, namespace, errandId).applicantPartyId()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No applicant on errand %s".formatted(errandId)));
		return new SyncContext(calculationId, applicant, header.getCalculationFromDate(), header.getCalculationToDate());
	}

	/**
	 * The linked calculation, looked up by id among the applicant's. FamilyCare filters that listing on the calculation
	 * <em>date</em>, not the period — the October proposal the prepare step creates on 25 September is dated in
	 * September — so the lookup reaches {@link ProposalBasisService#SAVED_CALCULATION_WINDOW_MONTHS} months either side of
	 * the period, as the decision proposal's does.
	 */
	private Optional<CalculationView> findCalculation(final String municipalityId, final String applicantPartyId, final Integer calculationId,
		final LocalDate fromDate, final LocalDate toDate) {
		final var from = fromDate.minusMonths(ProposalBasisService.SAVED_CALCULATION_WINDOW_MONTHS);
		final var to = toDate.plusMonths(ProposalBasisService.SAVED_CALCULATION_WINDOW_MONTHS);
		return lifecareCaseHistoryService.listCalculations(municipalityId, applicantPartyId, from, to).stream()
			.filter(calculation -> Objects.equals(calculation.id(), calculationId))
			.findFirst();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// The comparison
	// ------------------------------------------------------------------------------------------------------------------

	/** One income type on one side of the calculation: its summed amount, and how many rows the type has. */
	private record LifecareIncome(BigDecimal amount, int rows) {}

	/**
	 * What differs between the recorded SSBTEK amounts and the calculation. Only incomes careM has a record of are
	 * compared; an income a caseworker added in Lifecare on their own is none of SSBTEK's business.
	 */
	static List<SsbtekChange> compare(final List<FaCalculationSyncEntity> rows, final CalculationView calculation) {
		final var isFinal = Boolean.TRUE.equals(calculation.isFinal());
		final var lifecare = lifecareIncomes(calculation);
		final var baselineExists = rows.stream().anyMatch(row -> row.getSystemWrittenAt() != null);
		final var changes = new ArrayList<SsbtekChange>();
		rows.stream()
			.sorted(Comparator.comparing(FaCalculationSyncEntity::getIncomeTypeKey).thenComparing(FaCalculationSyncEntity::getRole))
			.forEach(row -> change(row, lifecare.get(key(row.getIncomeTypeKey(), row.getRole())), isFinal, baselineExists).ifPresent(changes::add));
		return changes;
	}

	private static Optional<SsbtekChange> change(final FaCalculationSyncEntity row, final LifecareIncome lifecare, final boolean isFinal, final boolean baselineExists) {
		final var ssbtek = nonZero(row.getSsbtekAmount());
		final var inLifecare = ofNullable(lifecare).map(LifecareIncome::amount).orElse(null);
		final var written = row.getSystemWrittenAt() != null;
		final var systemAmount = nonZero(row.getSystemWrittenAmount());

		if ((ssbtek != null) && (inLifecare == null)) {
			return Optional.of(toChange(row, KIND_ADD, finalOr(isFinal, addReason(written, baselineExists)), ssbtek, null));
		}
		if ((ssbtek != null) && !sameAmount(ssbtek, inLifecare)) {
			return Optional.of(toChange(row, KIND_CHANGE, finalOr(isFinal, changeReason(lifecare.rows(), written, systemAmount, inLifecare)), ssbtek, inLifecare));
		}
		// Only an income the system itself wrote is flagged as gone, and only once SSBTEK has been read without it.
		if ((ssbtek == null) && (inLifecare != null) && (systemAmount != null) && (row.getSsbtekReadAt() != null)) {
			return Optional.of(toChange(row, KIND_GONE, finalOr(isFinal, Optional.of(REASON_GONE_FROM_SSBTEK)), null, inLifecare));
		}
		return Optional.empty();
	}

	/**
	 * Why an income missing from the calculation needs confirming: the system wrote it (or chose not to) and it is not
	 * there, so a caseworker took it out; or careM has no record of this calculation at all. Empty: a new income.
	 */
	private static Optional<String> addReason(final boolean written, final boolean baselineExists) {
		if (written) {
			return Optional.of(REASON_REMOVED);
		}
		if (!baselineExists) {
			return Optional.of(REASON_NO_BASELINE);
		}
		return Optional.empty();
	}

	/** Why a changed amount needs confirming. Empty: the calculation still holds what the system wrote. */
	private static Optional<String> changeReason(final int rows, final boolean written, final BigDecimal systemAmount, final BigDecimal inLifecare) {
		if (rows > 1) {
			return Optional.of(REASON_MULTIPLE_ROWS);
		}
		if (!written) {
			return Optional.of(REASON_NO_BASELINE);
		}
		if (!sameAmount(inLifecare, systemAmount)) {
			return Optional.of(REASON_EDITED);
		}
		return Optional.empty();
	}

	/** A final calculation takes no change, whatever else holds. */
	private static Optional<String> finalOr(final boolean isFinal, final Optional<String> reason) {
		if (isFinal) {
			return Optional.of(REASON_FINAL);
		}
		return reason;
	}

	private static SsbtekChange toChange(final FaCalculationSyncEntity row, final String kind, final Optional<String> reason, final BigDecimal ssbtek,
		final BigDecimal inLifecare) {
		final String mode;
		if (reason.isPresent()) {
			mode = MODE_CONFIRM;
		} else {
			mode = MODE_AUTO;
		}
		return new SsbtekChange(kind, mode, reason.orElse(null), row.getRole(), row.getIncomeTypeId(), row.getIncomeTypeName(), ssbtek, inLifecare);
	}

	private static Map<String, LifecareIncome> lifecareIncomes(final CalculationView calculation) {
		final var incomes = ofNullable(calculation.incomes()).orElseGet(List::of).stream()
			.filter(income -> StringUtils.hasText(income.type()))
			.toList();
		final var rowsPerType = new HashMap<String, Integer>();
		incomes.forEach(income -> rowsPerType.merge(normalize(income.type()), 1, Integer::sum));

		final var result = new HashMap<String, LifecareIncome>();
		incomes.forEach(income -> {
			final var typeKey = normalize(income.type());
			addSide(result, typeKey, ROLE_APPLICANT, income.amountApplicant(), rowsPerType.get(typeKey));
			addSide(result, typeKey, ROLE_CO_APPLICANT, income.amountCoApplicant(), rowsPerType.get(typeKey));
		});
		return result;
	}

	private static void addSide(final Map<String, LifecareIncome> result, final String typeKey, final String role, final BigDecimal amount, final int rows) {
		ofNullable(nonZero(amount)).ifPresent(value -> result.merge(key(typeKey, role), new LifecareIncome(value, rows),
			(first, second) -> new LifecareIncome(first.amount().add(second.amount()), rows)));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Warnings
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * The warnings: one per disagreement with the calculation that SSBTEK caused, i.e. on an income whose SSBTEK amount
	 * has moved since the calculation was last aligned with it. While SSBTEK still says what it said then, a calculation
	 * that differs is the caseworker's own edit — theirs to make, and no news to them. (The {@link #changes} read still
	 * lists every disagreement, for the BFF to propose.)
	 */
	static List<WarningService.WarningInput> warnings(final List<FaCalculationSyncEntity> rows, final CalculationView calculation) {
		final var moved = rows.stream().filter(CalculationSyncService::ssbtekMoved).toList();
		final var baselines = new HashMap<String, BigDecimal>();
		moved.forEach(row -> baselines.put(key(row.getIncomeTypeKey(), row.getRole()), nonZero(row.getSsbtekBaselineAmount())));
		return compare(moved, calculation).stream()
			.map(change -> toWarning(change, baselines.get(key(normalize(change.incomeType()), change.role()))))
			.toList();
	}

	private static boolean ssbtekMoved(final FaCalculationSyncEntity row) {
		return !Objects.equals(nonZero(row.getSsbtekAmount()), nonZero(row.getSsbtekBaselineAmount()));
	}

	/**
	 * One warning per SSBTEK change. The text says only what SSBTEK said before and now — never the calculation's amount,
	 * which the caseworker may change at any time and the warning would then misstate until the next run. The SSBTEK
	 * amount is part of the key, so a warning the caseworker has dealt with stays dealt with until SSBTEK gives a new
	 * amount — and closes itself once the calculation matches.
	 *
	 * @param baseline the SSBTEK amount the calculation was aligned with; null when SSBTEK had not reported the income
	 */
	static WarningService.WarningInput toWarning(final SsbtekChange change, final BigDecimal baseline) {
		final var sourceKey = String.join(":", "ssbtek-sync", change.role(), normalize(change.incomeType()), plain(change.ssbtekAmount()));
		final var subject = change.incomeType() + " (" + roleLabel(change.role()) + ")";
		final String message;
		if (change.ssbtekAmount() == null) {
			message = "SSBTEK rapporterar inte längre " + subject + ", tidigare " + kronor(baseline);
		} else if (baseline == null) {
			message = "Ny inkomst i SSBTEK: " + subject + " " + kronor(change.ssbtekAmount());
		} else {
			message = "SSBTEK har ändrats: " + subject + " från " + kronor(baseline) + " till " + kronor(change.ssbtekAmount());
		}
		return new WarningService.WarningInput(WarningService.TYPE_SSBTEK_CALCULATION_DIFF, sourceKey, message);
	}

	private static String roleLabel(final String role) {
		if (ROLE_CO_APPLICANT.equals(role)) {
			return "medsökande";
		}
		return "sökande";
	}

	private static String kronor(final BigDecimal amount) {
		return plain(amount) + " kr";
	}

	private static String plain(final BigDecimal amount) {
		return ofNullable(amount).map(value -> value.stripTrailingZeros().toPlainString()).orElse("-");
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * A Lifecare income type name as it is matched: trimmed and lower case, the way Lifecare's own names are compared
	 * elsewhere in careM ({@code MapperUtil.normalize}, which the module boundary keeps out of reach).
	 */
	static String normalize(final String typeName) {
		return ofNullable(typeName).map(name -> name.trim().toLowerCase(Locale.ROOT)).orElse("");
	}

	private static String key(final String typeKey, final String role) {
		return typeKey + "|" + role;
	}

	/** An amount that is there: null and zero both mean the income is absent. */
	static BigDecimal nonZero(final BigDecimal amount) {
		if ((amount == null) || (amount.signum() == 0)) {
			return null;
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private static boolean sameAmount(final BigDecimal first, final BigDecimal second) {
		return (first != null) && (second != null) && (first.compareTo(second) == 0);
	}

	private static BigDecimal sum(final BigDecimal first, final BigDecimal second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.add(second);
	}

	private static OffsetDateTime now() {
		return OffsetDateTime.now(ZoneId.systemDefault());
	}
}
