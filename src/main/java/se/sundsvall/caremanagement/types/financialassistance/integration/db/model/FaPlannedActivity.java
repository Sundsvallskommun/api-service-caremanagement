package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;

import static org.hibernate.Length.LONG32;

@Embeddable
public class FaPlannedActivity {

	@Column(name = "person")
	private String person;

	@Column(name = "activity", length = LONG32)
	private String activity;

	@Column(name = "period_from")
	private LocalDate periodFrom;

	@Column(name = "period_to")
	private LocalDate periodTo;

	public static FaPlannedActivity create() {
		return new FaPlannedActivity();
	}

	public String getPerson() {
		return person;
	}

	public String getActivity() {
		return activity;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public void setActivity(final String activity) {
		this.activity = activity;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public FaPlannedActivity withPerson(final String person) {
		this.person = person;
		return this;
	}

	public FaPlannedActivity withActivity(final String activity) {
		this.activity = activity;
		return this;
	}

	public FaPlannedActivity withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public FaPlannedActivity withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	// 'activity' (a LONG32 column) is deliberately excluded from equals/hashCode/toString — it can be large and is not part
	// of the entity's identity.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaPlannedActivity that = (FaPlannedActivity) o;
		return Objects.equals(person, that.person)
			&& Objects.equals(periodFrom, that.periodFrom) && Objects.equals(periodTo, that.periodTo);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, periodFrom, periodTo);
	}

	@Override
	public String toString() {
		return "FaPlannedActivity{person='" + person + "', periodFrom=" + periodFrom
			+ ", periodTo=" + periodTo + '}';
	}
}
