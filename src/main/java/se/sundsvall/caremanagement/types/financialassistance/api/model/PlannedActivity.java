package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "A planned activity for the applicant or co-applicant.")
public class PlannedActivity {

	@Schema(description = "Which person the activity concerns", examples = "APPLICANT")
	private String person;

	@Schema(description = "Description of the activity", examples = "Work training placement")
	private String activity;

	@Schema(description = "Period from date", examples = "2026-06-01")
	private LocalDate periodFrom;

	@Schema(description = "Period to date", examples = "2026-08-31")
	private LocalDate periodTo;

	public static PlannedActivity create() {
		return new PlannedActivity();
	}

	public String getPerson() {
		return person;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public PlannedActivity withPerson(final String person) {
		this.person = person;
		return this;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(final String activity) {
		this.activity = activity;
	}

	public PlannedActivity withActivity(final String activity) {
		this.activity = activity;
		return this;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public PlannedActivity withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public PlannedActivity withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PlannedActivity that = (PlannedActivity) o;
		return Objects.equals(person, that.person) && Objects.equals(activity, that.activity)
			&& Objects.equals(periodFrom, that.periodFrom) && Objects.equals(periodTo, that.periodTo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, activity, periodFrom, periodTo);
	}

	@Override
	public String toString() {
		return "PlannedActivity{person='" + person + "', activity='" + activity + "', periodFrom=" + periodFrom
			+ ", periodTo=" + periodTo + '}';
	}
}
