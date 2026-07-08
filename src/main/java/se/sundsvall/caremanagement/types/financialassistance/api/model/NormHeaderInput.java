package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * What a caseworker sends to edit the calculation header — the chosen norm (Norm), the calculation date window
 * (from/to/calculation date) and the custom household size (common costs → custom household size). Only the
 * fields present are applied.
 */
@Schema(description = "Caseworker edit of the calculation header — norm, calculation dates and custom household size.")
public class NormHeaderInput {

	@Schema(description = "The selected FC norm id (Norm)", examples = "5")
	private Integer normId;

	@ArraySchema(schema = @Schema(description = "The norm type", examples = "NATIONAL_NORM", allowableValues = {
		"NATIONAL_NORM", "OTHER_NORM"
	}))
	private List<String> normType;

	@Schema(description = "Calculation period start (from)", examples = "2026-06-01")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationFromDate;

	@Schema(description = "Calculation period end (to)", examples = "2026-06-30")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationToDate;

	@Schema(description = "Calculation date (calculation date)", examples = "2026-06-15")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationDate;

	@Schema(description = "Whether a custom household size is used", examples = "true")
	private Boolean hasCustomHouseholdSize;

	@Schema(description = "The custom household size", examples = "1")
	private Integer householdSize;

	public static NormHeaderInput create() {
		return new NormHeaderInput();
	}

	public Integer getNormId() {
		return normId;
	}

	public void setNormId(final Integer normId) {
		this.normId = normId;
	}

	public NormHeaderInput withNormId(final Integer normId) {
		this.normId = normId;
		return this;
	}

	public List<String> getNormType() {
		return normType;
	}

	public void setNormType(final List<String> normType) {
		this.normType = normType;
	}

	public NormHeaderInput withNormType(final List<String> normType) {
		this.normType = normType;
		return this;
	}

	public LocalDate getCalculationFromDate() {
		return calculationFromDate;
	}

	public void setCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
	}

	public NormHeaderInput withCalculationFromDate(final LocalDate calculationFromDate) {
		this.calculationFromDate = calculationFromDate;
		return this;
	}

	public LocalDate getCalculationToDate() {
		return calculationToDate;
	}

	public void setCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
	}

	public NormHeaderInput withCalculationToDate(final LocalDate calculationToDate) {
		this.calculationToDate = calculationToDate;
		return this;
	}

	public LocalDate getCalculationDate() {
		return calculationDate;
	}

	public void setCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
	}

	public NormHeaderInput withCalculationDate(final LocalDate calculationDate) {
		this.calculationDate = calculationDate;
		return this;
	}

	public Boolean getHasCustomHouseholdSize() {
		return hasCustomHouseholdSize;
	}

	public void setHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
	}

	public NormHeaderInput withHasCustomHouseholdSize(final Boolean hasCustomHouseholdSize) {
		this.hasCustomHouseholdSize = hasCustomHouseholdSize;
		return this;
	}

	public Integer getHouseholdSize() {
		return householdSize;
	}

	public void setHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
	}

	public NormHeaderInput withHouseholdSize(final Integer householdSize) {
		this.householdSize = householdSize;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormHeaderInput that = (NormHeaderInput) o;
		return Objects.equals(normId, that.normId) && Objects.equals(normType, that.normType) && Objects.equals(calculationFromDate, that.calculationFromDate)
			&& Objects.equals(calculationToDate, that.calculationToDate) && Objects.equals(calculationDate, that.calculationDate)
			&& Objects.equals(hasCustomHouseholdSize, that.hasCustomHouseholdSize) && Objects.equals(householdSize, that.householdSize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(normId, normType, calculationFromDate, calculationToDate, calculationDate, hasCustomHouseholdSize, householdSize);
	}

	@Override
	public String toString() {
		return "NormHeaderInput{" +
			"normId=" + normId +
			", normType=" + normType +
			", calculationFromDate=" + calculationFromDate +
			", calculationToDate=" + calculationToDate +
			", calculationDate=" + calculationDate +
			", hasCustomHouseholdSize=" + hasCustomHouseholdSize +
			", householdSize=" + householdSize +
			'}';
	}
}
