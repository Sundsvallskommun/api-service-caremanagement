package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "The applicant's or co-applicant's planning towards self-sufficiency.")
public class Planning {

	@Schema(description = "Which person the planning concerns", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	})
	@OneOf(value = {
		"APPLICANT", "CO_APPLICANT"
	}, nullable = true)
	private String person;

	@Schema(description = "The type of planning", examples = "WORK", allowableValues = {
		"WORK", "JOBSEEKING", "SICK_LEAVE", "SFI", "OTHER"
	})
	@OneOf(value = {
		"WORK", "JOBSEEKING", "SICK_LEAVE", "SFI", "OTHER"
	}, nullable = true)
	private String planningType;

	@Schema(description = "Extent of work", examples = "FULL", allowableValues = {
		"FULL", "PART"
	})
	@OneOf(value = {
		"FULL", "PART"
	}, nullable = true)
	private String workExtent;

	@Schema(description = "Description of the work", examples = "Tillsvidareanställning som undersköterska")
	private String workDescription;

	@Schema(description = "Level of sick leave (percent)", examples = "100", allowableValues = {
		"100", "75", "50", "25"
	})
	@OneOf(value = {
		"100", "75", "50", "25"
	}, nullable = true)
	private String sickLeaveLevel;

	@Schema(description = "Sick leave from date", examples = "2026-05-01")
	private LocalDate sickFrom;

	@Schema(description = "Sick leave to date", examples = "2026-06-30")
	private LocalDate sickTo;

	@Schema(description = "SFI study path", examples = "1", allowableValues = {
		"1", "2", "3"
	})
	@OneOf(value = {
		"1", "2", "3"
	}, nullable = true)
	private String sfiStudyPath;

	@Schema(description = "SFI course", examples = "B", allowableValues = {
		"A", "B", "C", "D"
	})
	@OneOf(value = {
		"A", "B", "C", "D"
	}, nullable = true)
	private String sfiCourse;

	@Schema(description = "Description of other planning", examples = "Praktik via arbetsförmedlingen")
	private String otherDescription;

	public static Planning create() {
		return new Planning();
	}

	public String getPerson() {
		return person;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public Planning withPerson(final String person) {
		this.person = person;
		return this;
	}

	public String getPlanningType() {
		return planningType;
	}

	public void setPlanningType(final String planningType) {
		this.planningType = planningType;
	}

	public Planning withPlanningType(final String planningType) {
		this.planningType = planningType;
		return this;
	}

	public String getWorkExtent() {
		return workExtent;
	}

	public void setWorkExtent(final String workExtent) {
		this.workExtent = workExtent;
	}

	public Planning withWorkExtent(final String workExtent) {
		this.workExtent = workExtent;
		return this;
	}

	public String getWorkDescription() {
		return workDescription;
	}

	public void setWorkDescription(final String workDescription) {
		this.workDescription = workDescription;
	}

	public Planning withWorkDescription(final String workDescription) {
		this.workDescription = workDescription;
		return this;
	}

	public String getSickLeaveLevel() {
		return sickLeaveLevel;
	}

	public void setSickLeaveLevel(final String sickLeaveLevel) {
		this.sickLeaveLevel = sickLeaveLevel;
	}

	public Planning withSickLeaveLevel(final String sickLeaveLevel) {
		this.sickLeaveLevel = sickLeaveLevel;
		return this;
	}

	public LocalDate getSickFrom() {
		return sickFrom;
	}

	public void setSickFrom(final LocalDate sickFrom) {
		this.sickFrom = sickFrom;
	}

	public Planning withSickFrom(final LocalDate sickFrom) {
		this.sickFrom = sickFrom;
		return this;
	}

	public LocalDate getSickTo() {
		return sickTo;
	}

	public void setSickTo(final LocalDate sickTo) {
		this.sickTo = sickTo;
	}

	public Planning withSickTo(final LocalDate sickTo) {
		this.sickTo = sickTo;
		return this;
	}

	public String getSfiStudyPath() {
		return sfiStudyPath;
	}

	public void setSfiStudyPath(final String sfiStudyPath) {
		this.sfiStudyPath = sfiStudyPath;
	}

	public Planning withSfiStudyPath(final String sfiStudyPath) {
		this.sfiStudyPath = sfiStudyPath;
		return this;
	}

	public String getSfiCourse() {
		return sfiCourse;
	}

	public void setSfiCourse(final String sfiCourse) {
		this.sfiCourse = sfiCourse;
	}

	public Planning withSfiCourse(final String sfiCourse) {
		this.sfiCourse = sfiCourse;
		return this;
	}

	public String getOtherDescription() {
		return otherDescription;
	}

	public void setOtherDescription(final String otherDescription) {
		this.otherDescription = otherDescription;
	}

	public Planning withOtherDescription(final String otherDescription) {
		this.otherDescription = otherDescription;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Planning that = (Planning) o;
		return Objects.equals(person, that.person) && Objects.equals(planningType, that.planningType)
			&& Objects.equals(workExtent, that.workExtent) && Objects.equals(workDescription, that.workDescription)
			&& Objects.equals(sickLeaveLevel, that.sickLeaveLevel) && Objects.equals(sickFrom, that.sickFrom)
			&& Objects.equals(sickTo, that.sickTo) && Objects.equals(sfiStudyPath, that.sfiStudyPath)
			&& Objects.equals(sfiCourse, that.sfiCourse) && Objects.equals(otherDescription, that.otherDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, planningType, workExtent, workDescription, sickLeaveLevel, sickFrom, sickTo,
			sfiStudyPath, sfiCourse, otherDescription);
	}

	@Override
	public String toString() {
		return "Planning{person='" + person + "', planningType='" + planningType + "', workExtent='" + workExtent
			+ "', workDescription='" + workDescription + "', sickLeaveLevel='" + sickLeaveLevel + "', sickFrom=" + sickFrom
			+ ", sickTo=" + sickTo + ", sfiStudyPath='" + sfiStudyPath + "', sfiCourse='" + sfiCourse
			+ "', otherDescription='" + otherDescription + "'}";
	}
}
