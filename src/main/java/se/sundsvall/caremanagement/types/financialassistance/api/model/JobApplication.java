package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "A job application reported by the applicant or co-applicant.")
public class JobApplication {

	@Schema(description = "Which person submitted the job application", examples = "APPLICANT")
	private String person;

	@Schema(description = "The date the job application was submitted", examples = "2026-05-20")
	private LocalDate applicationDate;

	@Schema(description = "The job title applied for", examples = "Lagerarbetare")
	@Size(max = 255)
	private String jobTitle;

	@Schema(description = "Employer and place of work", examples = "PostNord, Sundsvall")
	@Size(max = 255)
	private String employerAndPlace;

	public static JobApplication create() {
		return new JobApplication();
	}

	public String getPerson() {
		return person;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public JobApplication withPerson(final String person) {
		this.person = person;
		return this;
	}

	public LocalDate getApplicationDate() {
		return applicationDate;
	}

	public void setApplicationDate(final LocalDate applicationDate) {
		this.applicationDate = applicationDate;
	}

	public JobApplication withApplicationDate(final LocalDate applicationDate) {
		this.applicationDate = applicationDate;
		return this;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(final String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public JobApplication withJobTitle(final String jobTitle) {
		this.jobTitle = jobTitle;
		return this;
	}

	public String getEmployerAndPlace() {
		return employerAndPlace;
	}

	public void setEmployerAndPlace(final String employerAndPlace) {
		this.employerAndPlace = employerAndPlace;
	}

	public JobApplication withEmployerAndPlace(final String employerAndPlace) {
		this.employerAndPlace = employerAndPlace;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final JobApplication that = (JobApplication) o;
		return Objects.equals(person, that.person) && Objects.equals(applicationDate, that.applicationDate)
			&& Objects.equals(jobTitle, that.jobTitle) && Objects.equals(employerAndPlace, that.employerAndPlace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, applicationDate, jobTitle, employerAndPlace);
	}

	@Override
	public String toString() {
		return "JobApplication{person='" + person + "', applicationDate=" + applicationDate + ", jobTitle='" + jobTitle
			+ "', employerAndPlace='" + employerAndPlace + "'}";
	}
}
