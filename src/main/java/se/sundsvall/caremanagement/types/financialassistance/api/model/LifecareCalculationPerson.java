package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single household member on a Lifecare calculation — the amount the member contributes to the norm and any
 * deviation period Lifecare applied.
 */
@Schema(description = "A household member on a Lifecare calculation.")
public class LifecareCalculationPerson {

	@Schema(description = "The Lifecare person id", examples = "200001011234")
	private String personId;

	@Schema(description = "The person name", examples = "Anna Andersson")
	private String name;

	@Schema(description = "The amount the person contributes to the norm", examples = "4500.0")
	private Double amount;

	@Schema(description = "The start date of the deviation period, when any", examples = "2026-06-01")
	private String deviationFromDate;

	@Schema(description = "The end date of the deviation period, when any", examples = "2026-06-30")
	private String deviationToDate;

	public static LifecareCalculationPerson create() {
		return new LifecareCalculationPerson();
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(final String personId) {
		this.personId = personId;
	}

	public LifecareCalculationPerson withPersonId(final String personId) {
		this.personId = personId;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public LifecareCalculationPerson withName(final String name) {
		this.name = name;
		return this;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(final Double amount) {
		this.amount = amount;
	}

	public LifecareCalculationPerson withAmount(final Double amount) {
		this.amount = amount;
		return this;
	}

	public String getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public LifecareCalculationPerson withDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public String getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public LifecareCalculationPerson withDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareCalculationPerson that = (LifecareCalculationPerson) o;
		return Objects.equals(personId, that.personId) && Objects.equals(name, that.name) && Objects.equals(amount, that.amount)
			&& Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(personId, name, amount, deviationFromDate, deviationToDate);
	}

	@Override
	public String toString() {
		return "LifecareCalculationPerson{personId='" + personId + "', name='" + name + "', amount=" + amount + ", deviationFromDate='" + deviationFromDate
			+ "', deviationToDate='" + deviationToDate + "'}";
	}
}
