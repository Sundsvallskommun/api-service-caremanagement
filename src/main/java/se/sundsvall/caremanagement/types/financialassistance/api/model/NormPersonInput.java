package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * What a caseworker sends to add a new person row (origin CASEWORKER) or patch an existing one — only the caseworker
 * days + note are honoured on a patch.
 */
@Schema(description = "What a caseworker sends to add or patch a person row (identity + caseworker-writable fields only).")
public class NormPersonInput {

	@Schema(description = "The party id of the household member")
	private String partyId;

	@Schema(description = "The role of the household member", allowableValues = {
		"APPLICANT", "CO_APPLICANT", "CHILD"
	})
	private String role;

	@Schema(description = "The name of the household member")
	private String name;

	@Schema(description = "The number of days the caseworker decided", examples = "15")
	private Integer caseworkerDays;

	@Schema(description = "Whether the household member is included in the norm")
	private Boolean included;

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

	@Schema(description = "Free-text note")
	private String note;

	public static NormPersonInput create() {
		return new NormPersonInput();
	}

	public String getPartyId() {
		return partyId;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public NormPersonInput withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public NormPersonInput withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public NormPersonInput withName(final String name) {
		this.name = name;
		return this;
	}

	public Integer getCaseworkerDays() {
		return caseworkerDays;
	}

	public void setCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
	}

	public NormPersonInput withCaseworkerDays(final Integer caseworkerDays) {
		this.caseworkerDays = caseworkerDays;
		return this;
	}

	public Boolean getIncluded() {
		return included;
	}

	public void setIncluded(final Boolean included) {
		this.included = included;
	}

	public NormPersonInput withIncluded(final Boolean included) {
		this.included = included;
		return this;
	}

	public LocalDate getDeviationFromDate() {
		return deviationFromDate;
	}

	public void setDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
	}

	public NormPersonInput withDeviationFromDate(final LocalDate deviationFromDate) {
		this.deviationFromDate = deviationFromDate;
		return this;
	}

	public LocalDate getDeviationToDate() {
		return deviationToDate;
	}

	public void setDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
	}

	public NormPersonInput withDeviationToDate(final LocalDate deviationToDate) {
		this.deviationToDate = deviationToDate;
		return this;
	}

	public String getNormInterval() {
		return normInterval;
	}

	public void setNormInterval(final String normInterval) {
		this.normInterval = normInterval;
	}

	public NormPersonInput withNormInterval(final String normInterval) {
		this.normInterval = normInterval;
		return this;
	}

	public BigDecimal getJobStimulusAmount() {
		return jobStimulusAmount;
	}

	public void setJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
	}

	public NormPersonInput withJobStimulusAmount(final BigDecimal jobStimulusAmount) {
		this.jobStimulusAmount = jobStimulusAmount;
		return this;
	}

	public String getNote() {
		return note;
	}

	public void setNote(final String note) {
		this.note = note;
	}

	public NormPersonInput withNote(final String note) {
		this.note = note;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormPersonInput that = (NormPersonInput) o;
		return Objects.equals(partyId, that.partyId) && Objects.equals(role, that.role) && Objects.equals(name, that.name)
			&& Objects.equals(caseworkerDays, that.caseworkerDays) && Objects.equals(included, that.included) && Objects.equals(deviationFromDate, that.deviationFromDate)
			&& Objects.equals(deviationToDate, that.deviationToDate) && Objects.equals(normInterval, that.normInterval)
			&& Objects.equals(jobStimulusAmount, that.jobStimulusAmount) && Objects.equals(note, that.note);
	}

	@Override
	public int hashCode() {
		return Objects.hash(partyId, role, name, caseworkerDays, included, deviationFromDate, deviationToDate, normInterval, jobStimulusAmount, note);
	}

	@Override
	public String toString() {
		return "NormPersonInput{" +
			"partyId='" + partyId + '\'' +
			", role='" + role + '\'' +
			", name='" + name + '\'' +
			", caseworkerDays=" + caseworkerDays +
			", included=" + included +
			", deviationFromDate=" + deviationFromDate +
			", deviationToDate=" + deviationToDate +
			", normInterval='" + normInterval + '\'' +
			", jobStimulusAmount=" + jobStimulusAmount +
			", note='" + note + '\'' +
			'}';
	}
}
