package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateFinancialAssistanceRequestTest {

	private static final FinancialAssistanceData DATA = FinancialAssistanceData.create().withApplicationType("NEW");

	@Test
	void builderMethods() {
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Application for financial assistance")
			.withDescription("Renewal om rent")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withData(DATA);

		assertThat(request.getTitle()).isEqualTo("Application for financial assistance");
		assertThat(request.getDescription()).isEqualTo("Renewal om rent");
		assertThat(request.getPriority()).isEqualTo("HIGH");
		assertThat(request.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(request.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(request.getData()).isEqualTo(DATA);
		assertThat(request).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var request = CreateFinancialAssistanceRequest.create();
		request.setTitle("title");
		request.setDescription("description");
		request.setPriority("LOW");
		request.setReporterUserId("reporter");
		request.setAssignedUserId("assignee");
		request.setData(DATA);

		assertThat(request.getTitle()).isEqualTo("title");
		assertThat(request.getDescription()).isEqualTo("description");
		assertThat(request.getPriority()).isEqualTo("LOW");
		assertThat(request.getReporterUserId()).isEqualTo("reporter");
		assertThat(request.getAssignedUserId()).isEqualTo("assignee");
		assertThat(request.getData()).isEqualTo(DATA);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(CreateFinancialAssistanceRequest.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = CreateFinancialAssistanceRequest.create().withTitle("T").withData(DATA);
		final var b = CreateFinancialAssistanceRequest.create().withTitle("T").withData(DATA);
		final var c = CreateFinancialAssistanceRequest.create().withTitle("X");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
