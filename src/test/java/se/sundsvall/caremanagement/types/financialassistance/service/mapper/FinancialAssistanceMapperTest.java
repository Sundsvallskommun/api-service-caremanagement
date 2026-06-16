package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Asset;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Child;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Cost;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
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

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAssistanceMapperTest {

	private static final OffsetDateTime ATTESTED_AT = OffsetDateTime.parse("2026-06-01T09:30:00Z");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T11:00:00Z");
	private static final OffsetDateTime TOUCHED = OffsetDateTime.parse("2026-06-05T12:00:00Z");
	private static final LocalDate DATE = LocalDate.parse("2026-05-15");

	@Test
	void toEntityMapsEverything() {
		final var data = fullData();

		final var entity = FinancialAssistanceMapper.toEntity(data, "errand-1");

		assertThat(entity).isNotNull();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getApplicationType()).isEqualTo("NEW");
		assertThat(entity.getMaritalStatus()).isEqualTo("SINGLE");
		assertThat(entity.getPeriodMonth()).isEqualTo(6);
		assertThat(entity.getPeriodYear()).isEqualTo(2026);
		assertThat(entity.getPeriodChoice()).isEqualTo("CURRENT_MONTH");
		assertThat(entity.getNormType()).isEqualTo("RIKSNORM");
		assertThat(entity.getOtherBenefitDescription()).isEqualTo("Etableringsersättning");
		assertThat(entity.getLivelihoodDescription()).isEqualTo("Söker arbete");
		assertThat(entity.getHasChildrenUnder21()).isTrue();
		assertThat(entity.getChildrenResidenceChanged()).isFalse();
		assertThat(entity.getChildrenResidenceChangeDescription()).isEqualTo("Bor växelvis");
		assertThat(entity.getHousingForm()).isEqualTo("RENTAL");
		assertThat(entity.getHousingPersonCount()).isEqualTo(3);
		assertThat(entity.getHousingRoomsPlusKitchen()).isEqualTo(3);
		assertThat(entity.getHousingDescription()).isEqualTo("Trerumslägenhet");
		assertThat(entity.getHousingChanged()).isFalse();
		assertThat(entity.getHousingChangeDescription()).isEqualTo("Flyttade i maj");
		assertThat(entity.getHasIncomes()).isTrue();
		assertThat(entity.getHasPendingBenefits()).isTrue();
		assertThat(entity.getHasAssets()).isTrue();
		assertThat(entity.getStaysInMunicipality()).isTrue();
		assertThat(entity.getStayDescription()).isEqualTo("Bor på folkbokföringsadressen");
		assertThat(entity.getAttestation()).isTrue();
		assertThat(entity.getAttestedAt()).isEqualTo(ATTESTED_AT);

		assertThat(entity.getChildren()).hasSize(1);
		assertThat(entity.getChildren().getFirst().getPartyId()).isEqualTo("20180101-1234");
		assertThat(entity.getChildren().getFirst().getFirstName()).isEqualTo("Kid");
		assertThat(entity.getChildren().getFirst().getLastName()).isEqualTo("Karlsson");
		assertThat(entity.getChildren().getFirst().getSchoolName()).isEqualTo("Skolan");
		assertThat(entity.getChildren().getFirst().getResidenceExtent()).isEqualTo("FULL");
		assertThat(entity.getChildren().getFirst().getDaysInHome()).isEqualTo(30);

		assertThat(entity.getCosts()).hasSize(1);
		assertThat(entity.getCosts().getFirst().getCostType()).isEqualTo("RENT");
		assertThat(entity.getCosts().getFirst().getAppliedAmount()).isEqualByComparingTo("5000.00");
		assertThat(entity.getCosts().getFirst().getOtherSubType()).isEqualTo("OTHER_SUB");
		assertThat(entity.getCosts().getFirst().getSpecification()).isEqualTo("Spec");
		assertThat(entity.getCosts().getFirst().getRecipientOrPeriod()).isEqualTo("Juni");

		assertThat(entity.getIncomes()).hasSize(1);
		assertThat(entity.getIncomes().getFirst().getIncomeType()).isEqualTo("SALARY");
		assertThat(entity.getIncomes().getFirst().getAmount()).isEqualByComparingTo("12000.00");
		assertThat(entity.getIncomes().getFirst().getIncomeDate()).isEqualTo(DATE);
		assertThat(entity.getIncomes().getFirst().getRecipient()).isEqualTo("Anna");

		assertThat(entity.getPendingBenefits()).hasSize(1);
		assertThat(entity.getPendingBenefits().getFirst().getBenefitName()).isEqualTo("BOSTADSBIDRAG");
		assertThat(entity.getPendingBenefits().getFirst().getApplicantName()).isEqualTo("Anna");

		assertThat(entity.getAssets()).hasSize(1);
		assertThat(entity.getAssets().getFirst().getAssetCategory()).isEqualTo("VEHICLE");
		assertThat(entity.getAssets().getFirst().getDescription()).isEqualTo("Bil");
		assertThat(entity.getAssets().getFirst().getValue()).isEqualByComparingTo("80000.00");
		assertThat(entity.getAssets().getFirst().getPropertyType()).isEqualTo("PROP");
		assertThat(entity.getAssets().getFirst().getPurchaseYear()).isEqualTo(2020);
		assertThat(entity.getAssets().getFirst().getPurchasePrice()).isEqualByComparingTo("120000.00");
		assertThat(entity.getAssets().getFirst().getCompanyName()).isEqualTo("Bolaget AB");
		assertThat(entity.getAssets().getFirst().getCompanyAssetSum()).isEqualByComparingTo("50000.00");
		assertThat(entity.getAssets().getFirst().getVehicleType()).isEqualTo("CAR");
		assertThat(entity.getAssets().getFirst().getRegistrationNumber()).isEqualTo("ABC123");
		assertThat(entity.getAssets().getFirst().getPurchaseDate()).isEqualTo(DATE);

		assertThat(entity.getPersons()).hasSize(1);
		assertThat(entity.getPersons().getFirst().getRole()).isEqualTo("APPLICANT");
		assertThat(entity.getPersons().getFirst().getPartyId()).isEqualTo("19900101-1234");
		assertThat(entity.getPersons().getFirst().getNeedsInterpreter()).isTrue();
		assertThat(entity.getPersons().getFirst().getInterpreterLanguage()).isEqualTo("Arabiska");
		assertThat(entity.getPersons().getFirst().getHadWorkLast12Months()).isFalse();
		assertThat(entity.getPersons().getFirst().getHadWorkDescription()).isEqualTo("Inget arbete");
		assertThat(entity.getPersons().getFirst().getPaymentMethod()).isEqualTo("BANK");
		assertThat(entity.getPersons().getFirst().getClearingNumber()).isEqualTo("1234");
		assertThat(entity.getPersons().getFirst().getAccountNumber()).isEqualTo("567890");
		assertThat(entity.getPersons().getFirst().getOtherPaymentDescription()).isEqualTo("Annat");
		assertThat(entity.getPersons().getFirst().getPaymentSameAsPrevious()).isTrue();
		assertThat(entity.getPersons().getFirst().getEmail()).isEqualTo("anna@example.com");
		assertThat(entity.getPersons().getFirst().getPhone()).isEqualTo("+46701234567");
		assertThat(entity.getPersons().getFirst().getNotifyByEmail()).isTrue();
		assertThat(entity.getPersons().getFirst().getNotifyBySms()).isFalse();

		assertThat(entity.getPlannings()).hasSize(1);
		assertThat(entity.getPlannings().getFirst().getPerson()).isEqualTo("Anna");
		assertThat(entity.getPlannings().getFirst().getPlanningType()).isEqualTo("WORK");
		assertThat(entity.getPlannings().getFirst().getWorkExtent()).isEqualTo("100");
		assertThat(entity.getPlannings().getFirst().getWorkDescription()).isEqualTo("Arbete");
		assertThat(entity.getPlannings().getFirst().getSickLeaveLevel()).isEqualTo("50");
		assertThat(entity.getPlannings().getFirst().getSfiStudyPath()).isEqualTo("PATH_1");
		assertThat(entity.getPlannings().getFirst().getSfiCourse()).isEqualTo("KURS_A");
		assertThat(entity.getPlannings().getFirst().getOtherDescription()).isEqualTo("Övrigt");

		assertThat(entity.getPlannedActivities()).hasSize(1);
		assertThat(entity.getPlannedActivities().getFirst().getPerson()).isEqualTo("Anna");
		assertThat(entity.getPlannedActivities().getFirst().getActivity()).isEqualTo("Jobbsökning");
		assertThat(entity.getPlannedActivities().getFirst().getPeriodFrom()).isEqualTo(DATE);
		assertThat(entity.getPlannedActivities().getFirst().getPeriodTo()).isEqualTo(DATE.plusMonths(1));

		assertThat(entity.getJobApplications()).hasSize(1);
		assertThat(entity.getJobApplications().getFirst().getPerson()).isEqualTo("Anna");
		assertThat(entity.getJobApplications().getFirst().getApplicationDate()).isEqualTo(DATE);
		assertThat(entity.getJobApplications().getFirst().getJobTitle()).isEqualTo("Snickare");
		assertThat(entity.getJobApplications().getFirst().getEmployerAndPlace()).isEqualTo("Bygg AB, Sundsvall");
	}

	@Test
	void toDataMapsEverything() {
		final var entity = fullEntity();

		final var data = FinancialAssistanceMapper.toData(entity);

		assertThat(data).isNotNull();
		assertThat(data.getApplicationType()).isEqualTo("RENEWAL");
		assertThat(data.getMaritalStatus()).isEqualTo("COHABITING");
		assertThat(data.getPeriodMonth()).isEqualTo(7);
		assertThat(data.getPeriodYear()).isEqualTo(2025);
		assertThat(data.getPeriodChoice()).isEqualTo("NEXT_MONTH");
		assertThat(data.getNormType()).isEqualTo("OTHER_NORM");
		assertThat(data.getOtherBenefitDescription()).isEqualTo("Annat bidrag");
		assertThat(data.getLivelihoodDescription()).isEqualTo("Egen försörjning");
		assertThat(data.getHasChildrenUnder21()).isFalse();
		assertThat(data.getChildrenResidenceChanged()).isTrue();
		assertThat(data.getChildrenResidenceChangeDescription()).isEqualTo("Ändrad");
		assertThat(data.getHousingForm()).isEqualTo("CONDOMINIUM");
		assertThat(data.getHousingPersonCount()).isEqualTo(3);
		assertThat(data.getHousingRoomsPlusKitchen()).isEqualTo(2);
		assertThat(data.getHousingDescription()).isEqualTo("Tvåa");
		assertThat(data.getHousingChanged()).isTrue();
		assertThat(data.getHousingChangeDescription()).isEqualTo("Bytt bostad");
		assertThat(data.getHasIncomes()).isFalse();
		assertThat(data.getHasPendingBenefits()).isFalse();
		assertThat(data.getHasAssets()).isFalse();
		assertThat(data.getStaysInMunicipality()).isFalse();
		assertThat(data.getStayDescription()).isEqualTo("Bor utomlands");
		assertThat(data.getAttestation()).isFalse();
		assertThat(data.getAttestedAt()).isEqualTo(ATTESTED_AT);

		assertThat(data.getChildren()).hasSize(1);
		assertThat(data.getChildren().getFirst().getFirstName()).isEqualTo("Kid");
		assertThat(data.getCosts()).hasSize(1);
		assertThat(data.getCosts().getFirst().getAppliedAmount()).isEqualByComparingTo("5000.00");
		assertThat(data.getIncomes()).hasSize(1);
		assertThat(data.getIncomes().getFirst().getIncomeDate()).isEqualTo(DATE);
		assertThat(data.getPendingBenefits()).hasSize(1);
		assertThat(data.getPendingBenefits().getFirst().getBenefitName()).isEqualTo("BOSTADSBIDRAG");
		assertThat(data.getAssets()).hasSize(1);
		assertThat(data.getAssets().getFirst().getValue()).isEqualByComparingTo("80000.00");
		assertThat(data.getPersons()).hasSize(1);
		assertThat(data.getPersons().getFirst().getRole()).isEqualTo("APPLICANT");
		assertThat(data.getPersons().getFirst().getEmail()).isEqualTo("anna@example.com");
		assertThat(data.getPersons().getFirst().getPhone()).isEqualTo("+46701234567");
		assertThat(data.getPersons().getFirst().getNotifyByEmail()).isTrue();
		assertThat(data.getPersons().getFirst().getNotifyBySms()).isFalse();
		assertThat(data.getPlannings()).hasSize(1);
		assertThat(data.getPlannings().getFirst().getPlanningType()).isEqualTo("WORK");
		assertThat(data.getPlannedActivities()).hasSize(1);
		assertThat(data.getPlannedActivities().getFirst().getActivity()).isEqualTo("Jobbsökning");
		assertThat(data.getJobApplications()).hasSize(1);
		assertThat(data.getJobApplications().getFirst().getJobTitle()).isEqualTo("Snickare");
	}

	@Test
	void toViewAssemblesEnvelopeAndData() {
		final var envelope = Errand.create()
			.withId("errand-1")
			.withErrandNumber("EB-2026-00042")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withTypeSlug("financial-assistance")
			.withTitle("Ansökan om ekonomiskt bistånd")
			.withStatus("INKOMMEN")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withProcessInstanceId("process-1")
			.withCreated(CREATED)
			.withModified(MODIFIED)
			.withTouched(TOUCHED);
		final var entity = fullEntity();

		final var view = FinancialAssistanceMapper.toView(envelope, entity);

		assertThat(view).isNotNull();
		assertThat(view.getId()).isEqualTo("errand-1");
		assertThat(view.getErrandNumber()).isEqualTo("EB-2026-00042");
		assertThat(view.getMunicipalityId()).isEqualTo("2281");
		assertThat(view.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(view.getTypeSlug()).isEqualTo("financial-assistance");
		assertThat(view.getTitle()).isEqualTo("Ansökan om ekonomiskt bistånd");
		assertThat(view.getStatus()).isEqualTo("INKOMMEN");
		assertThat(view.getPriority()).isEqualTo("HIGH");
		assertThat(view.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(view.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(view.getProcessInstanceId()).isEqualTo("process-1");
		assertThat(view.getCreated()).isEqualTo(CREATED);
		assertThat(view.getModified()).isEqualTo(MODIFIED);
		assertThat(view.getTouched()).isEqualTo(TOUCHED);
		assertThat(view.getData()).isNotNull();
		assertThat(view.getData().getApplicationType()).isEqualTo("RENEWAL");
	}

	@Test
	void nullSafe() {
		assertThat(FinancialAssistanceMapper.toEntity(null, "e")).isNull();
		assertThat(FinancialAssistanceMapper.toData(null)).isNull();
		assertThat(FinancialAssistanceMapper.toView(null, null)).isNull();

		final var view = FinancialAssistanceMapper.toView(Errand.create().withId("errand-1"), null);
		assertThat(view).isNotNull();
		assertThat(view.getId()).isEqualTo("errand-1");
		assertThat(view.getData()).isNull();
	}

	private static FinancialAssistanceData fullData() {
		return FinancialAssistanceData.create()
			.withApplicationType("NEW")
			.withMaritalStatus("SINGLE")
			.withPeriodMonth(6)
			.withPeriodYear(2026)
			.withPeriodChoice("CURRENT_MONTH")
			.withNormType("RIKSNORM")
			.withOtherBenefitDescription("Etableringsersättning")
			.withLivelihoodDescription("Söker arbete")
			.withHasChildrenUnder21(true)
			.withChildrenResidenceChanged(false)
			.withChildrenResidenceChangeDescription("Bor växelvis")
			.withHousingForm("RENTAL")
			.withHousingPersonCount(3)
			.withHousingRoomsPlusKitchen(3)
			.withHousingDescription("Trerumslägenhet")
			.withHousingChanged(false)
			.withHousingChangeDescription("Flyttade i maj")
			.withHasIncomes(true)
			.withHasPendingBenefits(true)
			.withHasAssets(true)
			.withStaysInMunicipality(true)
			.withStayDescription("Bor på folkbokföringsadressen")
			.withAttestation(true)
			.withAttestedAt(ATTESTED_AT)
			.withChildren(List.of(Child.create()
				.withPartyId("20180101-1234")
				.withFirstName("Kid")
				.withLastName("Karlsson")
				.withSchoolName("Skolan")
				.withResidenceExtent("FULL")
				.withDaysInHome(30)))
			.withCosts(List.of(Cost.create()
				.withCostType("RENT")
				.withAppliedAmount(new BigDecimal("5000.00"))
				.withOtherSubType("OTHER_SUB")
				.withSpecification("Spec")
				.withRecipientOrPeriod("Juni")))
			.withIncomes(List.of(Income.create()
				.withIncomeType("SALARY")
				.withAmount(new BigDecimal("12000.00"))
				.withIncomeDate(DATE)
				.withRecipient("Anna")))
			.withPendingBenefits(List.of(PendingBenefit.create()
				.withBenefitName("BOSTADSBIDRAG")
				.withApplicantName("Anna")))
			.withAssets(List.of(Asset.create()
				.withAssetCategory("VEHICLE")
				.withDescription("Bil")
				.withValue(new BigDecimal("80000.00"))
				.withPropertyType("PROP")
				.withPurchaseYear(2020)
				.withPurchasePrice(new BigDecimal("120000.00"))
				.withCompanyName("Bolaget AB")
				.withCompanyAssetSum(new BigDecimal("50000.00"))
				.withVehicleType("CAR")
				.withRegistrationNumber("ABC123")
				.withPurchaseDate(DATE)))
			.withPersons(List.of(Person.create()
				.withRole("APPLICANT")
				.withPartyId("19900101-1234")
				.withNeedsInterpreter(true)
				.withInterpreterLanguage("Arabiska")
				.withHadWorkLast12Months(false)
				.withHadWorkDescription("Inget arbete")
				.withPaymentMethod("BANK")
				.withClearingNumber("1234")
				.withAccountNumber("567890")
				.withOtherPaymentDescription("Annat")
				.withPaymentSameAsPrevious(true)
				.withEmail("anna@example.com")
				.withPhone("+46701234567")
				.withNotifyByEmail(true)
				.withNotifyBySms(false)))
			.withPlannings(List.of(Planning.create()
				.withPerson("Anna")
				.withPlanningType("WORK")
				.withWorkExtent("100")
				.withWorkDescription("Arbete")
				.withSickLeaveLevel("50")
				.withSfiStudyPath("PATH_1")
				.withSfiCourse("KURS_A")
				.withOtherDescription("Övrigt")))
			.withPlannedActivities(List.of(PlannedActivity.create()
				.withPerson("Anna")
				.withActivity("Jobbsökning")
				.withPeriodFrom(DATE)
				.withPeriodTo(DATE.plusMonths(1))))
			.withJobApplications(List.of(JobApplication.create()
				.withPerson("Anna")
				.withApplicationDate(DATE)
				.withJobTitle("Snickare")
				.withEmployerAndPlace("Bygg AB, Sundsvall")));
	}

	private static FinancialAssistanceEntity fullEntity() {
		return FinancialAssistanceEntity.create()
			.withErrandId("errand-1")
			.withApplicationType("RENEWAL")
			.withMaritalStatus("COHABITING")
			.withPeriodMonth(7)
			.withPeriodYear(2025)
			.withPeriodChoice("NEXT_MONTH")
			.withNormType("OTHER_NORM")
			.withOtherBenefitDescription("Annat bidrag")
			.withLivelihoodDescription("Egen försörjning")
			.withHasChildrenUnder21(false)
			.withChildrenResidenceChanged(true)
			.withChildrenResidenceChangeDescription("Ändrad")
			.withHousingForm("CONDOMINIUM")
			.withHousingPersonCount(3)
			.withHousingRoomsPlusKitchen(2)
			.withHousingDescription("Tvåa")
			.withHousingChanged(true)
			.withHousingChangeDescription("Bytt bostad")
			.withHasIncomes(false)
			.withHasPendingBenefits(false)
			.withHasAssets(false)
			.withStaysInMunicipality(false)
			.withStayDescription("Bor utomlands")
			.withAttestation(false)
			.withAttestedAt(ATTESTED_AT)
			.withChildren(List.of(FaChild.create()
				.withPartyId("20180101-1234")
				.withFirstName("Kid")
				.withLastName("Karlsson")
				.withSchoolName("Skolan")
				.withResidenceExtent("FULL")
				.withDaysInHome(30)))
			.withCosts(List.of(FaCost.create()
				.withCostType("RENT")
				.withAppliedAmount(new BigDecimal("5000.00"))
				.withOtherSubType("OTHER_SUB")
				.withSpecification("Spec")
				.withRecipientOrPeriod("Juni")))
			.withIncomes(List.of(FaIncome.create()
				.withIncomeType("SALARY")
				.withAmount(new BigDecimal("12000.00"))
				.withIncomeDate(DATE)
				.withRecipient("Anna")))
			.withPendingBenefits(List.of(FaPendingBenefit.create()
				.withBenefitName("BOSTADSBIDRAG")
				.withApplicantName("Anna")))
			.withAssets(List.of(FaAsset.create()
				.withAssetCategory("VEHICLE")
				.withDescription("Bil")
				.withValue(new BigDecimal("80000.00"))
				.withPropertyType("PROP")
				.withPurchaseYear(2020)
				.withPurchasePrice(new BigDecimal("120000.00"))
				.withCompanyName("Bolaget AB")
				.withCompanyAssetSum(new BigDecimal("50000.00"))
				.withVehicleType("CAR")
				.withRegistrationNumber("ABC123")
				.withPurchaseDate(DATE)))
			.withPersons(List.of(FaPerson.create()
				.withRole("APPLICANT")
				.withPartyId("19900101-1234")
				.withNeedsInterpreter(true)
				.withInterpreterLanguage("Arabiska")
				.withHadWorkLast12Months(false)
				.withHadWorkDescription("Inget arbete")
				.withPaymentMethod("BANK")
				.withClearingNumber("1234")
				.withAccountNumber("567890")
				.withOtherPaymentDescription("Annat")
				.withPaymentSameAsPrevious(true)
				.withEmail("anna@example.com")
				.withPhone("+46701234567")
				.withNotifyByEmail(true)
				.withNotifyBySms(false)))
			.withPlannings(List.of(FaPlanning.create()
				.withPerson("Anna")
				.withPlanningType("WORK")
				.withWorkExtent("100")
				.withWorkDescription("Arbete")
				.withSickLeaveLevel("50")
				.withSfiStudyPath("PATH_1")
				.withSfiCourse("KURS_A")
				.withOtherDescription("Övrigt")))
			.withPlannedActivities(List.of(FaPlannedActivity.create()
				.withPerson("Anna")
				.withActivity("Jobbsökning")
				.withPeriodFrom(DATE)
				.withPeriodTo(DATE.plusMonths(1))))
			.withJobApplications(List.of(FaJobApplication.create()
				.withPerson("Anna")
				.withApplicationDate(DATE)
				.withJobTitle("Snickare")
				.withEmployerAndPlace("Bygg AB, Sundsvall")));
	}
}
