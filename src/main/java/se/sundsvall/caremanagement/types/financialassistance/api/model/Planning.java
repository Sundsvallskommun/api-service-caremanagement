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

	@Schema(description = "Description of the work", examples = "Permanent employment as an assistant nurse")
	private String workDescription;

	@Schema(description = "Level of sick leave (percent)", examples = "100", allowableValues = {
		"100", "75", "50", "25"
	})
	@OneOf(value = {
		"100", "75", "50", "25"
	}, nullable = true)
	private String sickLeaveLevel;

	@Schema(description = "First day of the sick-leave period stated on the medical certificate", examples = "2026-09-01")
	private LocalDate sickLeaveFrom;

	@Schema(description = "Last day of the sick-leave period stated on the medical certificate", examples = "2026-09-30")
	private LocalDate sickLeaveTo;

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

	@Schema(description = "Description of other planning", examples = "Internship via the employment agency")
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

	public LocalDate getSickLeaveFrom() {
		return sickLeaveFrom;
	}

	public void setSickLeaveFrom(final LocalDate sickLeaveFrom) {
		this.sickLeaveFrom = sickLeaveFrom;
	}

	public Planning withSickLeaveFrom(final LocalDate sickLeaveFrom) {
		this.sickLeaveFrom = sickLeaveFrom;
		return this;
	}

	public LocalDate getSickLeaveTo() {
		return sickLeaveTo;
	}

	public void setSickLeaveTo(final LocalDate sickLeaveTo) {
		this.sickLeaveTo = sickLeaveTo;
	}

	public Planning withSickLeaveTo(final LocalDate sickLeaveTo) {
		this.sickLeaveTo = sickLeaveTo;
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
			&& Objects.equals(sickLeaveLevel, that.sickLeaveLevel) && Objects.equals(sickLeaveFrom, that.sickLeaveFrom)
			&& Objects.equals(sickLeaveTo, that.sickLeaveTo) && Objects.equals(sfiStudyPath, that.sfiStudyPath)
			&& Objects.equals(sfiCourse, that.sfiCourse) && Objects.equals(otherDescription, that.otherDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, planningType, workExtent, workDescription, sickLeaveLevel, sickLeaveFrom, sickLeaveTo,
			sfiStudyPath, sfiCourse, otherDescription);
	}

	@Override
	public String toString() {
		return "Planning{person='" + person + "', planningType='" + planningType + "', workExtent='" + workExtent
			+ "', workDescription='" + workDescription + "', sickLeaveLevel='" + sickLeaveLevel + "', sickLeaveFrom=" + sickLeaveFrom + ", sickLeaveTo=" + sickLeaveTo + ", sfiStudyPath='" + sfiStudyPath
			+ "', sfiCourse='" + sfiCourse + "', otherDescription='" + otherDescription + "'}";
	}
}
