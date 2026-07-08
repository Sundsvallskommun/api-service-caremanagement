package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * What a caseworker sends to add a new income row (origin CASEWORKER) or patch an existing one — only the caseworker
 * amounts (applicant/co-applicant sides) + note are honoured on a patch.
 */
@Schema(description = "What a caseworker sends to add or patch an income row (identity + caseworker-writable fields only).")
public class NormIncomeInput {

	@Schema(description = "The FC income-type id", examples = "20")
	private Integer typeId;

	@Schema(description = "The FC income-type name", examples = "Bostadsbidrag")
	@Size(max = 255)
	private String typeName;

	@Schema(description = "The amount the caseworker decided for the applicant", examples = "1900.00")
	private BigDecimal applicantCaseworkerAmount;

	@Schema(description = "The date the applicant amount is attributed to", examples = "2026-06-01T00:00:00Z")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime applicantAmountDate;

	@Schema(description = "The amount the caseworker decided for the co-applicant", examples = "1900.00")
	private BigDecimal coapplicantCaseworkerAmount;

	@Schema(description = "The date the co-applicant amount is attributed to", examples = "2026-06-01T00:00:00Z")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime coapplicantAmountDate;

	@Schema(description = "Free-text note", examples = "Justerat belopp enligt underlag")
	private String note;

	public static NormIncomeInput create() {
		return new NormIncomeInput();
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public NormIncomeInput withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public NormIncomeInput withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public BigDecimal getApplicantCaseworkerAmount() {
		return applicantCaseworkerAmount;
	}

	public void setApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
	}

	public NormIncomeInput withApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
		return this;
	}

	public OffsetDateTime getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public NormIncomeInput withApplicantAmountDate(final OffsetDateTime applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public BigDecimal getCoapplicantCaseworkerAmount() {
		return coapplicantCaseworkerAmount;
	}

	public void setCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
	}

	public NormIncomeInput withCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
		return this;
	}

	public OffsetDateTime getCoapplicantAmountDate() {
		return coapplicantAmountDate;
	}

	public void setCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
	}

	public NormIncomeInput withCoapplicantAmountDate(final OffsetDateTime coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormIncomeInput withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormIncomeInput that = (NormIncomeInput) o;
		return Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName)
			&& Objects.equals(applicantCaseworkerAmount, that.applicantCaseworkerAmount) && Objects.equals(applicantAmountDate, that.applicantAmountDate)
			&& Objects.equals(coapplicantCaseworkerAmount, that.coapplicantCaseworkerAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate)
			&& Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeId, typeName, applicantCaseworkerAmount, applicantAmountDate, coapplicantCaseworkerAmount, coapplicantAmountDate, note);
	}

	@Override
	public String toString() {
		return "NormIncomeInput{" +
			"typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantCaseworkerAmount=" + applicantCaseworkerAmount +
			", applicantAmountDate=" + applicantAmountDate +
			", coapplicantCaseworkerAmount=" + coapplicantCaseworkerAmount +
			", coapplicantAmountDate=" + coapplicantAmountDate +
			", note='" + note + '\'' +
			'}';
	}
}
