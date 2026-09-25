package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * A row added to or changed in the normberäkning: the union of the three sections' inputs. Before the beräkning is in
 * Lifecare it goes to careM's draft (section-specific fields only); after that it is made in Lifecare.
 */
@Schema(description = "A normberäkning row as a caseworker adds or changes it.")
public class NormberakningRowInput {

	/** An ISO date, optionally followed by a time. */
	static final String DATE_PREFIX = "^\\d{4}-\\d{2}-\\d{2}.*$";

	@Schema(description = "Income: the income type id", examples = "19")
	private Integer typeId;

	@Schema(description = "Income: the income type name", examples = "Aktivitetsstöd")
	@Size(max = 255)
	private String typeName;

	@Schema(description = "Income: the applicant amount", examples = "5000")
	private BigDecimal applicantCaseworkerAmount;

	@Schema(description = "Income: the date the applicant amount is attributed to (ISO date or date-time)", examples = "2026-09-02T00:00:00+02:00")
	@Pattern(regexp = DATE_PREFIX)
	private String applicantAmountDate;

	@Schema(description = "Income: the co-applicant amount", examples = "0")
	private BigDecimal coapplicantCaseworkerAmount;

	@Schema(description = "Income: the date the co-applicant amount is attributed to (ISO date or date-time)", examples = "2026-09-02T00:00:00+02:00")
	@Pattern(regexp = DATE_PREFIX)
	private String coapplicantAmountDate;

	@Schema(description = "Expense: the cost type; careM's code in the draft, Lifecare's expense code once in Lifecare", examples = "3")
	@Size(max = 64)
	private String costType;

	@Schema(description = "Expense: the bucket", allowableValues = {
		"EXPENSE", "SPECIAL_EXPENSE"
	}, examples = "EXPENSE")
	@OneOf(value = {
		"EXPENSE", "SPECIAL_EXPENSE"
	}, nullable = true)
	private String bucket;

	@Schema(description = "Expense: the other sub-type (careM draft only)")
	@Size(max = 32)
	private String otherSubType;

	@Schema(description = "Expense: the specification (careM draft only)")
	private String specification;

	@Schema(description = "Expense: the approved amount", examples = "4500")
	private BigDecimal caseworkerAmount;

	@Schema(description = "Expense: the amount applied for", examples = "5000")
	private BigDecimal appliedAmount;

	@Schema(description = "Person: the party id (careM draft only)")
	@Size(max = 36)
	private String partyId;

