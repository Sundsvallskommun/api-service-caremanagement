package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
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

	/** How an applicant's partyId is typed when promoted to a core stakeholder externalId. */
	private static final String EXTERNAL_ID_TYPE_PRIVATE = "PRIVATE";
	private static final String CONTACT_CHANNEL_EMAIL = "EMAIL";
	private static final String CONTACT_CHANNEL_PHONE = "PHONE";

	private FinancialAssistanceMapper() {}

	private static <S, T> List<T> mapList(final List<S> source, final Function<S, T> mapper) {
		return ofNullable(source)
			.map(list -> list.stream()
				.filter(Objects::nonNull)
				.map(mapper)
				.filter(Objects::nonNull)
				.toList())
			.orElse(null);
	}

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
				.withChildren(mapList(d.getChildren(), FinancialAssistanceMapper::toFaChild))
				.withCosts(mapList(d.getCosts(), FinancialAssistanceMapper::toFaCost))
				.withIncomes(mapList(d.getIncomes(), FinancialAssistanceMapper::toFaIncome))
				.withPendingBenefits(mapList(d.getPendingBenefits(), FinancialAssistanceMapper::toFaPendingBenefit))
				.withAssets(mapList(d.getAssets(), FinancialAssistanceMapper::toFaAsset))
				.withPersons(mapList(d.getPersons(), FinancialAssistanceMapper::toFaPerson))
				.withPlannings(mapList(d.getPlannings(), FinancialAssistanceMapper::toFaPlanning))
				.withPlannedActivities(mapList(d.getPlannedActivities(), FinancialAssistanceMapper::toFaPlannedActivity))
				.withJobApplications(mapList(d.getJobApplications(), FinancialAssistanceMapper::toFaJobApplication)))
			.orElse(null);
	}

	/**
	 * Applies non-null fields from {@code source} onto {@code entity} (PATCH semantics — null fields on the source leave
	 * the existing value untouched). The server-owned fields are never written from client data: {@code errandId},
	 * {@code applicationType} (derived from the type slug), {@code lastDailyRunAt}, {@code created} and {@code modified}.
	 */
	public static FinancialAssistanceEntity updateEntity(final FinancialAssistanceEntity entity, final FinancialAssistanceData source) {
		if (entity == null || source == null) {
			return entity;
		}
		ofNullable(source.getMaritalStatus()).ifPresent(entity::setMaritalStatus);
		ofNullable(source.getPeriodMonth()).ifPresent(entity::setPeriodMonth);
		ofNullable(source.getPeriodYear()).ifPresent(entity::setPeriodYear);
		ofNullable(source.getPeriodChoice()).ifPresent(entity::setPeriodChoice);
		ofNullable(source.getNormType()).ifPresent(value -> entity.setNormType(new ArrayList<>(value)));
		ofNullable(source.getOtherBenefitDescription()).ifPresent(entity::setOtherBenefitDescription);
		ofNullable(source.getLivelihoodDescription()).ifPresent(entity::setLivelihoodDescription);
		ofNullable(source.getHasChildrenUnder21()).ifPresent(entity::setHasChildrenUnder21);
		ofNullable(source.getChildrenResidenceChanged()).ifPresent(entity::setChildrenResidenceChanged);
		ofNullable(source.getChildrenResidenceChangeDescription()).ifPresent(entity::setChildrenResidenceChangeDescription);
		ofNullable(source.getHousingForm()).ifPresent(entity::setHousingForm);
		ofNullable(source.getHousingPersonCount()).ifPresent(entity::setHousingPersonCount);
		ofNullable(source.getHousingRoomsPlusKitchen()).ifPresent(entity::setHousingRoomsPlusKitchen);
		ofNullable(source.getHousingDescription()).ifPresent(entity::setHousingDescription);
		ofNullable(source.getHousingChanged()).ifPresent(entity::setHousingChanged);
		ofNullable(source.getHousingChangeDescription()).ifPresent(entity::setHousingChangeDescription);
		ofNullable(source.getHasIncomes()).ifPresent(entity::setHasIncomes);
		ofNullable(source.getHasPendingBenefits()).ifPresent(entity::setHasPendingBenefits);
		ofNullable(source.getHasAssets()).ifPresent(entity::setHasAssets);
		ofNullable(source.getStaysInMunicipality()).ifPresent(entity::setStaysInMunicipality);
		ofNullable(source.getStayDescription()).ifPresent(entity::setStayDescription);
		ofNullable(source.getAttestation()).ifPresent(entity::setAttestation);
		ofNullable(source.getAttestedAt()).ifPresent(entity::setAttestedAt);
		ofNullable(mapList(source.getChildren(), FinancialAssistanceMapper::toFaChild)).ifPresent(value -> entity.setChildren(new ArrayList<>(value)));
		ofNullable(mapList(source.getCosts(), FinancialAssistanceMapper::toFaCost)).ifPresent(value -> entity.setCosts(new ArrayList<>(value)));
		ofNullable(mapList(source.getIncomes(), FinancialAssistanceMapper::toFaIncome)).ifPresent(value -> entity.setIncomes(new ArrayList<>(value)));
		ofNullable(mapList(source.getPendingBenefits(), FinancialAssistanceMapper::toFaPendingBenefit)).ifPresent(value -> entity.setPendingBenefits(new ArrayList<>(value)));
		ofNullable(mapList(source.getAssets(), FinancialAssistanceMapper::toFaAsset)).ifPresent(value -> entity.setAssets(new ArrayList<>(value)));
		ofNullable(mapList(source.getPersons(), FinancialAssistanceMapper::toFaPerson)).ifPresent(value -> entity.setPersons(new ArrayList<>(value)));
		ofNullable(mapList(source.getPlannings(), FinancialAssistanceMapper::toFaPlanning)).ifPresent(value -> entity.setPlannings(new ArrayList<>(value)));
		ofNullable(mapList(source.getPlannedActivities(), FinancialAssistanceMapper::toFaPlannedActivity)).ifPresent(value -> entity.setPlannedActivities(new ArrayList<>(value)));
		ofNullable(mapList(source.getJobApplications(), FinancialAssistanceMapper::toFaJobApplication)).ifPresent(value -> entity.setJobApplications(new ArrayList<>(value)));
		return entity;
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
				.withChildren(mapList(e.getChildren(), FinancialAssistanceMapper::toChild))
				.withCosts(mapList(e.getCosts(), FinancialAssistanceMapper::toCost))
				.withIncomes(mapList(e.getIncomes(), FinancialAssistanceMapper::toIncome))
				.withPendingBenefits(mapList(e.getPendingBenefits(), FinancialAssistanceMapper::toPendingBenefit))
				.withAssets(mapList(e.getAssets(), FinancialAssistanceMapper::toAsset))
				.withPersons(mapList(e.getPersons(), FinancialAssistanceMapper::toPerson))
				.withPlannings(mapList(e.getPlannings(), FinancialAssistanceMapper::toPlanning))
				.withPlannedActivities(mapList(e.getPlannedActivities(), FinancialAssistanceMapper::toPlannedActivity))
				.withJobApplications(mapList(e.getJobApplications(), FinancialAssistanceMapper::toJobApplication)))
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
				.withLastDailyRunAt(ofNullable(entity).map(FinancialAssistanceEntity::getLastDailyRunAt).orElse(null))
				.withData(toData(entity)))
			.orElse(null);
	}

	// ---- Child --------------------------------------------------------------------------------------------------------

	static FaChild toFaChild(final Child child) {
		return ofNullable(child)
			.map(source -> FaChild.create()
				.withPartyId(source.getPartyId())
				.withFirstName(source.getFirstName())
				.withLastName(source.getLastName())
				.withSchoolName(source.getSchoolName())
				.withResidenceExtent(source.getResidenceExtent())
				.withDaysInHome(source.getDaysInHome()))
			.orElse(null);
	}

	static Child toChild(final FaChild entity) {
		return ofNullable(entity)
			.map(source -> Child.create()
				.withPartyId(source.getPartyId())
				.withFirstName(source.getFirstName())
				.withLastName(source.getLastName())
				.withSchoolName(source.getSchoolName())
				.withResidenceExtent(source.getResidenceExtent())
				.withDaysInHome(source.getDaysInHome()))
			.orElse(null);
	}

	// ---- Cost ---------------------------------------------------------------------------------------------------------

	static FaCost toFaCost(final Cost cost) {
		return ofNullable(cost)
			.map(source -> FaCost.create()
				.withCostType(source.getCostType())
				.withAppliedAmount(source.getAppliedAmount())
				.withOtherSubType(source.getOtherSubType())
				.withSpecification(source.getSpecification())
				.withRecipientOrPeriod(source.getRecipientOrPeriod()))
			.orElse(null);
	}

	static Cost toCost(final FaCost entity) {
		return ofNullable(entity)
			.map(source -> Cost.create()
				.withCostType(source.getCostType())
				.withAppliedAmount(source.getAppliedAmount())
				.withOtherSubType(source.getOtherSubType())
				.withSpecification(source.getSpecification())
				.withRecipientOrPeriod(source.getRecipientOrPeriod()))
			.orElse(null);
	}

	// ---- Income -------------------------------------------------------------------------------------------------------

	static FaIncome toFaIncome(final Income income) {
		return ofNullable(income)
			.map(source -> FaIncome.create()
				.withIncomeType(source.getIncomeType())
				.withAmount(source.getAmount())
				.withIncomeDate(source.getIncomeDate())
				.withRecipient(source.getRecipient()))
			.orElse(null);
	}

	static Income toIncome(final FaIncome entity) {
		return ofNullable(entity)
			.map(source -> Income.create()
				.withIncomeType(source.getIncomeType())
				.withAmount(source.getAmount())
				.withIncomeDate(source.getIncomeDate())
				.withRecipient(source.getRecipient()))
			.orElse(null);
	}

	// ---- Pending benefit ----------------------------------------------------------------------------------------------

	static FaPendingBenefit toFaPendingBenefit(final PendingBenefit pendingBenefit) {
		return ofNullable(pendingBenefit)
			.map(source -> FaPendingBenefit.create()
				.withBenefitName(source.getBenefitName())
				.withApplicantName(source.getApplicantName()))
			.orElse(null);
	}

	static PendingBenefit toPendingBenefit(final FaPendingBenefit entity) {
		return ofNullable(entity)
			.map(source -> PendingBenefit.create()
				.withBenefitName(source.getBenefitName())
				.withApplicantName(source.getApplicantName()))
			.orElse(null);
	}

	// ---- Asset --------------------------------------------------------------------------------------------------------

	static FaAsset toFaAsset(final Asset asset) {
		return ofNullable(asset)
			.map(source -> FaAsset.create()
				.withAssetCategory(source.getAssetCategory())
				.withDescription(source.getDescription())
				.withValue(source.getValue())
				.withPropertyType(source.getPropertyType())
				.withPurchaseYear(source.getPurchaseYear())
				.withPurchasePrice(source.getPurchasePrice())
				.withCompanyName(source.getCompanyName())
				.withCompanyAssetSum(source.getCompanyAssetSum())
				.withVehicleType(source.getVehicleType())
				.withRegistrationNumber(source.getRegistrationNumber())
				.withPurchaseDate(source.getPurchaseDate()))
			.orElse(null);
	}

	static Asset toAsset(final FaAsset entity) {
		return ofNullable(entity)
			.map(source -> Asset.create()
				.withAssetCategory(source.getAssetCategory())
				.withDescription(source.getDescription())
				.withValue(source.getValue())
				.withPropertyType(source.getPropertyType())
				.withPurchaseYear(source.getPurchaseYear())
				.withPurchasePrice(source.getPurchasePrice())
				.withCompanyName(source.getCompanyName())
				.withCompanyAssetSum(source.getCompanyAssetSum())
				.withVehicleType(source.getVehicleType())
				.withRegistrationNumber(source.getRegistrationNumber())
				.withPurchaseDate(source.getPurchaseDate()))
			.orElse(null);
	}

	// ---- Person -------------------------------------------------------------------------------------------------------

	static FaPerson toFaPerson(final Person person) {
		return ofNullable(person)
			.map(source -> FaPerson.create()
				.withRole(source.getRole())
				.withPartyId(source.getPartyId())
				.withNeedsInterpreter(source.getNeedsInterpreter())
				.withInterpreterLanguage(source.getInterpreterLanguage())
				.withHadWorkLast12Months(source.getHadWorkLast12Months())
				.withHadWorkDescription(source.getHadWorkDescription())
				.withPaymentMethod(source.getPaymentMethod())
				.withClearingNumber(source.getClearingNumber())
				.withAccountNumber(source.getAccountNumber())
				.withOtherPaymentDescription(source.getOtherPaymentDescription())
				.withPaymentSameAsPrevious(source.getPaymentSameAsPrevious())
				.withEmail(source.getEmail())
				.withPhone(source.getPhone())
				.withNotifyByEmail(source.getNotifyByEmail())
				.withNotifyBySms(source.getNotifyBySms()))
			.orElse(null);
	}

	static Person toPerson(final FaPerson entity) {
		return ofNullable(entity)
			.map(source -> Person.create()
				.withRole(source.getRole())
				.withPartyId(source.getPartyId())
				.withNeedsInterpreter(source.getNeedsInterpreter())
				.withInterpreterLanguage(source.getInterpreterLanguage())
				.withHadWorkLast12Months(source.getHadWorkLast12Months())
				.withHadWorkDescription(source.getHadWorkDescription())
				.withPaymentMethod(source.getPaymentMethod())
				.withClearingNumber(source.getClearingNumber())
				.withAccountNumber(source.getAccountNumber())
				.withOtherPaymentDescription(source.getOtherPaymentDescription())
				.withPaymentSameAsPrevious(source.getPaymentSameAsPrevious())
				.withEmail(source.getEmail())
				.withPhone(source.getPhone())
				.withNotifyByEmail(source.getNotifyByEmail())
				.withNotifyBySms(source.getNotifyBySms()))
			.orElse(null);
	}

	// ---- Person → core Stakeholder ------------------------------------------------------------------------------------

	/**
	 * Promote the application's persons to core {@link Stakeholder} rows so the errand carries its applicant/co-applicant
	 * in
	 * the shared stakeholders collection. Each {@link Person} with a role maps to a stakeholder holding that role, the
	 * partyId as a {@code PRIVATE} externalId, and the supplied email/phone as contact channels. Identity (name, address)
	 * is deliberately not denormalised here — it is resolved from the partyId on demand and is subject to
	 * protected-identity handling. Persons without a role are skipped; a null list yields an empty list.
	 */
	public static List<Stakeholder> toStakeholders(final List<Person> source) {
		return ofNullable(source).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(person -> StringUtils.hasText(person.getRole()))
			.map(FinancialAssistanceMapper::toStakeholder)
			.toList();
	}

	static Stakeholder toStakeholder(final Person person) {
		return ofNullable(person)
			.map(source -> Stakeholder.create()
				.withRole(source.getRole())
				.withExternalId(source.getPartyId())
				.withExternalIdType(ofNullable(source.getPartyId()).filter(StringUtils::hasText).map(_ -> EXTERNAL_ID_TYPE_PRIVATE).orElse(null))
				.withContactChannels(toContactChannels(source)))
			.orElse(null);
	}

	private static List<ContactChannel> toContactChannels(final Person person) {
		final var channels = new ArrayList<ContactChannel>();
		ofNullable(person.getEmail()).filter(StringUtils::hasText)
			.ifPresent(email -> channels.add(ContactChannel.create().withKey(CONTACT_CHANNEL_EMAIL).withValue(email)));
		ofNullable(person.getPhone()).filter(StringUtils::hasText)
			.ifPresent(phone -> channels.add(ContactChannel.create().withKey(CONTACT_CHANNEL_PHONE).withValue(phone)));
		return channels;
	}

	// ---- Planning -----------------------------------------------------------------------------------------------------

	static FaPlanning toFaPlanning(final Planning planning) {
		return ofNullable(planning)
			.map(source -> FaPlanning.create()
				.withPerson(source.getPerson())
				.withPlanningType(source.getPlanningType())
				.withWorkExtent(source.getWorkExtent())
				.withWorkDescription(source.getWorkDescription())
				.withSickLeaveLevel(source.getSickLeaveLevel())
				.withSfiStudyPath(source.getSfiStudyPath())
				.withSfiCourse(source.getSfiCourse())
				.withOtherDescription(source.getOtherDescription()))
			.orElse(null);
	}

	static Planning toPlanning(final FaPlanning entity) {
		return ofNullable(entity)
			.map(source -> Planning.create()
				.withPerson(source.getPerson())
				.withPlanningType(source.getPlanningType())
				.withWorkExtent(source.getWorkExtent())
				.withWorkDescription(source.getWorkDescription())
				.withSickLeaveLevel(source.getSickLeaveLevel())
				.withSfiStudyPath(source.getSfiStudyPath())
				.withSfiCourse(source.getSfiCourse())
				.withOtherDescription(source.getOtherDescription()))
			.orElse(null);
	}

	// ---- Planned activity ---------------------------------------------------------------------------------------------

	static FaPlannedActivity toFaPlannedActivity(final PlannedActivity plannedActivity) {
		return ofNullable(plannedActivity)
			.map(source -> FaPlannedActivity.create()
				.withPerson(source.getPerson())
				.withActivity(source.getActivity())
				.withPeriodFrom(source.getPeriodFrom())
				.withPeriodTo(source.getPeriodTo()))
			.orElse(null);
	}

	static PlannedActivity toPlannedActivity(final FaPlannedActivity entity) {
		return ofNullable(entity)
			.map(source -> PlannedActivity.create()
				.withPerson(source.getPerson())
				.withActivity(source.getActivity())
				.withPeriodFrom(source.getPeriodFrom())
				.withPeriodTo(source.getPeriodTo()))
			.orElse(null);
	}

	// ---- Job application ----------------------------------------------------------------------------------------------

	static FaJobApplication toFaJobApplication(final JobApplication jobApplication) {
		return ofNullable(jobApplication)
			.map(source -> FaJobApplication.create()
				.withPerson(source.getPerson())
				.withApplicationDate(source.getApplicationDate())
				.withJobTitle(source.getJobTitle())
				.withEmployerAndPlace(source.getEmployerAndPlace()))
			.orElse(null);
	}

	static JobApplication toJobApplication(final FaJobApplication entity) {
		return ofNullable(entity)
			.map(source -> JobApplication.create()
				.withPerson(source.getPerson())
				.withApplicationDate(source.getApplicationDate())
				.withJobTitle(source.getJobTitle())
				.withEmployerAndPlace(source.getEmployerAndPlace()))
			.orElse(null);
	}
}
