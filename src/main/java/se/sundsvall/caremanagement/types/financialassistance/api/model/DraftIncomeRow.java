package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One income row of the draft calculation — an FamilyCare income type with the applicant's and co-applicant's amounts.
 * Also
 * the persisted JSON shape (in the draft's {@code rows_json}). A caseworker edits these in Draken; on a decision they
 * are
 * posted to Lifecare.
 */
@Schema(description = "One income row of the draft calculation (FamilyCare income type + amounts).")
public class DraftIncomeRow {

	@Schema(description = "The FamilyCare income-type id", examples = "20")
	private Integer typeId;

	@Schema(description = "The FamilyCare income-type name", examples = "Bostadsbidrag")
	private String typeName;

	@Schema(description = "The applicant's amount for this income type", examples = "1850.0")
	private Double applicantAmount;

	@Schema(description = "The date the applicant's amount is attributed to (ISO)", examples = "2026-05-15T00:00:00Z")
	private String applicantAmountDate;

	@Schema(description = "The co-applicant's amount for this income type", examples = "0.0")
	private Double coApplicantAmount;

	@Schema(description = "The date the co-applicant's amount is attributed to (ISO)")
	private String coApplicantAmountDate;

	@Schema(description = "Free-text note (e.g. the SSBTEK source)", examples = "SSBTEK: Bostadsbidrag")
	private String note;

	public static DraftIncomeRow create() {
		return new DraftIncomeRow();
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public DraftIncomeRow withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public DraftIncomeRow withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public Double getApplicantAmount() {
		return applicantAmount;
	}

	public void setApplicantAmount(final Double applicantAmount) {
		this.applicantAmount = applicantAmount;
	}

	public DraftIncomeRow withApplicantAmount(final Double applicantAmount) {
		this.applicantAmount = applicantAmount;
		return this;
	}

	public String getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public DraftIncomeRow withApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public Double getCoApplicantAmount() {
		return coApplicantAmount;
	}

	public void setCoApplicantAmount(final Double coApplicantAmount) {
		this.coApplicantAmount = coApplicantAmount;
	}

	public DraftIncomeRow withCoApplicantAmount(final Double coApplicantAmount) {
		this.coApplicantAmount = coApplicantAmount;
		return this;
	}

	public String getCoApplicantAmountDate() {
		return coApplicantAmountDate;
	}

	public void setCoApplicantAmountDate(final String coApplicantAmountDate) {
		this.coApplicantAmountDate = coApplicantAmountDate;
	}

	public DraftIncomeRow withCoApplicantAmountDate(final String coApplicantAmountDate) {
		this.coApplicantAmountDate = coApplicantAmountDate;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public DraftIncomeRow withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final DraftIncomeRow that = (DraftIncomeRow) o;
		return Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName) && Objects.equals(applicantAmount, that.applicantAmount)
			&& Objects.equals(applicantAmountDate, that.applicantAmountDate) && Objects.equals(coApplicantAmount, that.coApplicantAmount)
			&& Objects.equals(coApplicantAmountDate, that.coApplicantAmountDate) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeId, typeName, applicantAmount, applicantAmountDate, coApplicantAmount, coApplicantAmountDate, note);
	}

	@Override
	public String toString() {
		return "DraftIncomeRow{" +
			"typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantAmount=" + applicantAmount +
			", applicantAmountDate='" + applicantAmountDate + '\'' +
			", coApplicantAmount=" + coApplicantAmount +
			", coApplicantAmountDate='" + coApplicantAmountDate + '\'' +
			", note='" + note + '\'' +
			'}';
	}
}
