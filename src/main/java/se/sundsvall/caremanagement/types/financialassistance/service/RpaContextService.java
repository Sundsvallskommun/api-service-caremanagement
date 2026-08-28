package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaContext;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;

/**
 * Assembles the {@link RpaContext} a robot fetches as its first step after picking up a queue item: the errand's
 * human-readable number plus the household's personal numbers, resolved on demand from the application's partyIds via
 * the citizen lookup. Serving the personal numbers here — instead of putting them in the queue item — keeps them out of
 * the Orchestrator queue store and makes every disclosure traceable in the errand's event log.
 */
@Service
public class RpaContextService {

	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CitizenService citizenService;

	RpaContextService(final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final CitizenService citizenService) {
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.citizenService = citizenService;
	}

	/** The robot context for an errand. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public RpaContext get(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElse(List.of());

		return new RpaContext(
			errand.getErrandNumber(),
			resolvePersonalNumber(municipalityId, persons, ROLE_APPLICANT),
			resolvePersonalNumber(municipalityId, persons, ROLE_CO_APPLICANT));
	}

	/** The personal number for the household member with the given role, or {@code null} when absent or unresolvable. */
	private String resolvePersonalNumber(final String municipalityId, final List<FaPerson> persons, final String role) {
		return persons.stream()
			.filter(person -> role.equals(person.getRole()))
			.map(FaPerson::getPartyId)
			.filter(StringUtils::hasText)
			.findFirst()
			.flatMap(partyId -> citizenService.getPersonalNumber(municipalityId, partyId))
			.orElse(null);
	}
}