	@Schema(description = "Person: the role (careM draft only)", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD", "VISITATION_CHILD"
	})
	private String role;

	@Schema(description = "Person: the name (careM draft only)")
	@Size(max = 255)
	private String name;

	@Schema(description = "Person: the days in the household; none means the whole period", examples = "10")
	private Integer caseworkerDays;

	@Schema(description = "Person: whether the member is included (careM draft only)")
	private Boolean included;

	@Schema(description = "Person: the start of the deviation, ISO date (careM draft only)", examples = "2026-09-01")
	@Pattern(regexp = DATE_PREFIX)
	private String deviationFromDate;

	@Schema(description = "Person: the end of the deviation, ISO date (careM draft only)", examples = "2026-09-30")
	@Pattern(regexp = DATE_PREFIX)
	private String deviationToDate;

	@Schema(description = "Person: the norm interval (careM draft only)")
	@Size(max = 64)
	private String normInterval;

	@Schema(description = "Person: the Lifecare norm row the member is put on (Lifecare only)", examples = "12")
	private Integer normRowId;

	@Schema(description = "Free-text note; Lifecare takes at most 80 characters", maxLength = 80)
	@Size(max = 80)
	private String note;

	public static NormberakningRowInput create() {
		return new NormberakningRowInput();
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(final Integer typeId) {
		this.typeId = typeId;
	}

	public NormberakningRowInput withTypeId(final Integer typeId) {
		this.typeId = typeId;
		return this;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	public NormberakningRowInput withTypeName(final String typeName) {
		this.typeName = typeName;
		return this;
	}

	public BigDecimal getApplicantCaseworkerAmount() {
		return applicantCaseworkerAmount;
	}

	public void setApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
	}

	public NormberakningRowInput withApplicantCaseworkerAmount(final BigDecimal applicantCaseworkerAmount) {
		this.applicantCaseworkerAmount = applicantCaseworkerAmount;
		return this;
	}

	public String getApplicantAmountDate() {
		return applicantAmountDate;
	}

	public void setApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
	}

	public NormberakningRowInput withApplicantAmountDate(final String applicantAmountDate) {
		this.applicantAmountDate = applicantAmountDate;
		return this;
	}

	public BigDecimal getCoapplicantCaseworkerAmount() {
		return coapplicantCaseworkerAmount;
	}

	public void setCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
	}

	public NormberakningRowInput withCoapplicantCaseworkerAmount(final BigDecimal coapplicantCaseworkerAmount) {
		this.coapplicantCaseworkerAmount = coapplicantCaseworkerAmount;
		return this;
	}

	public String getCoapplicantAmountDate() {
		return coapplicantAmountDate;
	}

	public void setCoapplicantAmountDate(final String coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
	}

	public NormberakningRowInput withCoapplicantAmountDate(final String coapplicantAmountDate) {
		this.coapplicantAmountDate = coapplicantAmountDate;
		return this;
	}

	public String getCostType() {
		return costType;
	}

	public void setCostType(final String costType) {
		this.costType = costType;
	}

	public NormberakningRowInput withCostType(final String costType) {
		this.costType = costType;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(final String bucket) {
		this.bucket = bucket;
	}

	public NormberakningRowInput withBucket(final String bucket) {
		this.bucket = bucket;
		return this;
	}

	public String getOtherSubType() {
		return otherSubType;
	}

	public void setOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
	}

	public NormberakningRowInput withOtherSubType(final String otherSubType) {
		this.otherSubType = otherSubType;
		return this;
	}

	public String getSpecification() {
		return specification;
	}

	public void setSpecification(final String specification) {
		this.specification = specification;
	}

	public NormberakningRowInput withSpecification(final String specification) {
		this.specification = specification;
		return this;
	}

	public BigDecimal getCaseworkerAmount() {
		return caseworkerAmount;
	}

	public void setCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
	}

	public NormberakningRowInput withCaseworkerAmount(final BigDecimal caseworkerAmount) {
		this.caseworkerAmount = caseworkerAmount;
		return this;
	}

	public BigDecimal getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public NormberakningRowInput withAppliedAmount(final BigDecimal appliedAmount) {
		this.appliedAmount = appliedAmount;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormberakningRowInput withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormberakningRowInput withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormberakningRowInput withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getCaseworkerDays() {
		return caseworkerDays;
	}

	public void setCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
	}

	public NormberakningRowInput withCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
		return this;
	}

	public Boolean getIncluded() {
		return included;
	}

	public void setIncluded(final Boolean included) {
		this.included = included;
	}

	public NormberakningRowInput withIncluded(final Boolean included) {
		this.included = included;
		return this;
	}

	public String getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormberakningRowInput withDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public String getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormberakningRowInput withDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public NormberakningRowInput withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public Integer getNormRowId() {
		return normRowId;
	}

	public void setNormRowId(final Integer normRowId) {
		this.normRowId = normRowId;
	}

	public NormberakningRowInput withNormRowId(final Integer normRowId) {
		this.normRowId = normRowId;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormberakningRowInput withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningRowInput that = (NormberakningRowInput) o;
		return Objects.equals(typeId, that.typeId) && Objects.equals(typeName, that.typeName) && Objects.equals(applicantCaseworkerAmount, that.applicantCaseworkerAmount) && Objects.equals(applicantAmountDate, that.applicantAmountDate) && Objects.equals(
			coapplicantCaseworkerAmount, that.coapplicantCaseworkerAmount) && Objects.equals(coapplicantAmountDate, that.coapplicantAmountDate) && Objects.equals(costType, that.costType) && Objects.equals(bucket, that.bucket) && Objects.equals(
				otherSubType, that.otherSubType) && Objects.equals(specification, that.specification) && Objects.equals(caseworkerAmount, that.caseworkerAmount) && Objects.equals(appliedAmount, that.appliedAmount) && Objects.equals(partyId, that.partyId)
			&& Objects.equals(role, that.role) && Objects.equals(name, that.name) && Objects.equals(caseworkerDays, that.caseworkerDays) && Objects.equals(included, that.included) && Objects.equals(deviationFromDate, that.deviationFromDate) && Objects
				.equals(deviationToDate, that.deviationToDate) && Objects.equals(normInterval, that.normInterval) && Objects.equals(normRowId, that.normRowId) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeId, typeName, applicantCaseworkerAmount, applicantAmountDate, coapplicantCaseworkerAmount, coapplicantAmountDate, costType, bucket, otherSubType, specification, caseworkerAmount, appliedAmount, partyId, role, name,
			caseworkerDays, included, deviationFromDate, deviationToDate, normInterval, normRowId, note);
	}

	@Override
	public String toString() {
		return "NormberakningRowInput{" +
			"typeId=" + typeId +
			", typeName='" + typeName + '\'' +
			", applicantCaseworkerAmount=" + applicantCaseworkerAmount +
			", applicantAmountDate='" + applicantAmountDate + '\'' +
			", coapplicantCaseworkerAmount=" + coapplicantCaseworkerAmount +
			", coapplicantAmountDate='" + coapplicantAmountDate + '\'' +
			", costType='" + costType + '\'' +
			", bucket='" + bucket + '\'' +
			", otherSubType='" + otherSubType + '\'' +
			", specification='" + specification + '\'' +
			", caseworkerAmount=" + caseworkerAmount +
			", appliedAmount=" + appliedAmount +
			", partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", caseworkerDays=" + caseworkerDays +
			", included=" + included +
			", deviationFromDate='" + deviationFromDate + '\'' +
			", deviationToDate='" + deviationToDate + '\'' +
			", normInterval='" + normInterval + '\'' +
			", normRowId=" + normRowId +
			", note='" + note + '\'' +
			'}';
	}
}
