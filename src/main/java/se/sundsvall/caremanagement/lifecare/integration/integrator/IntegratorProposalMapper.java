package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.AktualisationsAttachmentSenderTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringAttachmentTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsOrganizationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsSpecifyTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsWorkingStatusDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationHouseholdMemberDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpenseTypeDTO;
import generated.se.sundsvall.lifecareintegrator.ActualisationProposal;
import generated.se.sundsvall.lifecareintegrator.ActualisationReference;
import generated.se.sundsvall.lifecareintegrator.ActualisationType;
import generated.se.sundsvall.lifecareintegrator.AttachmentType;
import generated.se.sundsvall.lifecareintegrator.CalculationProposal;
import generated.se.sundsvall.lifecareintegrator.HouseholdMember;
import generated.se.sundsvall.lifecareintegrator.Lookup;
import generated.se.sundsvall.lifecareintegrator.Norm;
import generated.se.sundsvall.lifecareintegrator.Organization;
import generated.se.sundsvall.lifecareintegrator.ProposalCase;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.mapEach;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.IntegratorValues.toText;

/**
 * Translates the integrator's two proposals — the shape of a new calculation and of a new actualisation — back into
 * the FamilyCare DTOs {@code CalculationAssembler} and {@code ActualisationAssembler} build from.
 *
 * <p>
 * These are mostly code lists, and FamilyCare gives each list its own single-purpose DTO where the integrator reuses
 * one {@code Lookup} of id and name. Mapping is therefore repetitive rather than difficult: the same two fields, into
 * a different class each time.
 *
 * <p>
 * A proposal is what the handläggare's form is built from, so an absent list means "no choices offered" and is never
 * the same as a missing one. Every list therefore comes back empty rather than null.
 */
final class IntegratorProposalMapper {

	private IntegratorProposalMapper() {}

	// ---- The calculation proposal ------------------------------------------------------------------------------------

	static PersonBasedCalculationProposalDTO toCalculationProposal(final CalculationProposal proposal) {
		return ofNullable(proposal)
			.map(source -> new PersonBasedCalculationProposalDTO()
				.investigations(mapEach(source.getInvestigations(), IntegratorProposalMapper::toCalculationInvestigation))
				.services(mapEach(source.getServices(), IntegratorProposalMapper::toCalculationService))
				.norms(mapEach(source.getNorms(), IntegratorProposalMapper::toNorm))
				.householdMembers(mapEach(source.getHouseholdMembers(), IntegratorProposalMapper::toHouseholdMember))
				.calculationIncomeTypes(mapEach(source.getIncomeTypes(), IntegratorProposalMapper::toIncomeType))
				.calculationExpenseTypes(mapEach(source.getExpenseTypes(), IntegratorProposalMapper::toExpenseType))
				.calculationSpecialExpenseTypes(mapEach(source.getSpecialExpenseTypes(), IntegratorProposalMapper::toSpecialExpenseType))
				.aktualiseringMandatory(source.getActualisationMandatory())
				.numberOfFamilyMembersNotInHousehold(source.getNumberOfFamilyMembersNotInHousehold())
				.aktualiserings(mapEach(source.getActualisations(), IntegratorProposalMapper::toActualisationReference)))
			.orElse(null);
	}

	private static PersonBasedCalculationInvestigationDTO toCalculationInvestigation(final ProposalCase source) {
		return new PersonBasedCalculationInvestigationDTO()
			.id(source.getId())
			.type(source.getType())
			.name(source.getName())
			.startDate(toText(source.getStartDate()));
	}

	private static PersonBasedCalculationServiceDTO toCalculationService(final ProposalCase source) {
		return new PersonBasedCalculationServiceDTO()
			.id(source.getId())
			.type(source.getType())
			.name(source.getName())
			.startDate(toText(source.getStartDate()));
	}

	private static PersonBasedCalculationNormDTO toNorm(final Norm source) {
		return new PersonBasedCalculationNormDTO()
			.id(source.getId())
			.name(source.getName())
			.fromDate(toText(source.getValidFrom()))
			.toDate(toText(source.getValidTo()));
	}

	/** The one place in a proposal that names a person, so the same party-id rule applies as everywhere else. */
	private static PersonBasedCalculationHouseholdMemberDTO toHouseholdMember(final HouseholdMember source) {
		return new PersonBasedCalculationHouseholdMemberDTO()
			.personId(source.getPartyId())
			.name(source.getName())
			.childFromOtherHousehold(source.getChildFromOtherHousehold());
	}

	private static PersonBasedCalculationCalculationIncomeTypeDTO toIncomeType(final Lookup source) {
		return new PersonBasedCalculationCalculationIncomeTypeDTO().id(source.getId()).name(source.getName());
	}

