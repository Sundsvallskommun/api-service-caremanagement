package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * One person row of the calculation draft, as returned to Draken — a household member (applicant, co-applicant or
 * child). {@code processDays} is the number of days in the home the process derived (read-only); the caseworker's
 * override ({@code caseworkerDays}) and the note are editable. {@code effectiveDays} is what is used = the caseworker
 * value when set, otherwise the process value. Drives the norm base.
 */
@Schema(description = "One person row of the calculation draft (household member, process vs caseworker days).")
public class NormPersonRow {

	@Schema(description = "The row id", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Who created the row: the process or a caseworker", allowableValues = {
		"SYSTEM", "CASEWORKER"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String origin;

	@Schema(description = "Stable 0-based position of the row within its section; assigned on creation and kept across refreshes so the row stays in place", examples = "0", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer position;

	@Schema(description = "The party id of the household member", accessMode = Schema.AccessMode.READ_ONLY)
	private String partyId;

	@Schema(description = "The role of the household member", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String role;

	@Schema(description = "The name of the household member", accessMode = Schema.AccessMode.READ_ONLY)
	private String name;

	@Schema(description = "The number of days in the home the process derived", examples = "30", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer processDays;

	@Schema(description = "The number of days a caseworker decided; overrides the process value when set", examples = "15")
	private Integer caseworkerDays;

	@Schema(description = "The number of days actually used (caseworker value when set, otherwise process value)", accessMode = Schema.AccessMode.READ_ONLY)
	private Integer effectiveDays;

	@Schema(description = "Whether the household member is included in the norm")
	private boolean included;

	@Schema(description = "The start date of the member's deviation from the household")
	@DateTimeFormat(iso = DATE)
	private LocalDate deviationFromDate;

	@Schema(description = "The end date of the member's deviation from the household")
	@DateTimeFormat(iso = DATE)
	private LocalDate deviationToDate;

	@Schema(description = "The norm interval applied to the member")
	private String normInterval;

	@Schema(description = "The job stimulus amount applied to the member", examples = "1000.00")
	private BigDecimal jobStimulusAmount;

	@Schema(description = "Whether the row is soft-deleted (excluded from the calculation, not resurrected by the daily refresh)", accessMode = Schema.AccessMode.READ_ONLY)
	private boolean deleted;

	@Schema(description = "Free-text note")
	private String note;

	@Schema(description = "When the row was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the row was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static NormPersonRow create() {
		return new NormPersonRow();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NormPersonRow withId(final String id) {
		this.id = id;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public NormPersonRow withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(final Integer position) {
		this.position = position;
	}

	public NormPersonRow withPosition(final Integer position) {
		this.position = position;
		return this;
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormPersonRow withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormPersonRow withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormPersonRow withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getProcessDays() {
		return processDays;
	}

	public void setProcessDays(final Integer processDays) {
		this.processDays = processDays;
	}

	public NormPersonRow withProcessDays(final Integer processDays) {
		this.processDays = processDays;
		return this;
	}

	public Integer getCaseworkerDays() {
		return caseworkerDays;
	}

	public void setCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
	}

	public NormPersonRow withCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
		return this;
	}

	public Integer getEffectiveDays() {
		return effectiveDays;
	}

	public void setEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
	}

	public NormPersonRow withEffectiveDays(final Integer effectiveDays) {
		this.effectiveDays = effectiveDays;
		return this;
	}

	public boolean isIncluded() {
		return included;
	}

	public void setIncluded(final boolean included) {
		this.included = included;
	}

	public NormPersonRow withIncluded(final boolean included) {
		this.included = included;
		return this;
	}

	public LocalDate getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormPersonRow withDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public LocalDate getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormPersonRow withDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public NormPersonRow withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public BigDecimal getJobStimulusAmount() {
		return jobStimulusAmount;
	}

	public void setJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
	}

	public NormPersonRow withJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
		return this;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(final boolean deleted) {
		this.deleted = deleted;
	}

	public NormPersonRow withDeleted(final boolean deleted) {
		this.deleted = deleted;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormPersonRow withNote(final String note) {
		this.note = note;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormPersonRow withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormPersonRow withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormPersonRow that = (NormPersonRow) o;
		return deleted == that.deleted && included == that.included && Objects.equals(id, that.id) && Objects.equals(origin, that.origin)
			&& Objects.equals(position, that.position) && Objects.equals(partyId, that.partyId) && Objects.equals(role, that.role) && Objects.equals(name, that.name)
			&& Objects.equals(processDays, that.processDays) && Objects.equals(caseworkerDays, that.caseworkerDays) && Objects.equals(effectiveDays, that.effectiveDays)
			&& Objects.equals(deviationFromDate, that.deviationFromDate) && Objects.equals(deviationToDate, that.deviationToDate)
			&& Objects.equals(normInterval, that.normInterval) && Objects.equals(jobStimulusAmount, that.jobStimulusAmount) && Objects.equals(note, that.note)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, origin, position, partyId, role, name, processDays, caseworkerDays, effectiveDays, included, deviationFromDate, deviationToDate, normInterval,
			jobStimulusAmount, deleted, note, created, updated);
	}

	@Override
	public String toString() {
		return "NormPersonRow{" +
			"id='" + id + '\'' +
			", origin='" + origin + '\'' +
			", position=" + position +
			", partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", processDays=" + processDays +
			", caseworkerDays=" + caseworkerDays +
			", effectiveDays=" + effectiveDays +
			", included=" + included +
			", deviationFromDate=" + deviationFromDate +
			", deviationToDate=" + deviationToDate +
			", normInterval='" + normInterval + '\'' +
			", jobStimulusAmount=" + jobStimulusAmount +
			", deleted=" + deleted +
			", note='" + note + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
