package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.ProposalMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * The shared basis both section proposals are derived from: the errand's calculation draft, the resolved household,
 * the application month, and the estimated bistånd. The draft carries the income/expense sums but no norm — the norm
 * sum is taken from the applicant's most recent Lifecare calculation before the application month (the previous
 * household), best-effort: a failed or empty Lifecare read leaves the norm (and thereby the estimate) unknown rather
 * than guessing. Nothing here is stored.
 */
@Service
public class ProposalBasisService {

	private static final Logger LOG = LoggerFactory.getLogger(ProposalBasisService.class);

	/** Swedish explanation surfaced on the proposal when no amount could be estimated. */
	public static final String EXPLANATION_NO_NORM = "Ingen norm kunde läsas från Lifecare – beloppet kunde inte beräknas.";

	private final ErrandService errandService;
	private final DraftService draftService;
	private final HouseholdPartyService householdPartyService;
	private final LifecareCaseService lifecareCaseService;

	ProposalBasisService(final ErrandService errandService, final DraftService draftService, final HouseholdPartyService householdPartyService,
		final LifecareCaseService lifecareCaseService) {
		this.errandService = errandService;
		this.draftService = draftService;
		this.householdPartyService = householdPartyService;
		this.lifecareCaseService = lifecareCaseService;
	}

	/**
	 * What a proposal is computed from. {@code applicationMonth} is empty when the draft header carries none;
	 * {@code normSum}/{@code estimatedAmount} are empty when the norm is unknown.
	 */
	public record ProposalBasis(
		CalculationDraft draft,
		HouseholdPartyService.Household household,
		Optional<YearMonth> applicationMonth,
		Optional<BigDecimal> normSum,
		Optional<BigDecimal> estimatedAmount) {
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
		final var normSum = household.applicantPersonalNumber()
			.flatMap(applicant -> applicationMonth.flatMap(month -> previousNormSum(applicant, month)));
		final var estimatedAmount = normSum.map(norm -> ProposalMapper.estimatedAmount(draft, norm));
		return new ProposalBasis(draft, household, applicationMonth, normSum, estimatedAmount);
	}

	/** The previous calculation's norm sum, best-effort — a failed Lifecare read degrades to "unknown". */
	private Optional<BigDecimal> previousNormSum(final String applicant, final YearMonth applicationMonth) {
		try {
			return ofNullable(lifecareCaseService.previousHousehold(applicant, applicationMonth).normSum());
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous calculation household — the proposal amount is left unknown", e);
			return Optional.empty();
		}
	}
}