	private static PersonBasedCalculationExpenseTypeDTO toExpenseType(final Lookup source) {
		return new PersonBasedCalculationExpenseTypeDTO().id(source.getId()).name(source.getName());
	}

	private static PersonBasedCalculationSpecialExpenseTypeDTO toSpecialExpenseType(final Lookup source) {
		return new PersonBasedCalculationSpecialExpenseTypeDTO().id(source.getId()).name(source.getName());
	}

	private static PersonBasedCalculationAktualiseringDTO toActualisationReference(final ActualisationReference source) {
		return new PersonBasedCalculationAktualiseringDTO()
			.id(source.getId())
			.type(source.getType())
			.date(toText(source.getDate()));
	}

	// ---- The actualisation proposal ----------------------------------------------------------------------------------

	static PersonBasedAktualiseringProposalDTO toActualisationProposal(final ActualisationProposal proposal) {
		return ofNullable(proposal)
			.map(source -> new PersonBasedAktualiseringProposalDTO()
				.actualisationTypes(mapEach(source.getActualisationTypes(), IntegratorProposalMapper::toActualisationType))
				.specifyTypes(mapEach(source.getSpecifyTypes(), lookup -> new PersonBasedAktualiseringsSpecifyTypeDTO().id(lookup.getId()).name(lookup.getName())))
				.workingStatus(mapEach(source.getWorkingStatus(), lookup -> new PersonBasedAktualiseringsWorkingStatusDTO().id(lookup.getId()).name(lookup.getName())))
				.organizations(mapEach(source.getOrganizations(), IntegratorProposalMapper::toOrganization))
				.investigations(mapEach(source.getInvestigations(), IntegratorProposalMapper::toActualisationInvestigation))
				.services(mapEach(source.getServices(), IntegratorProposalMapper::toActualisationService))
				.attachmentTypes(mapEach(source.getAttachmentTypes(), IntegratorProposalMapper::toAttachmentType)))
			.orElse(null);
	}

	private static PersonBasedAktualiseringsInfoDTO toActualisationType(final ActualisationType source) {
		return new PersonBasedAktualiseringsInfoDTO()
			.id(source.getId())
			.name(source.getName())
			.specifyTypeMandatory(source.getSpecifyTypeMandatory())
			.workingStatus(source.getWorkingStatus())
			.reasons(mapEach(source.getReasons(), lookup -> new PersonBasedAktualiseringsReasonDTO().id(lookup.getId()).name(lookup.getName())))
			.fromWho(mapEach(source.getFromWho(), lookup -> new PersonBasedAktualiseringsFromWhoDTO().id(lookup.getId()).name(lookup.getName())))
			.investigationTypes(mapEach(source.getInvestigationTypes(), lookup -> new PersonBasedAktualiseringsInvestigationTypeDTO().id(lookup.getId()).name(lookup.getName())))
			.serviceTypes(mapEach(source.getServiceTypes(), lookup -> new PersonBasedAktualiseringsServiceTypeDTO().id(lookup.getId()).name(lookup.getName())));
	}

	private static PersonBasedAktualiseringsOrganizationDTO toOrganization(final Organization source) {
		return new PersonBasedAktualiseringsOrganizationDTO()
			.id(source.getId())
			.unitId(source.getUnitId())
			.name(source.getName());
	}

	private static PersonBasedAktualiseringsInvestigationDTO toActualisationInvestigation(final ProposalCase source) {
		return new PersonBasedAktualiseringsInvestigationDTO()
			.id(source.getId())
			.type(source.getType())
			.name(source.getName())
			.startDate(toText(source.getStartDate()))
			.organisationId(source.getOrganisationId())
			.organisationUnitId(source.getOrganisationUnitId())
			.caseworkerId(source.getCaseworkerId());
	}

	private static PersonBasedAktualiseringsServiceDTO toActualisationService(final ProposalCase source) {
		return new PersonBasedAktualiseringsServiceDTO()
			.id(source.getId())
			.type(source.getType())
			.name(source.getName())
			.startDate(toText(source.getStartDate()))
			.organisationId(source.getOrganisationId())
			.organisationUnitId(source.getOrganisationUnitId())
			.caseworkerId(source.getCaseworkerId());
	}

	private static PersonBasedAktualiseringAttachmentTypeDTO toAttachmentType(final AttachmentType source) {
		return new PersonBasedAktualiseringAttachmentTypeDTO()
			.id(source.getId())
			.name(source.getName())
			.senderTypes(mapEach(source.getSenderTypes(), lookup -> new AktualisationsAttachmentSenderTypeDTO().id(lookup.getId()).name(lookup.getName())));
	}
}
