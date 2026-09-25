package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.HouseholdPartyService;
import se.sundsvall.caremanagement.types.financialassistance.service.LifecareServiceIdService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Loads an errand for its Lifecare calls, and links what those calls created back onto it.
 *
 * <p>
 * The Lifecare keys always come from here, never from the caller: a caller who could name any insats or person could
 * read or write any client in Lifecare through the integration account.
 * </p>
 */
@Service
public class LifecareErrandService {

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final FinancialAssistanceErrandService financialAssistanceErrandService;
	private final LifecareServiceIdService lifecareServiceIdService;
	private final HouseholdPartyService householdPartyService;
	private final CitizenService citizenService;

	LifecareErrandService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository,
		final FinancialAssistanceErrandService financialAssistanceErrandService, final LifecareServiceIdService lifecareServiceIdService,
		final HouseholdPartyService householdPartyService, final CitizenService citizenService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.financialAssistanceErrandService = financialAssistanceErrandService;
		this.lifecareServiceIdService = lifecareServiceIdService;
		this.householdPartyService = householdPartyService;
		this.citizenService = citizenService;
	}

	/**
	 * The errand with its Lifecare keys. Scope checked: 404 when the errand is not in this municipality and namespace.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the errand
	 */
	public LifecareErrand load(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = financialAssistanceRepository.findByErrandId(errandId);
		final var serviceId = lifecareServiceIdService.currentOrResolve(municipalityId, namespace, errandId);
		return new LifecareErrand(municipalityId, namespace, errandId, serviceId,
			entity.map(FinancialAssistanceEntity::getLifecareCalculationId).orElse(null),
			entity.map(FinancialAssistanceEntity::getLifecareDecisionId).orElse(null),
			entity.map(FinancialAssistanceEntity::getLifecarePaymentIds).orElse(null),
			entity.map(FinancialAssistanceEntity::getPeriodYear).orElse(null),
			entity.map(FinancialAssistanceEntity::getPeriodMonth).orElse(null));
	}

	/**
	 * The applicant's personnummer, which the few client-scoped Lifecare calls (the document list) are keyed on.
	 *
	 * @param  errand the errand
	 * @return        the personnummer, 12 digits
	 */
	public String applicantPersonalNumber(final LifecareErrand errand) {
		return householdPartyService.household(errand.municipalityId(), errand.namespace(), errand.errandId()).applicantPartyId()
			.flatMap(partyId -> citizenService.getPersonalNumber(errand.municipalityId(), partyId))
			.filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(CONFLICT, "The applicant's personnummer could not be resolved"));
	}

	/**
	 * Whether the household has a medsökande. Lifecare writes for such households are not built yet and are refused.
	 *
	 * @param  errand the errand
	 * @return        true when there is a medsökande
	 */
	public boolean coApplicantPresent(final LifecareErrand errand) {
		return householdPartyService.coApplicantPresent(errand.municipalityId(), errand.namespace(), errand.errandId());
	}

	/**
	 * Links a Lifecare calculation to the errand. Write-once: a different id than the one already linked is a 409.
	 *
	 * @param errand        the errand
	 * @param calculationId the Lifecare calculation
	 */
	public void linkCalculation(final LifecareErrand errand, final int calculationId) {
		financialAssistanceErrandService.updateData(errand.municipalityId(), errand.namespace(), errand.errandId(),
			FinancialAssistanceData.create().withLifecareCalculationId(calculationId));
	}

	/**
	 * Links a Lifecare decision to the errand.
	 *
	 * @param errand     the errand
	 * @param decisionId the Lifecare decision
	 */
	public void linkDecision(final LifecareErrand errand, final int decisionId) {
		financialAssistanceErrandService.updateData(errand.municipalityId(), errand.namespace(), errand.errandId(),
			FinancialAssistanceData.create().withLifecareDecisionId(decisionId));
	}

	/**
	 * Adds a Lifecare payment to the ones linked to the errand, keeping the ones already there.
	 *
	 * @param errand    the errand
	 * @param paymentId the Lifecare payment
	 */
	public void linkPayment(final LifecareErrand errand, final String paymentId) {
		final var current = financialAssistanceRepository.findByErrandId(errand.errandId())
			.map(FinancialAssistanceEntity::getLifecarePaymentIds)
			.map(ArrayList::new)
			.orElseGet(ArrayList::new);
		if (current.contains(paymentId)) {
			return;
		}
		current.add(paymentId);
		financialAssistanceErrandService.updateData(errand.municipalityId(), errand.namespace(), errand.errandId(),
			FinancialAssistanceData.create().withLifecarePaymentIds(current));
	}

	/**
	 * The signed-in caseworker's account, from X-Sent-By.
	 *
	 * @return the account name
	 */
	public String caller() {
		return Optional.ofNullable(Identifier.get())
			.map(Identifier::getValue)
			.filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, "Missing or malformed required header '" + Identifier.HEADER_NAME + "'"));
	}
}
