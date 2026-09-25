package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.ProposalMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * The shared basis both section proposals are derived from: the errand's calculation draft, the resolved household,
 * the application month, and the bistånd amount.
 * <p>
 * Once the handläggare has saved the normberäkning in Lifecare (the errand carries {@code lifecareCalculationId}), the
 * amount is that calculation's result as Lifecare computed it — its underskott, {@code -balance} — because Lifecare
 * applies the norm and jobbstimulans that the draft cannot. Before that, or when the saved calculation cannot be read,
 * the amount is estimated from the draft: the draft carries the income/expense sums but no norm, so the norm sum is
 * taken from the applicant's most recent Lifecare calculation before the application month (the previous household),
 * best-effort: a failed or empty Lifecare read leaves the norm (and thereby the estimate) unknown rather than guessing.
 * Nothing here is stored.
 */
@Service
public class ProposalBasisService {

	private static final Logger LOG = LoggerFactory.getLogger(ProposalBasisService.class);

	/** The amount is the result of the normberäkning saved in Lifecare. */
	public static final String AMOUNT_BASIS_LIFECARE_CALCULATION = "LIFECARE_CALCULATION";

	/** The amount is estimated from the draft and the previous Lifecare calculation's norm. */
	public static final String AMOUNT_BASIS_ESTIMATE = "ESTIMATE";

	/** How many months either side of the calculation period the saved-calculation lookup reaches. */
	static final int SAVED_CALCULATION_WINDOW_MONTHS = 2;

	/** Swedish explanation surfaced on the proposal when no amount could be estimated. */
	public static final String EXPLANATION_NO_NORM = "Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.";

	private final ErrandService errandService;
	private final DraftService draftService;
	private final HouseholdPartyService householdPartyService;
	private final LifecareCaseService lifecareCaseService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final FinancialAssistanceRepository financialAssistanceRepository;

	ProposalBasisService(final ErrandService errandService, final DraftService draftService, final HouseholdPartyService householdPartyService,
		final LifecareCaseService lifecareCaseService, final LifecareCaseHistoryService lifecareCaseHistoryService,
		final FinancialAssistanceRepository financialAssistanceRepository) {
		this.errandService = errandService;
		this.draftService = draftService;
		this.householdPartyService = householdPartyService;
		this.lifecareCaseService = lifecareCaseService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.financialAssistanceRepository = financialAssistanceRepository;
	}

	/**
	 * What a proposal is computed from. {@code applicationMonth} is empty when the draft header carries none;
	 * {@code estimatedAmount} is the bistånd (positive = underskott), empty when it is unknown; {@code amountBasis} says
	 * where it came from ({@link #AMOUNT_BASIS_LIFECARE_CALCULATION} or {@link #AMOUNT_BASIS_ESTIMATE}), empty with it;
	 * {@code normSum} is the saved calculation's norm, or the previous calculation's norm for an estimate — always as the
	 * positive cost it is, though FamilyCare's calculation listing carries it negated; {@code lifecareServiceId} is the
	 * errand's own EB insats in Lifecare, empty when the errand has none yet.
	 */
	public record ProposalBasis(
		CalculationDraft draft,
		HouseholdPartyService.Household household,
		Optional<YearMonth> applicationMonth,
		Optional<BigDecimal> normSum,
		Optional<BigDecimal> estimatedAmount,
		Optional<String> amountBasis,
		Optional<Integer> lifecareServiceId) {
	}

