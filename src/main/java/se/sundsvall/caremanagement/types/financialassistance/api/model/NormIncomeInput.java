package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * What a handläggare sends to add a new income row (origin HANDLAGGARE) or patch an existing one — only the handläggare
 * amounts (applicant/co-applicant sides) + note are honoured on a patch.
 */
@Schema(description = "What a handläggare sends to add or patch an income row (identity + handläggare-writable fields only).")
public class NormIncomeInput {

	@Schema(description = "The FC income-type id", examples = "20")
	private Integer typeId;

	@Schema(description = "The FC income-type name", examples = "Bostadsbidrag")
	private String typeName;

	@Schema(description = "The amount the handläggare decided for the applicant", examples = "1900.00")
	private BigDecimal applicantHandlaggareAmount;

	@Schema(description = "The date the applicant amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime applicantAmountDate;

	@Schema(description = "The amount the handläggare decided for the co-applicant", examples = "1900.00")
	private BigDecimal coapplicantHandlaggareAmount;

	@Schema(description = "The date the co-applicant amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime coapplicantAmountDate;

	@Schema(description = "Free-text note")
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

	public BigDecimal getApplicantHandlaggareAmount() {
		return applicantHandlaggareAmount;
	}

	public void setApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
	}

	public NormIncomeInput withApplicantHandlaggareAmount(final BigDecimal applicantHandlaggareAmount) {
		this.applicantHandlaggareAmount = applicantHandlaggareAmount;
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

	public BigDecimal getCoapplicantHandlaggareAmount() {
		return coapplicantHandlaggareAmount;
	}

	public void setCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
	}

	public NormIncomeInput withCoapplicantHandlaggareAmount(final BigDecimal coapplicantHandlaggareAmount) {
		this.coapplicantHandlaggareAmount = coapplicantHandlaggareAmount;
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
			&& Objects.equals(applicantHandlaggareAmount, that.applicantHandlaggareAmount) && Objects.equals(applicantAmountDate, that.applicantAmountDate)
			&& Objects.equals(coapplicantHandlaggareAmount, that.coapplicantHandlaggareAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate)
			&& Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeId, typeName, applicantHandlaggareAmount, applicantAmountDate, coapplicantHandlaggareAmount, coapplicantAmountDate, note);
	}

	@Override
	public String toString() {
		return "NormIncomeInput{" +
			"typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantHandlaggareAmount=" + applicantHandlaggareAmount +
			", applicantAmountDate=" + applicantAmountDate +
			", coapplicantHandlaggareAmount=" + coapplicantHandlaggareAmount +
			", coapplicantAmountDate=" + coapplicantAmountDate +
			", note='" + note + '\'' +
			'}';
	}
}
