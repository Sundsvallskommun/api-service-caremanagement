package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

import static org.hibernate.Length.LONG32;

@Embeddable
public class FaPlanning {

	@Column(name = "person")
	private String person;

	@Column(name = "planning_type")
	private String planningType;

	@Column(name = "work_extent")
	private String workExtent;

	@Column(name = "work_description", length = LONG32)
	private String workDescription;

	@Column(name = "sick_leave_level")
	private String sickLeaveLevel;

	@Column(name = "sfi_study_path")
	private String sfiStudyPath;

	@Column(name = "sfi_course")
	private String sfiCourse;

	@Column(name = "other_description", length = LONG32)
	private String otherDescription;

	public static FaPlanning create() {
		return new FaPlanning();
	}

	public String getPerson() {
		return person;
	}

	public String getPlanningType() {
		return planningType;
	}

	public String getWorkExtent() {
		return workExtent;
	}

	public String getWorkDescription() {
		return workDescription;
	}

	public String getSickLeaveLevel() {
		return sickLeaveLevel;
	}

	public String getSfiStudyPath() {
		return sfiStudyPath;
	}

	public String getSfiCourse() {
		return sfiCourse;
	}

	public String getOtherDescription() {
		return otherDescription;
	}

	public void setPerson(final String person) {
		this.person = person;
	}

	public void setPlanningType(final String planningType) {
		this.planningType = planningType;
	}

	public void setWorkExtent(final String workExtent) {
		this.workExtent = workExtent;
	}

	public void setWorkDescription(final String workDescription) {
		this.workDescription = workDescription;
	}

	public void setSickLeaveLevel(final String sickLeaveLevel) {
		this.sickLeaveLevel = sickLeaveLevel;
	}

	public void setSfiStudyPath(final String sfiStudyPath) {
		this.sfiStudyPath = sfiStudyPath;
	}

	public void setSfiCourse(final String sfiCourse) {
		this.sfiCourse = sfiCourse;
	}

	public void setOtherDescription(final String otherDescription) {
		this.otherDescription = otherDescription;
	}

	public FaPlanning withPerson(final String person) {
		this.person = person;
		return this;
	}

	public FaPlanning withPlanningType(final String planningType) {
		this.planningType = planningType;
		return this;
	}

	public FaPlanning withWorkExtent(final String workExtent) {
		this.workExtent = workExtent;
		return this;
	}

	public FaPlanning withWorkDescription(final String workDescription) {
		this.workDescription = workDescription;
		return this;
	}

	public FaPlanning withSickLeaveLevel(final String sickLeaveLevel) {
		this.sickLeaveLevel = sickLeaveLevel;
		return this;
	}

	public FaPlanning withSfiStudyPath(final String sfiStudyPath) {
		this.sfiStudyPath = sfiStudyPath;
		return this;
	}

	public FaPlanning withSfiCourse(final String sfiCourse) {
		this.sfiCourse = sfiCourse;
		return this;
	}

	public FaPlanning withOtherDescription(final String otherDescription) {
		this.otherDescription = otherDescription;
		return this;
	}

	// 'workDescription' and 'otherDescription' (LONG32 columns) are deliberately excluded from equals/hashCode/toString —
	// they can be large and are not part of the entity's identity.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaPlanning that = (FaPlanning) o;
		return Objects.equals(person, that.person) && Objects.equals(planningType, that.planningType)
			&& Objects.equals(workExtent, that.workExtent)
			&& Objects.equals(sickLeaveLevel, that.sickLeaveLevel) && Objects.equals(sfiStudyPath, that.sfiStudyPath)
			&& Objects.equals(sfiCourse, that.sfiCourse);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, planningType, workExtent, sickLeaveLevel,
			sfiStudyPath, sfiCourse);
	}

	@Override
	public String toString() {
		return "FaPlanning{person='" + person + "', planningType='" + planningType + "', workExtent='" + workExtent
			+ "', sickLeaveLevel='" + sickLeaveLevel + "', sfiStudyPath='" + sfiStudyPath
			+ "', sfiCourse='" + sfiCourse + "'}";
	}
}
