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

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPreviousPerson that = (NormberakningPreviousPerson) o;
		return Objects.equals(name, that.name) && Objects.equals(amount, that.amount) && Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, amount, deviationFromDate, deviationToDate);
	}

	@Override
	public String toString() {
		return "NormberakningPreviousPerson{" +
			"name='" + name + '\'' +
			", amount=" + amount +
			", deviationFromDate='" + deviationFromDate + '\'' +
			", deviationToDate='" + deviationToDate + '\'' +
			'}';
	}
}
