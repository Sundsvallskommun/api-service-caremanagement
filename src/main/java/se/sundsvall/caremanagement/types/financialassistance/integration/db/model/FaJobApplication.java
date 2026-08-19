package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class FaJobApplication {

	@Column(name = "person")
	private String person;

	@Column(name = "application_date")
	private LocalDate applicationDate;

	@Column(name = "job_title")
	private String jobTitle;

	@Column(name = "employer_and_place")
	private String employerAndPlace;

	public static FaJobApplication create() {
		return new FaJobApplication();
	}

	public String getPerson() {
		return person;
	}

	public LocalDate getApplicationDate() {
		return applicationDate;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public String getEmployerAndPlace() {
		return employerAndPlace;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public void setApplicationDate(final LocalDate applicationDate) {
		this.applicationDate = applicationDate;
	}

	public void setJobTitle(final String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public void setEmployerAndPlace(final String employerAndPlace) {
		this.employerAndPlace = employerAndPlace;
	}

	public FaJobApplication withPerson(final String person) {
		this.person = person;
		return this;
	}

	public FaJobApplication withApplicationDate(final LocalDate applicationDate) {
		this.applicationDate = applicationDate;
		return this;
	}

	public FaJobApplication withJobTitle(final String jobTitle) {
		this.jobTitle = jobTitle;
		return this;
	}

	public FaJobApplication withEmployerAndPlace(final String employerAndPlace) {
		this.employerAndPlace = employerAndPlace;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaJobApplication that = (FaJobApplication) o;
		return Objects.equals(person, that.person) && Objects.equals(applicationDate, that.applicationDate)
			&& Objects.equals(jobTitle, that.jobTitle) && Objects.equals(employerAndPlace, that.employerAndPlace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, applicationDate, jobTitle, employerAndPlace);
	}

	@Override
	public String toString() {
		return "FaJobApplication{person='" + person + "', applicationDate=" + applicationDate + ", jobTitle='" + jobTitle
			+ "', employerAndPlace='" + employerAndPlace + "'}";
	}
}
