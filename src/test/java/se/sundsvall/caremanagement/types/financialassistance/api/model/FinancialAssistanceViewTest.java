package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.decisions.api.model.Decision;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialAssistanceViewTest {

	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T10:00:00Z");
	private static final OffsetDateTime TOUCHED = OffsetDateTime.parse("2026-06-05T10:00:00Z");
	private static final OffsetDateTime LAST_DAILY_RUN_AT = OffsetDateTime.parse("2026-06-06T03:00:00Z");
	private static final FinancialAssistanceData DATA = FinancialAssistanceData.create().withApplicationType("NEW");
	private static final Decision RECOMMENDATION = Decision.create().withDecisionType("RECOMMENDATION").withValue("OK");
	private static final SectionApprovals SECTION_APPROVALS = SectionApprovals.create()
		.withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true))
		.withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(false))
		.withDecision(SectionApproval.create().withSection("DECISION").withApproved(false));

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void builderMethods() {
		final var view = FinancialAssistanceView.create()
			.withId("cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.withErrandNumber("EB-26060042")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withTypeSlug("financial-assistance")
			.withTitle("Application for financial assistance")
			.withStatus("ONGOING")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withProcessInstanceId("8d2e1c3a-4f56-7890-abcd-ef1234567890")
			.withCreated(CREATED)
			.withModified(MODIFIED)
			.withTouched(TOUCHED)
			.withLastDailyRunAt(LAST_DAILY_RUN_AT)
			.withData(DATA)
			.withRecommendation(RECOMMENDATION)
			.withSectionApprovals(SECTION_APPROVALS);

		assertThat(view.getId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
		assertThat(view.getErrandNumber()).isEqualTo("EB-26060042");
		assertThat(view.getMunicipalityId()).isEqualTo("2281");
		assertThat(view.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(view.getTypeSlug()).isEqualTo("financial-assistance");
		assertThat(view.getTitle()).isEqualTo("Application for financial assistance");
		assertThat(view.getStatus()).isEqualTo("ONGOING");
		assertThat(view.getPriority()).isEqualTo("HIGH");
		assertThat(view.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(view.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(view.getProcessInstanceId()).isEqualTo("8d2e1c3a-4f56-7890-abcd-ef1234567890");
		assertThat(view.getCreated()).isEqualTo(CREATED);
		assertThat(view.getModified()).isEqualTo(MODIFIED);
		assertThat(view.getTouched()).isEqualTo(TOUCHED);
		assertThat(view.getLastDailyRunAt()).isEqualTo(LAST_DAILY_RUN_AT);
		assertThat(view.getData()).isEqualTo(DATA);
		assertThat(view.getRecommendation()).isEqualTo(RECOMMENDATION);
		assertThat(view.getSectionApprovals()).isEqualTo(SECTION_APPROVALS);
		assertThat(view).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var view = FinancialAssistanceView.create();
		view.setId("id");
		view.setErrandNumber("EB-26060001");
		view.setMunicipalityId("2281");
		view.setNamespace("FINANCIAL_ASSISTANCE");
		view.setTypeSlug("financial-assistance");
		view.setTitle("title");
		view.setStatus("NEW");
		view.setPriority("MEDIUM");
		view.setReporterUserId("reporter");
		view.setAssignedUserId("assignee");
		view.setProcessInstanceId("pi-1");
		view.setCreated(CREATED);
		view.setModified(MODIFIED);
		view.setTouched(TOUCHED);
		view.setData(DATA);
		view.setRecommendation(RECOMMENDATION);

		assertThat(view.getId()).isEqualTo("id");
		assertThat(view.getErrandNumber()).isEqualTo("EB-26060001");
		assertThat(view.getStatus()).isEqualTo("NEW");
		assertThat(view.getTouched()).isEqualTo(TOUCHED);
		assertThat(view.getData()).isEqualTo(DATA);
		assertThat(view.getRecommendation()).isEqualTo(RECOMMENDATION);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(FinancialAssistanceView.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = FinancialAssistanceView.create().withId("1").withTitle("T").withData(DATA);
		final var b = FinancialAssistanceView.create().withId("1").withTitle("T").withData(DATA);
		final var c = FinancialAssistanceView.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
