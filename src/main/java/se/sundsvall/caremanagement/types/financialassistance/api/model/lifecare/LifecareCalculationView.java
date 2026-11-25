package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * The errand's beräkning as it stands in Lifecare. The household and its personnummer are left out.
 */
@Schema(description = "The errand's beräkning as it stands in Lifecare.")
public class LifecareCalculationView {

	@Schema(description = "Lifecare's calculation id", examples = "31")
	private Integer id;

	@Schema(description = "The norm", examples = "Riksnorm 2026")
	private String normName;

	@Schema(description = "The calculation date", examples = "2026-09-24")
	private String date;

	@Schema(description = "The start of the period", examples = "2026-09-01")
	private String startDate;

	@Schema(description = "The end of the period", examples = "2026-09-30")
	private String endDate;

	@Schema(description = "Saved as slutlig in Lifecare; it can no longer be changed then")
	private Boolean finalized;

	@Schema(description = "When Lifecare last saved it")
	private String updated;

	@Schema(description = "Lifecare's summering, absent before Lifecare has counted it")
	private LifecareCalculationSummary summary;

	public static LifecareCalculationView create() {
		return new LifecareCalculationView();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public LifecareCalculationView withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getNormName() {
		return normName;
	}

	public void setNormName(final String normName) {
		this.normName = normName;
	}

	public LifecareCalculationView withNormName(final String normName) {
		this.normName = normName;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public LifecareCalculationView withDate(final String date) {
		this.date = date;
		return this;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(final String startDate) {
		this.startDate = startDate;
	}

	public LifecareCalculationView withStartDate(final String startDate) {
		this.startDate = startDate;
		return this;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(final String endDate) {
		this.endDate = endDate;
	}

	public LifecareCalculationView withEndDate(final String endDate) {
		this.endDate = endDate;
		return this;
	}

	public Boolean getFinalized() {
		return finalized;
	}

	public void setFinalized(final Boolean finalized) {
		this.finalized = finalized;
	}

	public LifecareCalculationView withFinalized(final Boolean finalized) {
		this.finalized = finalized;
		return this;
	}

	public String getUpdated() {
		return updated;
	}

	public void setUpdated(final String updated) {
		this.updated = updated;
	}

	public LifecareCalculationView withUpdated(final String updated) {
		this.updated = updated;
		return this;
	}

	public LifecareCalculationSummary getSummary() {
		return summary;
	}

	public void setSummary(final LifecareCalculationSummary summary) {
		this.summary = summary;
	}

	public LifecareCalculationView withSummary(final LifecareCalculationSummary summary) {
		this.summary = summary;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareCalculationView that = (LifecareCalculationView) o;
		return Objects.equals(id, that.id) && Objects.equals(normName, that.normName) && Objects.equals(date, that.date) && Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate) && Objects.equals(finalized, that.finalized)
			&& Objects.equals(updated, that.updated) && Objects.equals(summary, that.summary);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, normName, date, startDate, endDate, finalized, updated, summary);
	}

	@Override
	public String toString() {
		return "LifecareCalculationView{" +
			"id=" + id +
			", normName='" + normName + '\'' +
			", date='" + date + '\'' +
			", startDate='" + startDate + '\'' +
			", endDate='" + endDate + '\'' +
			", finalized=" + finalized +
			", updated='" + updated + '\'' +
			", summary=" + summary +
			'}';
	}
}
