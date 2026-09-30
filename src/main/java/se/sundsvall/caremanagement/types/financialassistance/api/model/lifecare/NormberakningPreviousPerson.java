package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A household member of a previous Lifecare beräkning, without personnummer.
 */
@Schema(description = "A household member of a previous Lifecare beräkning.")
public class NormberakningPreviousPerson {

	@Schema(description = "The name")
	private String name;

	@Schema(description = "The member's share of the norm")
	private BigDecimal amount;

	@Schema(description = "The start of the deviation")
	private String deviationFromDate;

	@Schema(description = "The end of the deviation")
	private String deviationToDate;

	@Schema(description = "The member's days in the household; absent for the whole period", examples = "15")
	private Integer deviationDays;

	@Schema(description = "The member's place in the household as Lifecare recorded it for this beräkning: SINGLE (ensamstående), COUPLE (sammanboende par) or OTHER; absent when Lifecare gave none",
		examples = "SINGLE",
		allowableValues = {
			"SINGLE", "COUPLE", "OTHER"
		})
	private String relationType;

	public static NormberakningPreviousPerson create() {
		return new NormberakningPreviousPerson();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormberakningPreviousPerson withName(final String name) {
		this.name = name;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public NormberakningPreviousPerson withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormberakningPreviousPerson withDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public String getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormberakningPreviousPerson withDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public Integer getDeviationDays() {
		return deviationDays;
	}

	public void setDeviationDays(final Integer deviationDays) {
		this.deviationDays = deviationDays;
	}

	public NormberakningPreviousPerson withDeviationDays(final Integer deviationDays) {
		this.deviationDays = deviationDays;
		return this;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(final String relationType) {
		this.relationType = relationType;
	}

	public NormberakningPreviousPerson withRelationType(final String relationType) {
		this.relationType = relationType;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPreviousPerson that = (NormberakningPreviousPerson) o;
		return Objects.equals(name, that.name) && Objects.equals(amount, that.amount) && Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate)
			&& Objects.equals(deviationDays, that.deviationDays) && Objects.equals(relationType, that.relationType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, amount, deviationFromDate, deviationToDate, deviationDays, relationType);
	}

	@Override
	public String toString() {
		return "NormberakningPreviousPerson{" +
			"name='" + name + '\'' +
			", amount=" + amount +
			", deviationFromDate='" + deviationFromDate + '\'' +
			", deviationToDate='" + deviationToDate + '\'' +
			", deviationDays=" + deviationDays +
			", relationType='" + relationType + '\'' +
			'}';
	}
}
