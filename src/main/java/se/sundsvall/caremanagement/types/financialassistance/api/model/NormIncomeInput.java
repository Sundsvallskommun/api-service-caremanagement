package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * What a handläggare sends to add a new income row (origin HANDLAGGARE) or patch an existing one — only the handläggare
 * amount + note are honoured on a patch.
 */
@Schema(description = "What a handläggare sends to add or patch an income row (identity + handläggare-writable fields only).")
public class NormIncomeInput {

	@Schema(description = "The FC income-type id", examples = "20")
	private Integer typeId;

	@Schema(description = "The FC income-type name", examples = "Bostadsbidrag")
	private String typeName;

	@Schema(description = "Whose income this is", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	})
	private String recipient;

	@Schema(description = "The amount the handläggare decided", examples = "1900.00")
	private BigDecimal handlaggareAmount;

	@Schema(description = "The date the handläggare amount is attributed to")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime handlaggareAmountDate;

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

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public NormIncomeInput withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public BigDecimal getHandlaggareAmount() {
		return handlaggareAmount;
	}

	public void setHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
	}

	public NormIncomeInput withHandlaggareAmount(final BigDecimal handlaggareAmount) {
		this.handlaggareAmount = handlaggareAmount;
		return this;
	}

	public OffsetDateTime getHandlaggareAmountDate() {
		return handlaggareAmountDate;
	}

	public void setHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
	}

	public NormIncomeInput withHandlaggareAmountDate(final OffsetDateTime handlaggareAmountDate) {
		this.handlaggareAmountDate = handlaggareAmountDate;
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
		return Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName) && Objects.equals(recipient, that.recipient)
			&& Objects.equals(handlaggareAmount, that.handlaggareAmount) && Objects.equals(handlaggareAmountDate, that.handlaggareAmountDate)
			&& Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeId, typeName, recipient, handlaggareAmount, handlaggareAmountDate, note);
	}

	@Override
	public String toString() {
		return "NormIncomeInput{" +
			"typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", recipient='" + recipient + '\'' +
			", handlaggareAmount=" + handlaggareAmount +
			", handlaggareAmountDate=" + handlaggareAmountDate +
			", note='" + note + '\'' +
			'}';
	}
}
