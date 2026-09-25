package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * An income row of the normberäkning: one income type with the applicant (S) and co-applicant (M) side.
 */
@Schema(description = "An income row of the normberäkning.")
public class NormberakningIncomeRow {

	@Schema(description = "The row id: careM's row id, or Lifecare's income code")
	private String id;

	@Schema(description = "Stable 0-based position of the row within its section", examples = "0")
	private Integer position;

	@Schema(description = "Who created the row: the process or a caseworker (always CASEWORKER in Lifecare)", allowableValues = {
		"SYSTEM", "CASEWORKER"
	}, accessMode = READ_ONLY)
	private String origin;

	@Schema(description = "The income type id", examples = "19")
	private Integer typeId;

	@Schema(description = "The income type name", examples = "Aktivitetsstöd")
	private String typeName;

	@Schema(description = "The amount the process decided for the applicant (careM draft only)")
	private BigDecimal applicantProcessAmount;

	@Schema(description = "The amount a caseworker decided for the applicant; the gross when jobbstimulans applies")
	private BigDecimal applicantCaseworkerAmount;

	@Schema(description = "The amount actually used for the applicant")
	private BigDecimal applicantEffectiveAmount;

	@Schema(description = "The date the applicant amount is attributed to")
	private String applicantAmountDate;

	@Schema(description = "Whether jobbstimulans applies to the applicant side of this income (Lifecare only)")
	private Boolean applicantJobStimulus;

	@Schema(description = "The applicant amount Lifecare counts once jobbstimulans is taken off (Lifecare only)")
	private BigDecimal applicantCountedAmount;

	@Schema(description = "The amount the process decided for the co-applicant (careM draft only)")
	private BigDecimal coapplicantProcessAmount;

	@Schema(description = "The amount a caseworker decided for the co-applicant")
	private BigDecimal coapplicantCaseworkerAmount;

	@Schema(description = "The amount actually used for the co-applicant")
	private BigDecimal coapplicantEffectiveAmount;

	@Schema(description = "The date the co-applicant amount is attributed to")
	private String coapplicantAmountDate;

	@Schema(description = "Whether the row is soft-deleted (careM draft only)")
	private Boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	public static NormberakningIncomeRow create() {
		return new NormberakningIncomeRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormberakningIncomeRow withId(final String id) {
		this.id = id;
		return this;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public NormberakningIncomeRow withPosition(final Integer position) {
		this.position = position;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormberakningIncomeRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public NormberakningIncomeRow withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public NormberakningIncomeRow withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public BigDecimal getApplicantProcessAmount() {
		return applicantProcessAmount;
	}

	public void setApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
	}

	public NormberakningIncomeRow withApplicantProcessAmount(final BigDecimal applicantProcessAmount) {
		this.applicantProcessAmount = applicantProcessAmount;
		return this;
	}

	public BigDecimal getApplicantCaseworkerAmount() {
		return applicantCaseworkerAmount;
	}

	public void setApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
	}

	public NormberakningIncomeRow withApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
		return this;
	}

	public BigDecimal getApplicantEffectiveAmount() {
		return applicantEffectiveAmount;
	}

	public void setApplicantEffectiveAmount(final BigDecimal applicantEffectiveAmount) {
		this.applicantEffectiveAmount = applicantEffectiveAmount;
	}

	public NormberakningIncomeRow withApplicantEffectiveAmount(final BigDecimal applicantEffectiveAmount) {
		this.applicantEffectiveAmount = applicantEffectiveAmount;
		return this;
	}

	public String getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public NormberakningIncomeRow withApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public Boolean getApplicantJobStimulus() {
		return applicantJobStimulus;
	}

	public void setApplicantJobStimulus(final Boolean applicantJobStimulus) {
		this.applicantJobStimulus = applicantJobStimulus;
	}

	public NormberakningIncomeRow withApplicantJobStimulus(final Boolean applicantJobStimulus) {
		this.applicantJobStimulus = applicantJobStimulus;
		return this;
	}

	public BigDecimal getApplicantCountedAmount() {
		return applicantCountedAmount;
	}

