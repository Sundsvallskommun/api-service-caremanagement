package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;

import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.PAYMENT_PROPOSAL_TYPES;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_CO_APPLICANT_SPLIT_PAYMENT;

/**
 * The PAYMENT-section warnings: when there is a medsökande the caseworker is warned to check for delad utbetalning.
 * Reconciled when the DECISION section is approved, so the warning is on the errand when the caseworker moves on to the
 * payment.
 */
@Service
public class PaymentWarningService {

	static final String WARNING_CO_APPLICANT_SPLIT_PAYMENT = "Det finns medsökande i ärendet – kontrollera om det ska vara delad utbetalning";

	private final HouseholdPartyService householdPartyService;
	private final WarningService warningService;

	PaymentWarningService(final HouseholdPartyService householdPartyService, final WarningService warningService) {
		this.householdPartyService = householdPartyService;
		this.warningService = warningService;
	}

	/** Reconcile the PAYMENT-section warnings of an errand the caller has already scope-checked. */
	@Transactional
	public List<Warning> reconcile(final String municipalityId, final String namespace, final String errandId) {
		final var household = householdPartyService.household(municipalityId, namespace, errandId);
		return warningService.reconcileByTypes(errandId, PAYMENT_PROPOSAL_TYPES, warningInputs(household.coApplicantPresent()));
	}

	private static List<WarningService.WarningInput> warningInputs(final boolean coApplicantPresent) {
		if (!coApplicantPresent) {
			return List.of();
		}
		return List.of(new WarningService.WarningInput(TYPE_CO_APPLICANT_SPLIT_PAYMENT, "co-applicant", WARNING_CO_APPLICANT_SPLIT_PAYMENT));
	}
}
