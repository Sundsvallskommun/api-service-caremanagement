package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Asset;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Child;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Cost;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Income;
import se.sundsvall.caremanagement.types.financialassistance.api.model.JobApplication;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PendingBenefit;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Person;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PlannedActivity;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Planning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaAsset;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaJobApplication;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPendingBenefit;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPlannedActivity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPlanning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;

/**
 * Maps the strongly-typed {@link FinancialAssistanceData} payload to and from the {@link FinancialAssistanceEntity}
 * aggregate (scalars + the nine {@code @ElementCollection} value lists), and assembles the
 * {@link FinancialAssistanceView}
 * response from the core {@link Errand} envelope plus the typed data. Null-safe throughout; null lists stay null.
 */
public final class FinancialAssistanceMapper {

	private FinancialAssistanceMapper() {}

	public static FinancialAssistanceEntity toEntity(final FinancialAssistanceData data, final String errandId) {
		return ofNullable(data)
			.map(d -> FinancialAssistanceEntity.create()
				.withErrandId(errandId)
				.withApplicationType(d.getApplicationType())
				.withMaritalStatus(d.getMaritalStatus())
				.withPeriodMonth(d.getPeriodMonth())
				.withPeriodYear(d.getPeriodYear())
				.withPeriodChoice(d.getPeriodChoice())
				.withNormType(d.getNormType())
				.withOtherBenefitDescription(d.getOtherBenefitDescription())
				.withLivelihoodDescription(d.getLivelihoodDescription())
				.withHasChildrenUnder21(d.getHasChildrenUnder21())
				.withChildrenResidenceChanged(d.getChildrenResidenceChanged())
				.withChildrenResidenceChangeDescription(d.getChildrenResidenceChangeDescription())
				.withHousingForm(d.getHousingForm())
				.withHousingPersonCount(d.getHousingPersonCount())
				.withHousingRoomsPlusKitchen(d.getHousingRoomsPlusKitchen())
				.withHousingDescription(d.getHousingDescription())
				.withHousingChanged(d.getHousingChanged())
				.withHousingChangeDescription(d.getHousingChangeDescription())
				.withHasIncomes(d.getHasIncomes())
				.withHasPendingBenefits(d.getHasPendingBenefits())
				.withHasAssets(d.getHasAssets())
				.withStaysInMunicipality(d.getStaysInMunicipality())
				.withStayDescription(d.getStayDescription())
				.withAttestation(d.getAttestation())
				.withAttestedAt(d.getAttestedAt())
				.withChildren(toFaChildren(d.getChildren()))
				.withCosts(toFaCosts(d.getCosts()))
				.withIncomes(toFaIncomes(d.getIncomes()))
				.withPendingBenefits(toFaPendingBenefits(d.getPendingBenefits()))
				.withAssets(toFaAssets(d.getAssets()))
				.withPersons(toFaPersons(d.getPersons()))
				.withPlannings(toFaPlannings(d.getPlannings()))
				.withPlannedActivities(toFaPlannedActivities(d.getPlannedActivities()))
				.withJobApplications(toFaJobApplications(d.getJobApplications())))
			.orElse(null);
	}