	public void setApplicantCountedAmount(final BigDecimal applicantCountedAmount) {
		this.applicantCountedAmount = applicantCountedAmount;
	}

	public NormberakningIncomeRow withApplicantCountedAmount(final BigDecimal applicantCountedAmount) {
		this.applicantCountedAmount = applicantCountedAmount;
		return this;
	}

	public BigDecimal getCoapplicantProcessAmount() {
		return coapplicantProcessAmount;
	}

	public void setCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
	}

	public NormberakningIncomeRow withCoapplicantProcessAmount(final BigDecimal coapplicantProcessAmount) {
		this.coapplicantProcessAmount = coapplicantProcessAmount;
		return this;
	}

	public BigDecimal getCoapplicantCaseworkerAmount() {
		return coapplicantCaseworkerAmount;
	}

	public void setCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
	}

	public NormberakningIncomeRow withCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
		return this;
	}

	public BigDecimal getCoapplicantEffectiveAmount() {
		return coapplicantEffectiveAmount;
	}

	public void setCoapplicantEffectiveAmount(final BigDecimal coapplicantEffectiveAmount) {
		this.coapplicantEffectiveAmount = coapplicantEffectiveAmount;
	}

	public NormberakningIncomeRow withCoapplicantEffectiveAmount(final BigDecimal coapplicantEffectiveAmount) {
		this.coapplicantEffectiveAmount = coapplicantEffectiveAmount;
		return this;
	}

	public String getCoapplicantAmountDate() {
		return coapplicantAmountDate;
	}

	public void setCoapplicantAmountDate(final String coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
	}

	public NormberakningIncomeRow withCoapplicantAmountDate(final String coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
		return this;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(final Boolean deleted) {
		this.deleted = deleted;
	}

	public NormberakningIncomeRow withDeleted(final Boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormberakningIncomeRow withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningIncomeRow that = (NormberakningIncomeRow) o;
		return Objects.equals(id, that.id) && Objects.equals(position, that.position) && Objects.equals(origin, that.origin) && Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName) && Objects.equals(applicantProcessAmount,
			that.applicantProcessAmount) && Objects.equals(applicantCaseworkerAmount, that.applicantCaseworkerAmount) && Objects.equals(applicantEffectiveAmount, that.applicantEffectiveAmount) && Objects.equals(applicantAmountDate,
				that.applicantAmountDate) && Objects.equals(applicantJobStimulus, that.applicantJobStimulus) && Objects.equals(applicantCountedAmount, that.applicantCountedAmount) && Objects.equals(coapplicantProcessAmount, that.coapplicantProcessAmount)
			&& Objects.equals(coapplicantCaseworkerAmount, that.coapplicantCaseworkerAmount) && Objects.equals(coapplicantEffectiveAmount, that.coapplicantEffectiveAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate) && Objects
				.equals(deleted, that.deleted) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, position, origin, typeId, typeName, applicantProcessAmount, applicantCaseworkerAmount, applicantEffectiveAmount, applicantAmountDate, applicantJobStimulus, applicantCountedAmount, coapplicantProcessAmount,
			coapplicantCaseworkerAmount, coapplicantEffectiveAmount, coapplicantAmountDate, deleted, note);
	}

	@Override
	public String toString() {
		return "NormberakningIncomeRow{" +
			"id='" + id + '\'' +
			", position=" + position +
			", origin='" + origin + '\'' +
			", typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantProcessAmount=" + applicantProcessAmount +
			", applicantCaseworkerAmount=" + applicantCaseworkerAmount +
			", applicantEffectiveAmount=" + applicantEffectiveAmount +
			", applicantAmountDate='" + applicantAmountDate + '\'' +
			", applicantJobStimulus=" + applicantJobStimulus +
			", applicantCountedAmount=" + applicantCountedAmount +
			", coapplicantProcessAmount=" + coapplicantProcessAmount +
			", coapplicantCaseworkerAmount=" + coapplicantCaseworkerAmount +
			", coapplicantEffectiveAmount=" + coapplicantEffectiveAmount +
			", coapplicantAmountDate='" + coapplicantAmountDate + '\'' +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			'}';
	}
}
