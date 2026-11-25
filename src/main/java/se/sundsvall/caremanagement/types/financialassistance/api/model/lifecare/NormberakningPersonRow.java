package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A household member row of the normberäkning, in careM's draft or in the beräkning saved in Lifecare.
 */
@Schema(description = "A household member row of the normberäkning.")
public class NormberakningPersonRow {

	@Schema(description = "The row id: careM's row id, or Lifecare's personKey (new-N for a member not saved yet)")
	private String id;

	@Schema(description = "Stable 0-based position of the row within its section", examples = "0")
	private Integer position;

	@Schema(description = "Who created the row: the process or a caseworker (always CASEWORKER in Lifecare)", allowableValues = {
		"SYSTEM", "CASEWORKER"
	}, accessMode = READ_ONLY)
	private String origin;

	@Schema(description = "The party id of the household member (careM draft only)")
	private String partyId;

	@Schema(description = "The personnummer of the household member; best-effort, can be absent")
	private String personalNumber;

	@Schema(description = "The role of the household member", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD", "VISITATION_CHILD"
	})
	private String role;

	@Schema(description = "Swedish display name for the role (careM draft only)", examples = "Medsökande")
	private String roleDisplayName;

	@Schema(description = "The name of the household member")
	private String name;

	@Schema(description = "The number of days in the home the process derived (careM draft only)", examples = "30")
	private Integer processDays;

	@Schema(description = "The number of days a caseworker decided", examples = "15")
	private Integer caseworkerDays;

	@Schema(description = "The number of days actually used", examples = "15")
	private Integer effectiveDays;

	@Schema(description = "Whether the household member is included in the norm")
	private Boolean included;

	@Schema(description = "The start date of the member's deviation from the household (ISO date)", examples = "2026-09-05")
	private String deviationFromDate;

	@Schema(description = "The end date of the member's deviation from the household (ISO date)", examples = "2026-09-30")
	private String deviationToDate;

	@Schema(description = "The normintervall the member is placed on", examples = "Ensamstående")
	private String normInterval;

	@Schema(description = "The Lifecare norm row the member is placed on (Lifecare only)", examples = "2")
	private Integer normRowId;

	@Schema(description = "The member's own share of the norm (the Belopp column)", examples = "3940")
	private BigDecimal amount;

	@Schema(description = "Whether the row is soft-deleted (careM draft only)")
	private Boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	public static NormberakningPersonRow create() {
		return new NormberakningPersonRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormberakningPersonRow withId(final String id) {
		this.id = id;
		return this;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public NormberakningPersonRow withPosition(final Integer position) {
		this.position = position;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormberakningPersonRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormberakningPersonRow withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getPersonalNumber() {
		return personalNumber;
	}

	public void setPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
	}

	public NormberakningPersonRow withPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormberakningPersonRow withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getRoleDisplayName() {
		return roleDisplayName;
	}

	public void setRoleDisplayName(final String roleDisplayName) {
		this.roleDisplayName = roleDisplayName;
	}

	public NormberakningPersonRow withRoleDisplayName(final String roleDisplayName) {
		this.roleDisplayName = roleDisplayName;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormberakningPersonRow withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getProcessDays() {
		return processDays;
	}

	public void setProcessDays(final Integer processDays) {
		this.processDays = processDays;
	}

	public NormberakningPersonRow withProcessDays(final Integer processDays) {
		this.processDays = processDays;
		return this;
	}

	public Integer getCaseworkerDays() {
		return caseworkerDays;
	}

	public void setCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
	}

	public NormberakningPersonRow withCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
		return this;
	}

	public Integer getEffectiveDays() {
		return effectiveDays;
	}

	public void setEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
	}

	public NormberakningPersonRow withEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
		return this;
	}

	public Boolean getIncluded() {
		return included;
	}

	public void setIncluded(final Boolean included) {
		this.included = included;
	}

	public NormberakningPersonRow withIncluded(final Boolean included) {
		this.included = included;
		return this;
	}

	public String getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormberakningPersonRow withDeviationFromDate(final String deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public String getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormberakningPersonRow withDeviationToDate(final String deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public NormberakningPersonRow withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public Integer getNormRowId() {
		return normRowId;
	}

	public void setNormRowId(final Integer normRowId) {
		this.normRowId = normRowId;
	}

	public NormberakningPersonRow withNormRowId(final Integer normRowId) {
		this.normRowId = normRowId;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public NormberakningPersonRow withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(final Boolean deleted) {
		this.deleted = deleted;
	}

	public NormberakningPersonRow withDeleted(final Boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormberakningPersonRow withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPersonRow that = (NormberakningPersonRow) o;
		return Objects.equals(id, that.id) && Objects.equals(position, that.position) && Objects.equals(origin, that.origin) && Objects.equals(partyId, that.partyId) && Objects.equals(personalNumber, that.personalNumber) && Objects.equals(role, that.role)
			&& Objects.equals(roleDisplayName, that.roleDisplayName) && Objects.equals(name, that.name) && Objects.equals(processDays, that.processDays) && Objects.equals(caseworkerDays, that.caseworkerDays) && Objects.equals(effectiveDays,
				that.effectiveDays) && Objects.equals(included, that.included) && Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate) && Objects.equals(normInterval, that.normInterval)
			&& Objects.equals(normRowId, that.normRowId) && Objects.equals(amount, that.amount) && Objects.equals(deleted, that.deleted) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, position, origin, partyId, personalNumber, role, roleDisplayName, name, processDays, caseworkerDays, effectiveDays, included, deviationFromDate, deviationToDate, normInterval, normRowId, amount, deleted, note);
	}

	@Override
	public String toString() {
		return "NormberakningPersonRow{" +
			"id='" + id + '\'' +
			", position=" + position +
			", origin='" + origin + '\'' +
			", partyId='" + partyId + '\'' +
			", personalNumber='" + personalNumber + '\'' +
			", role='" + role + '\'' +
			", roleDisplayName='" + roleDisplayName + '\'' +
			", name='" + name + '\'' +
			", processDays=" + processDays +
			", caseworkerDays=" + caseworkerDays +
			", effectiveDays=" + effectiveDays +
			", included=" + included +
			", deviationFromDate='" + deviationFromDate + '\'' +
			", deviationToDate='" + deviationToDate + '\'' +
			", normInterval='" + normInterval + '\'' +
			", normRowId=" + normRowId +
			", amount=" + amount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			'}';
	}
}