	public static FinancialAssistanceData toData(final FinancialAssistanceEntity entity) {
		return ofNullable(entity)
			.map(e -> FinancialAssistanceData.create()
				.withApplicationType(e.getApplicationType())
				.withMaritalStatus(e.getMaritalStatus())
				.withPeriodMonth(e.getPeriodMonth())
				.withPeriodYear(e.getPeriodYear())
				.withPeriodChoice(e.getPeriodChoice())
				.withNormType(e.getNormType())
				.withOtherBenefitDescription(e.getOtherBenefitDescription())
				.withLivelihoodDescription(e.getLivelihoodDescription())
				.withHasChildrenUnder21(e.getHasChildrenUnder21())
				.withChildrenResidenceChanged(e.getChildrenResidenceChanged())
				.withChildrenResidenceChangeDescription(e.getChildrenResidenceChangeDescription())
				.withHousingForm(e.getHousingForm())
				.withHousingPersonCount(e.getHousingPersonCount())
				.withHousingRoomsPlusKitchen(e.getHousingRoomsPlusKitchen())
				.withHousingDescription(e.getHousingDescription())
				.withHousingChanged(e.getHousingChanged())
				.withHousingChangeDescription(e.getHousingChangeDescription())
				.withHasIncomes(e.getHasIncomes())
				.withHasPendingBenefits(e.getHasPendingBenefits())
				.withHasAssets(e.getHasAssets())
				.withStaysInMunicipality(e.getStaysInMunicipality())
				.withStayDescription(e.getStayDescription())
				.withAttestation(e.getAttestation())
				.withAttestedAt(e.getAttestedAt())
				.withChildren(toChildren(e.getChildren()))
				.withCosts(toCosts(e.getCosts()))
				.withIncomes(toIncomes(e.getIncomes()))
				.withPendingBenefits(toPendingBenefits(e.getPendingBenefits()))
				.withAssets(toAssets(e.getAssets()))
				.withPersons(toPersons(e.getPersons()))
				.withPlannings(toPlannings(e.getPlannings()))
				.withPlannedActivities(toPlannedActivities(e.getPlannedActivities()))
				.withJobApplications(toJobApplications(e.getJobApplications())))
			.orElse(null);
	}

	public static FinancialAssistanceView toView(final Errand envelope, final FinancialAssistanceEntity entity) {
		return ofNullable(envelope)
			.map(env -> FinancialAssistanceView.create()
				.withId(env.getId())
				.withErrandNumber(env.getErrandNumber())
				.withMunicipalityId(env.getMunicipalityId())
				.withNamespace(env.getNamespace())
				.withTypeSlug(env.getTypeSlug())
				.withTitle(env.getTitle())
				.withStatus(env.getStatus())
				.withPriority(env.getPriority())
				.withReporterUserId(env.getReporterUserId())
				.withAssignedUserId(env.getAssignedUserId())
				.withProcessInstanceId(env.getProcessInstanceId())
				.withCreated(env.getCreated())
				.withModified(env.getModified())
				.withTouched(env.getTouched())
				.withData(toData(entity)))
			.orElse(null);
	}

	// ---- Child --------------------------------------------------------------------------------------------------------

