package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of building and posting an SSBTEK-driven calculation: the id of the calculation created in Lifecare
 * FamilyCare, plus
 * the warnings the caseworker must review — incomes Drakel could not auto-transfer ({@code unhandledIncomes}) and
 * benefits whose net income changed beyond the threshold between the rule periods ({@code changeWarnings}). The
 * warnings
 * are human-readable lines; the structured form lives in the service layer.
 */
@Schema(description = "The created Lifecare calculation id plus the income warnings to review.")
public class CalculationResponse {

	@Schema(description = "The id of the calculation created in Lifecare FamilyCare", examples = "4711")
	private Integer calculationId;

	@Schema(description = "SSBTEK incomes that could not be auto-transferred and must be reviewed", examples = "[\"Bostadstillägg (NOT_ON_WHITELIST)\"]")
	private List<String> unhandledIncomes = new ArrayList<>();

	@Schema(description = "Benefits whose net income changed beyond the threshold between the periods", examples = "[\"Bostadsbidrag: -23% (comparison 2400 -> control 1850)\"]")
	private List<String> changeWarnings = new ArrayList<>();

	@Schema(description = "Whether this month's calculation covers every income type the previous month's did — false means SSBTEK data is still missing and the process should poll again", examples = "true")
	private boolean informationComplete = true;

	@Schema(description = "Previous-month income types not yet present this month (the SSBTEK data still being awaited)", examples = "[\"Bostadsbidrag\"]")
	private List<String> missingIncomeTypes = new ArrayList<>();

	public static CalculationResponse create() {
		return new CalculationResponse();
	}

	public Integer getCalculationId() {
		return calculationId;
	}

	public void setCalculationId(final Integer calculationId) {
		this.calculationId = calculationId;
	}

	public CalculationResponse withCalculationId(final Integer calculationId) {
		this.calculationId = calculationId;
		return this;
	}

	public List<String> getUnhandledIncomes() {
		return unhandledIncomes;
	}

	public void setUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
	}

	public CalculationResponse withUnhandledIncomes(final List<String> unhandledIncomes) {
		this.unhandledIncomes = unhandledIncomes;
		return this;
	}

	public List<String> getChangeWarnings() {
		return changeWarnings;
	}

	public void setChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
	}

	public CalculationResponse withChangeWarnings(final List<String> changeWarnings) {
		this.changeWarnings = changeWarnings;
		return this;
	}

	public boolean isInformationComplete() {
		return informationComplete;
	}

	public void setInformationComplete(final boolean informationComplete) {
		this.informationComplete = informationComplete;
	}

	public CalculationResponse withInformationComplete(final boolean informationComplete) {
		this.informationComplete = informationComplete;
		return this;
	}

	public List<String> getMissingIncomeTypes() {
		return missingIncomeTypes;
	}

	public void setMissingIncomeTypes(final List<String> missingIncomeTypes) {
		this.missingIncomeTypes = missingIncomeTypes;
	}

	public CalculationResponse withMissingIncomeTypes(final List<String> missingIncomeTypes) {
		this.missingIncomeTypes = missingIncomeTypes;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CalculationResponse that = (CalculationResponse) o;
		return informationComplete == that.informationComplete && Objects.equals(calculationId, that.calculationId) && Objects.equals(unhandledIncomes, that.unhandledIncomes)
			&& Objects.equals(changeWarnings, that.changeWarnings) && Objects.equals(missingIncomeTypes, that.missingIncomeTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(calculationId, unhandledIncomes, changeWarnings, informationComplete, missingIncomeTypes);
	}

	@Override
	public String toString() {
		return "CalculationResponse{calculationId=" + calculationId + ", unhandledIncomes=" + unhandledIncomes + ", changeWarnings=" + changeWarnings
			+ ", informationComplete=" + informationComplete + ", missingIncomeTypes=" + missingIncomeTypes + "}";
	}
}
