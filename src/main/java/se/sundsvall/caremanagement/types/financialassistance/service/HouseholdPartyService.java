package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;

/**
 * Resolves the errand's household parties for the section proposals: the applicant's partyId (for the Lifecare
 * reads), whether there is a medsökande, and the applicant's application-payload person row (for the stated payment
 * account). The partyId per role is the errand's stakeholder of the role first, the application payload's person row as
 * fallback ({@link #resolvePartyId}).
 */
@Service
public class HouseholdPartyService {

	private final StakeholderService stakeholderService;
	private final FinancialAssistanceRepository financialAssistanceRepository;

	HouseholdPartyService(final StakeholderService stakeholderService, final FinancialAssistanceRepository financialAssistanceRepository) {
		this.stakeholderService = stakeholderService;
		this.financialAssistanceRepository = financialAssistanceRepository;
	}

	/**
	 * The resolved household: the applicant's partyId (empty when the errand names no applicant), the co-applicant
	 * presence, the applicant's payment details and display name. The partyId is what the Lifecare reads take, so
	 * nothing here needs the citizen service.
	 */
	public record Household(Optional<String> applicantPartyId, boolean coApplicantPresent, Optional<FaPerson> applicantPerson, Optional<String> applicantName) {
	}

	/** Resolve the household for an errand the caller has already scope-checked. */
	@Transactional(readOnly = true)
	public Household household(final String municipalityId, final String namespace, final String errandId) {
		final var stakeholders = stakeholderService.readAll(municipalityId, namespace, errandId);
		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElse(List.of());

		final var applicantPartyId = resolvePartyId(stakeholders, persons, ROLE_APPLICANT);
		final var coApplicantPresent = resolvePartyId(stakeholders, persons, ROLE_CO_APPLICANT).isPresent();
		final var applicantPerson = persons.stream()
			.filter(person -> ROLE_APPLICANT.equals(person.getRole()))
			.findFirst();
		final var applicantName = stakeholders.stream()
			.filter(stakeholder -> ROLE_APPLICANT.equals(stakeholder.getRole()))
			.map(HouseholdPartyService::displayName)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();

		return new Household(applicantPartyId, coApplicantPresent, applicantPerson, applicantName);
	}

	/**
	 * The household children's first names by partyId, for naming a child's SSBTEK income on the applicant's column and in
	 * warnings. A child without a partyId cannot have an SSBTEK income and is left out; a child without a first name maps
	 * to an empty name, which the callers render as just "barn".
	 */
	static Map<String, String> childNames(final FinancialAssistanceEntity errand) {
		return ofNullable(errand)
			.map(FinancialAssistanceEntity::getChildren)
			.orElseGet(List::of)
			.stream()
			.filter(child -> hasText(child.getPartyId()))
			.collect(Collectors.toMap(FaChild::getPartyId, child -> ofNullable(child.getFirstName()).orElse(""), (first, duplicate) -> first));
	}

	/**
	 * The partyId for a household role: the errand's stakeholder of that role first (the canonical promoted identity),
	 * falling back to the application payload's person row — some intake flows populate only one of the two.
	 */
	static Optional<String> resolvePartyId(final List<Stakeholder> stakeholders, final List<FaPerson> persons, final String role) {
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

	/**
	 * Whether the errand's household has a medsökande. Local data only (stakeholders and the application's person rows) —
	 * no citizen lookup — so a caller on the daily prepare path cannot be failed by the citizen register.
	 */
	@Transactional(readOnly = true)
	public boolean coApplicantPresent(final String municipalityId, final String namespace, final String errandId) {
		final var persons = financialAssistanceRepository.findByErrandId(errandId)
			.map(FinancialAssistanceEntity::getPersons)
			.orElse(List.of());
		return resolvePartyId(stakeholderService.readAll(municipalityId, namespace, errandId), persons, ROLE_CO_APPLICANT).isPresent();
	}

	private static Optional<String> displayName(final Stakeholder stakeholder) {
		final var name = (ofNullable(stakeholder.getFirstName()).orElse("") + " " + ofNullable(stakeholder.getLastName()).orElse("")).trim();
		if (hasText(name)) {
			return Optional.of(name);
		}
		return Optional.empty();
	}
}
