package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;

/**
 * The errand-scoped facade for financial assistance income warnings: every call first scope-checks the errand (throws
 * {@code 404} when it is missing in this namespace/municipality) before delegating to {@link WarningService}. Split out
 * of the former FinancialAssistanceService god-service so the warnings surface is its own cohesive unit.
 */
@Service
@Transactional
public class FinancialAssistanceWarningService {

	private final ErrandService errandService;
	private final WarningService warningService;

	FinancialAssistanceWarningService(final ErrandService errandService, final WarningService warningService) {
		this.errandService = errandService;
		this.warningService = warningService;
	}

	/**
	 * Create a warning on an errand directly — the careM temp stage, with no Lifecare round-trip. Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	public Warning createWarning(final String municipalityId, final String namespace, final String errandId, final CreateWarningRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.create(errandId, request.getType(), request.getSourceKey(), request.getMessage());
	}

	/**
	 * The financial assistance income warnings on an errand — the acknowledgeable objects a caseworker reviews in Draken.
	 * Scoped: throws {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public List<Warning> listWarnings(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.list(errandId);
	}

	/**
	 * How many active (OPEN/ACKNOWLEDGED, not CLOSED) income warnings are on an errand — the badge count. Scoped: throws
	 * {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public long countActiveWarnings(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.countActive(errandId);
	}

	/**
	 * Acknowledge or close a warning on an errand. Scoped: throws {@code 404} when the errand or warning is missing,
	 * {@code 400} when the target status is not {@code ACKNOWLEDGED}/{@code CLOSED}.
	 */
	public Warning updateWarning(final String municipalityId, final String namespace, final String errandId, final String warningId, final String status) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return warningService.updateStatus(errandId, warningId, status);
	}
}
