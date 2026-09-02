package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaContext;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;

/**
 * Assembles the {@link RpaContext} a robot fetches as its first step after picking up a queue item: the errand's
 * human-readable number plus the household's personal numbers, resolved on demand via the citizen lookup. The partyId
 * per role is taken from the errand's stakeholders first (the canonical promoted identity — some intake flows leave the
 * application payload's person list empty) and falls back to the financial assistance person rows. Serving the personal
 * numbers here — instead of putting them in the queue item — keeps them out of the Orchestrator queue store and makes
 * every disclosure traceable in the errand's event log.
 */
@Service
public class RpaContextService {

	private final ErrandService errandService;
	private final StakeholderService stakeholderService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CitizenService citizenService;

	RpaContextService(final ErrandService errandService, final StakeholderService stakeholderService,
		final FinancialAssistanceRepository financialAssistanceRepository, final CitizenService citizenService) {
		this.errandService = errandService;
		this.stakeholderService = stakeholderService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.citizenService = citizenService;
	}

	/** The robot context for an errand. Scoped: throws {@code 404} when the errand is missing here. */
	@Transactional(readOnly = true)
	public RpaContext get(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final var stakeholders = stakeholderService.readAll(municipalityId, namespace, errandId);
		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElse(List.of());

		return new RpaContext(
			errand.getErrandNumber(),
			resolvePersonalNumber(municipalityId, stakeholders, persons, ROLE_APPLICANT),
			resolvePersonalNumber(municipalityId, stakeholders, persons, ROLE_CO_APPLICANT));
	}

	/** The personal number for the household member with the given role, or {@code null} when absent or unresolvable. */
	private String resolvePersonalNumber(final String municipalityId, final List<Stakeholder> stakeholders, final List<FaPerson> persons, final String role) {
		return resolvePartyId(stakeholders, persons, role)
			.flatMap(partyId -> citizenService.getPersonalNumber(municipalityId, partyId))
			.orElse(null);
	}

	/**
	 * The partyId for a household role: the errand's stakeholder of that role first (the canonical promoted identity),
	 * falling back to the application payload's person row — some intake flows populate only one of the two.
	 */
	private static Optional<String> resolvePartyId(final List<Stakeholder> stakeholders, final List<FaPerson> persons, final String role) {
		return stakeholders.stream()
			.filter(stakeholder -> role.equals(stakeholder.getRole()))
			.map(Stakeholder::getExternalId)
			.filter(StringUtils::hasText)
			.findFirst()
			.or(() -> persons.stream()
				.filter(person -> role.equals(person.getRole()))
				.map(FaPerson::getPartyId)
				.filter(StringUtils::hasText)
				.findFirst());
	}
}