	static List<FaChild> toFaChildren(final List<Child> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaChild).toList()).orElse(null);
	}

	static FaChild toFaChild(final Child c) {
		return FaChild.create()
			.withPersonalNumber(c.getPersonalNumber())
			.withFirstName(c.getFirstName())
			.withLastName(c.getLastName())
			.withSchoolName(c.getSchoolName())
			.withResidenceExtent(c.getResidenceExtent())
			.withDaysInHome(c.getDaysInHome());
	}

	static List<Child> toChildren(final List<FaChild> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toChild).toList()).orElse(null);
	}

	static Child toChild(final FaChild e) {
		return Child.create()
			.withPersonalNumber(e.getPersonalNumber())
			.withFirstName(e.getFirstName())
			.withLastName(e.getLastName())
			.withSchoolName(e.getSchoolName())
			.withResidenceExtent(e.getResidenceExtent())
			.withDaysInHome(e.getDaysInHome());
	}

	// ---- Cost ---------------------------------------------------------------------------------------------------------

	static List<FaCost> toFaCosts(final List<Cost> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaCost).toList()).orElse(null);
	}

	static FaCost toFaCost(final Cost c) {
		return FaCost.create()
			.withCostType(c.getCostType())
			.withAppliedAmount(c.getAppliedAmount())
			.withOtherSubType(c.getOtherSubType())
			.withSpecification(c.getSpecification())
			.withRecipientOrPeriod(c.getRecipientOrPeriod());
	}

	static List<Cost> toCosts(final List<FaCost> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toCost).toList()).orElse(null);
	}

	static Cost toCost(final FaCost e) {
		return Cost.create()
			.withCostType(e.getCostType())
			.withAppliedAmount(e.getAppliedAmount())
			.withOtherSubType(e.getOtherSubType())
			.withSpecification(e.getSpecification())
			.withRecipientOrPeriod(e.getRecipientOrPeriod());
	}

	// ---- Income -------------------------------------------------------------------------------------------------------

	static List<FaIncome> toFaIncomes(final List<Income> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaIncome).toList()).orElse(null);
	}

	static FaIncome toFaIncome(final Income c) {
		return FaIncome.create()
			.withIncomeType(c.getIncomeType())
			.withAmount(c.getAmount())
			.withIncomeDate(c.getIncomeDate())
			.withRecipient(c.getRecipient());
	}

	static List<Income> toIncomes(final List<FaIncome> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toIncome).toList()).orElse(null);
	}

	static Income toIncome(final FaIncome e) {
		return Income.create()
			.withIncomeType(e.getIncomeType())
			.withAmount(e.getAmount())
			.withIncomeDate(e.getIncomeDate())
			.withRecipient(e.getRecipient());
	}

	// ---- Pending benefit ----------------------------------------------------------------------------------------------

	static List<FaPendingBenefit> toFaPendingBenefits(final List<PendingBenefit> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaPendingBenefit).toList()).orElse(null);
	}

	static FaPendingBenefit toFaPendingBenefit(final PendingBenefit c) {
		return FaPendingBenefit.create()
			.withBenefitName(c.getBenefitName())
			.withApplicantName(c.getApplicantName());
	}

	static List<PendingBenefit> toPendingBenefits(final List<FaPendingBenefit> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toPendingBenefit).toList()).orElse(null);
	}

	static PendingBenefit toPendingBenefit(final FaPendingBenefit e) {
		return PendingBenefit.create()
			.withBenefitName(e.getBenefitName())
			.withApplicantName(e.getApplicantName());
	}

	// ---- Asset --------------------------------------------------------------------------------------------------------

	static List<FaAsset> toFaAssets(final List<Asset> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaAsset).toList()).orElse(null);
	}

	static FaAsset toFaAsset(final Asset c) {
		return FaAsset.create()
			.withAssetCategory(c.getAssetCategory())
			.withDescription(c.getDescription())
			.withValue(c.getValue())
			.withPropertyType(c.getPropertyType())
			.withPurchaseYear(c.getPurchaseYear())
			.withPurchasePrice(c.getPurchasePrice())
			.withCompanyName(c.getCompanyName())
			.withCompanyAssetSum(c.getCompanyAssetSum())
			.withVehicleType(c.getVehicleType())
			.withRegistrationNumber(c.getRegistrationNumber())
			.withPurchaseDate(c.getPurchaseDate());
	}

	static List<Asset> toAssets(final List<FaAsset> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toAsset).toList()).orElse(null);
	}

	static Asset toAsset(final FaAsset e) {
		return Asset.create()
			.withAssetCategory(e.getAssetCategory())
			.withDescription(e.getDescription())
			.withValue(e.getValue())
			.withPropertyType(e.getPropertyType())
			.withPurchaseYear(e.getPurchaseYear())
			.withPurchasePrice(e.getPurchasePrice())
			.withCompanyName(e.getCompanyName())
			.withCompanyAssetSum(e.getCompanyAssetSum())
			.withVehicleType(e.getVehicleType())
			.withRegistrationNumber(e.getRegistrationNumber())
			.withPurchaseDate(e.getPurchaseDate());
	}

	// ---- Person -------------------------------------------------------------------------------------------------------

	static List<FaPerson> toFaPersons(final List<Person> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaPerson).toList()).orElse(null);
	}

	static FaPerson toFaPerson(final Person c) {
		return FaPerson.create()
			.withRole(c.getRole())
			.withPersonalNumber(c.getPersonalNumber())
			.withNeedsInterpreter(c.getNeedsInterpreter())
			.withInterpreterLanguage(c.getInterpreterLanguage())
			.withHadWorkLast12Months(c.getHadWorkLast12Months())
			.withHadWorkDescription(c.getHadWorkDescription())
			.withPaymentMethod(c.getPaymentMethod())
			.withClearingNumber(c.getClearingNumber())
			.withAccountNumber(c.getAccountNumber())
			.withOtherPaymentDescription(c.getOtherPaymentDescription())
			.withPaymentSameAsPrevious(c.getPaymentSameAsPrevious());
	}

	static List<Person> toPersons(final List<FaPerson> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toPerson).toList()).orElse(null);
	}

	static Person toPerson(final FaPerson e) {
		return Person.create()
			.withRole(e.getRole())
			.withPersonalNumber(e.getPersonalNumber())
			.withNeedsInterpreter(e.getNeedsInterpreter())
			.withInterpreterLanguage(e.getInterpreterLanguage())
			.withHadWorkLast12Months(e.getHadWorkLast12Months())
			.withHadWorkDescription(e.getHadWorkDescription())
			.withPaymentMethod(e.getPaymentMethod())
			.withClearingNumber(e.getClearingNumber())
			.withAccountNumber(e.getAccountNumber())
			.withOtherPaymentDescription(e.getOtherPaymentDescription())
			.withPaymentSameAsPrevious(e.getPaymentSameAsPrevious());
	}

	// ---- Planning -----------------------------------------------------------------------------------------------------

	static List<FaPlanning> toFaPlannings(final List<Planning> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaPlanning).toList()).orElse(null);
	}

	static FaPlanning toFaPlanning(final Planning c) {
		return FaPlanning.create()
			.withPerson(c.getPerson())
			.withPlanningType(c.getPlanningType())
			.withWorkExtent(c.getWorkExtent())
			.withWorkDescription(c.getWorkDescription())
			.withSickLeaveLevel(c.getSickLeaveLevel())
			.withSfiStudyPath(c.getSfiStudyPath())
			.withSfiCourse(c.getSfiCourse())
			.withOtherDescription(c.getOtherDescription());
	}

	static List<Planning> toPlannings(final List<FaPlanning> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toPlanning).toList()).orElse(null);
	}

	static Planning toPlanning(final FaPlanning e) {
		return Planning.create()
			.withPerson(e.getPerson())
			.withPlanningType(e.getPlanningType())
			.withWorkExtent(e.getWorkExtent())
			.withWorkDescription(e.getWorkDescription())
			.withSickLeaveLevel(e.getSickLeaveLevel())
			.withSfiStudyPath(e.getSfiStudyPath())
			.withSfiCourse(e.getSfiCourse())
			.withOtherDescription(e.getOtherDescription());
	}

	// ---- Planned activity ---------------------------------------------------------------------------------------------

	static List<FaPlannedActivity> toFaPlannedActivities(final List<PlannedActivity> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaPlannedActivity).toList()).orElse(null);
	}

	static FaPlannedActivity toFaPlannedActivity(final PlannedActivity c) {
		return FaPlannedActivity.create()
			.withPerson(c.getPerson())
			.withActivity(c.getActivity())
			.withPeriodFrom(c.getPeriodFrom())
			.withPeriodTo(c.getPeriodTo());
	}

	static List<PlannedActivity> toPlannedActivities(final List<FaPlannedActivity> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toPlannedActivity).toList()).orElse(null);
	}

	static PlannedActivity toPlannedActivity(final FaPlannedActivity e) {
		return PlannedActivity.create()
			.withPerson(e.getPerson())
			.withActivity(e.getActivity())
			.withPeriodFrom(e.getPeriodFrom())
			.withPeriodTo(e.getPeriodTo());
	}

	// ---- Job application ----------------------------------------------------------------------------------------------

	static List<FaJobApplication> toFaJobApplications(final List<JobApplication> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toFaJobApplication).toList()).orElse(null);
	}

	static FaJobApplication toFaJobApplication(final JobApplication c) {
		return FaJobApplication.create()
			.withPerson(c.getPerson())
			.withApplicationDate(c.getApplicationDate())
			.withJobTitle(c.getJobTitle())
			.withEmployerAndPlace(c.getEmployerAndPlace());
	}

	static List<JobApplication> toJobApplications(final List<FaJobApplication> source) {
		return ofNullable(source).map(list -> list.stream().map(FinancialAssistanceMapper::toJobApplication).toList()).orElse(null);
	}

	static JobApplication toJobApplication(final FaJobApplication e) {
		return JobApplication.create()
			.withPerson(e.getPerson())
			.withApplicationDate(e.getApplicationDate())
			.withJobTitle(e.getJobTitle())
			.withEmployerAndPlace(e.getEmployerAndPlace());
	}
}
