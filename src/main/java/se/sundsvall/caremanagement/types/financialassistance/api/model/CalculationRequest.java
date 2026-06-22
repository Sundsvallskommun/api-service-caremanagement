package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request to build and post an SSBTEK-driven calculation to Lifecare FC for one applicant (and optional co-applicant)
 * and one application month. The household's income basis is fetched from SSBTEK for the rule periods derived from the
 * month; the resulting calculation is created in Lifecare.
 */
@Schema(description = "Request to build and post the SSBTEK-driven calculation for an application month.")
public class CalculationRequest {

	@Schema(description = "The applicant's partyId (personId GUID)", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidUuid
	private String applicant;

	@Schema(description = "The co-applicant's (co-applicant) partyId (personId GUID), when applying together with a partner", examples = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
	@ValidUuid(nullable = true)
	private String coApplicant;

	@Schema(description = "The application month (ISO year-month, yyyy-MM)", examples = "2026-06", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be an ISO year-month (yyyy-MM)")
	private String applicationMonth;

	@Schema(
		description = "The id of the caremanagement errand the calculation concerns. When present, a Decision(RECOMMENDATION) summarising the income warnings is recorded on the errand for the caseworker to review; when omitted, the calculation is built without recording a recommendation.",
		examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	@ValidUuid(nullable = true)
	private String errandId;

	@Schema(description = "The incomes classified by the operaton rules (the evaluate-income-rules worker output), as JSON. When present, caremanagement maps these to FC income rows instead of fetching SSBTEK and evaluating the raw list itself.")
	private String classifiedIncomes;

	@Schema(description = "The unhandled-income warnings from the operaton rules, recorded on the errand recommendation")
	private List<String> unhandledIncomes;

	@Schema(description = "The period-over-period change warnings from the operaton rules, recorded on the errand recommendation")
	private List<String> changeWarnings;

	public static CalculationRequest create() {
		return new CalculationRequest();
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(final String applicant) {
		this.applicant = applicant;
	}

	public CalculationRequest withApplicant(final String applicant) {
		this.applicant = applicant;
		return this;
	}

	public String getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
	}

	public CalculationRequest withCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public CalculationRequest withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public CalculationRequest withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getClassifiedIncomes() {
		return classifiedIncomes;
	}

	public void setClassifiedIncomes(final String classifiedIncomes) {
		this.classifiedIncomes = classifiedIncomes;
	}

	public CalculationRequest withClassifiedIncomes(final String classifiedIncomes) {
		this.classifiedIncomes = classifiedIncomes;
		return this;
	}

	public List<String> getUnhandledIncomes() {
		return unhandledIncomes;
	}

	public void setUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
	}

	public CalculationRequest withUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
		return this;
	}

	public List<String> getChangeWarnings() {
		return changeWarnings;
	}

	public void setChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
	}

	public CalculationRequest withChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CalculationRequest that = (CalculationRequest) o;
		return Objects.equals(applicant, that.applicant) && Objects.equals(coApplicant, that.coApplicant) && Objects.equals(applicationMonth, that.applicationMonth)
			&& Objects.equals(errandId, that.errandId) && Objects.equals(classifiedIncomes, that.classifiedIncomes) && Objects.equals(unhandledIncomes, that.unhandledIncomes)
			&& Objects.equals(changeWarnings, that.changeWarnings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, coApplicant, applicationMonth, errandId, classifiedIncomes, unhandledIncomes, changeWarnings);
	}

	@Override
	public String toString() {
		return "CalculationRequest{applicant='" + applicant + "', coApplicant='" + coApplicant + "', applicationMonth='" + applicationMonth + "', errandId='" + errandId
			+ "', classifiedIncomes='" + classifiedIncomes + "', unhandledIncomes=" + unhandledIncomes + ", changeWarnings=" + changeWarnings + "}";
	}
}
