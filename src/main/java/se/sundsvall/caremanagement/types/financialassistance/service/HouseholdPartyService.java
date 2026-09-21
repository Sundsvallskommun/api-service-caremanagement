package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;

/**
 * Resolves the errand's household parties for the section proposals: the applicant's personnummer (for the Lifecare
 * reads), whether there is a medsökande, and the applicant's application-payload person row (for the stated payment
 * account). Uses the same partyId resolution as {@link RpaContextService} — the errand's stakeholder of the role first,
 * the application payload's person row as fallback. Personal numbers are resolved on demand and never stored or logged.
 */
@Service
public class HouseholdPartyService {

	private final StakeholderService stakeholderService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final CitizenService citizenService;

	HouseholdPartyService(final StakeholderService stakeholderService, final FinancialAssistanceRepository financialAssistanceRepository,
		final CitizenService citizenService) {
		this.stakeholderService = stakeholderService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.citizenService = citizenService;
	}

	/**
	 * The resolved household: the applicant's personnummer (empty when unresolvable), the co-applicant presence, the
	 * applicant's payment details and display name.
	 */
	public record Household(Optional<String> applicantPersonalNumber, boolean coApplicantPresent, Optional<FaPerson> applicantPerson, Optional<String> applicantName) {
	}

	/** Resolve the household for an errand the caller has already scope-checked. */
	@Transactional(readOnly = true)
	public Household household(final String municipalityId, final String namespace, final String errandId) {
		final var stakeholders = stakeholderService.readAll(municipalityId, namespace, errandId);
		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElse(List.of());

		final var applicantPersonalNumber = RpaContextService.resolvePartyId(stakeholders, persons, ROLE_APPLICANT)
			.flatMap(partyId -> citizenService.getPersonalNumber(municipalityId, partyId));
		final var coApplicantPresent = RpaContextService.resolvePartyId(stakeholders, persons, ROLE_CO_APPLICANT).isPresent();
		final var applicantPerson = persons.stream()
			.filter(person -> ROLE_APPLICANT.equals(person.getRole()))
			.findFirst();
		final var applicantName = stakeholders.stream()
			.filter(stakeholder -> ROLE_APPLICANT.equals(stakeholder.getRole()))
			.map(HouseholdPartyService::displayName)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();

		return new Household(applicantPersonalNumber, coApplicantPresent, applicantPerson, applicantName);
	}

	private static Optional<String> displayName(final Stakeholder stakeholder) {
		final var name = (ofNullable(stakeholder.getFirstName()).orElse("") + " " + ofNullable(stakeholder.getLastName()).orElse("")).trim();
		if (hasText(name)) {
			return Optional.of(name);
		}
		return Optional.empty();
	}
}