	/**
	 * Read the basis for an errand. Scoped: throws {@code 404} when the errand is missing in this namespace/municipality,
	 * or when it has no calculation draft yet.
	 */
	@Transactional(readOnly = true)
	public ProposalBasis basis(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var draft = draftService.get(errandId); // 404 when no draft
		final var household = householdPartyService.household(municipalityId, namespace, errandId);
		final var applicationMonth = ofNullable(draft.getApplicationMonth()).filter(month -> hasText(month)).map(YearMonth::parse);
		final var lifecareServiceId = financialAssistanceRepository.findByErrandId(errandId).map(FinancialAssistanceEntity::getLifecareServiceId);
		final var savedCalculation = household.applicantPartyId()
			.flatMap(applicant -> applicationMonth.flatMap(month -> savedCalculation(municipalityId, errandId, applicant, draft, month)));
		if (savedCalculation.isPresent()) {
			final var calculation = savedCalculation.get();
			return new ProposalBasis(draft, household, applicationMonth, ofNullable(calculation.normSum()).map(BigDecimal::abs), Optional.of(calculation.balance().negate()),
				Optional.of(AMOUNT_BASIS_LIFECARE_CALCULATION), lifecareServiceId);
		}
		final var normSum = household.applicantPartyId()
			.flatMap(applicant -> applicationMonth.flatMap(month -> previousNormSum(municipalityId, applicant, month)));
		final var estimatedAmount = normSum.map(norm -> ProposalMapper.estimatedAmount(draft, norm));
		return new ProposalBasis(draft, household, applicationMonth, normSum, estimatedAmount, estimatedAmount.map(amount -> AMOUNT_BASIS_ESTIMATE), lifecareServiceId);
	}

	/**
	 * The normberäkning saved in Lifecare for this errand, best-effort: empty when none is linked yet, when Lifecare
	 * cannot be read, or when the linked calculation is not found — the proposal then falls back to the estimate. Looked
	 * up by id among the applicant's calculations, since the listing is the read the Lifecare route offers. FamilyCare
	 * filters that listing on the calculation <em>date</em>, not the period: a proposal for October created on 25
	 * September is dated in September, and a calculation saved after its month is dated then — so the window reaches
	 * {@value #SAVED_CALCULATION_WINDOW_MONTHS} months either side of the period.
	 */
	private Optional<CalculationView> savedCalculation(final String municipalityId, final String errandId, final String applicant,
		final CalculationDraft draft, final YearMonth applicationMonth) {
		final var calculationId = financialAssistanceRepository.findByErrandId(errandId).map(FinancialAssistanceEntity::getLifecareCalculationId);
		if (calculationId.isEmpty()) {
			return Optional.empty();
		}
		final var periodFrom = ofNullable(draft.getCalculationFromDate()).orElseGet(() -> applicationMonth.atDay(1));
		final var periodTo = ofNullable(draft.getCalculationToDate()).orElseGet(applicationMonth::atEndOfMonth);
		final var from = periodFrom.minusMonths(SAVED_CALCULATION_WINDOW_MONTHS);
		final var to = periodTo.plusMonths(SAVED_CALCULATION_WINDOW_MONTHS);
		try {
			final var match = lifecareCaseHistoryService.listCalculations(municipalityId, applicant, from, to).stream()
				.filter(calculation -> Objects.equals(calculation.id(), calculationId.get()))
				.filter(calculation -> calculation.balance() != null)
				.findFirst();
			if (match.isEmpty()) {
				LOG.warn("Saved Lifecare calculation {} for errand {} was not found between {} and {} — the proposal amount is estimated", calculationId.get(), errandId, from, to);
			}
			return match;
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the saved Lifecare calculation {} for errand {} — the proposal amount is estimated", calculationId.get(), errandId, e);
			return Optional.empty();
		}
	}

	/**
	 * The previous calculation's norm sum as a positive cost, best-effort — a failed Lifecare read degrades to "unknown".
	 * FamilyCare carries it negated, and added as it came it turned every estimate into an avslag.
	 */
	private Optional<BigDecimal> previousNormSum(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return ofNullable(lifecareCaseService.previousHousehold(municipalityId, applicant, applicationMonth).normSum()).map(BigDecimal::abs);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation household — the proposal amount is left unknown", e);
			return Optional.empty();
		}
	}
}
