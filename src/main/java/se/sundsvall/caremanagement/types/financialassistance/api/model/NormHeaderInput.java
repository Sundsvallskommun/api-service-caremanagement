package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * What a handläggare sends to edit the normberäkning header — the chosen norm (Norm), the calculation date window
 * (Från/Till/Beräkningsdatum) and the custom household size (Gemensamma kostnader → Annan hushållsstorlek). Only the
 * fields present are applied.
 */
@Schema(description = "Handläggare edit of the normberäkning header — norm, calculation dates and custom household size.")
public class NormHeaderInput {

	@Schema(description = "The selected FC norm id (Norm)", examples = "5")
	private Integer normId;

	@Schema(description = "The norm type", allowableValues = {
		"RIKSNORM", "OTHER_NORM"
	})
	private String normType;

	@Schema(description = "Calculation period start (Från)")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationFromDate;

	@Schema(description = "Calculation period end (Till)")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationToDate;

	@Schema(description = "Calculation date (Beräkningsdatum)")
	@DateTimeFormat(iso = DATE)
	private LocalDate calculationDate;

	@Schema(description = "Whether a custom household size is used (Annan hushållsstorlek)", examples = "true")
	private Boolean hasCustomHouseholdSize;

	@Schema(description = "The custom household size (Hushållsstorlek)", examples = "1")
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

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public NormHeaderInput withNormType(final String normType) {
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
			", normType='" + normType + '\'' +
			", calculationFromDate=" + calculationFromDate +
			", calculationToDate=" + calculationToDate +
			", calculationDate=" + calculationDate +
			", hasCustomHouseholdSize=" + hasCustomHouseholdSize +
			", householdSize=" + householdSize +
			'}';
	}
}
