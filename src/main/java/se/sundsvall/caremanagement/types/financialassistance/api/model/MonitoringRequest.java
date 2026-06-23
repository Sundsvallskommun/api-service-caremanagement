package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Request to create or replace an EB monitoring on an errand. The same body is used for create (POST) and update (PUT);
 * the monitoring has no acknowledge lifecycle, so every mutable field is supplied each time.
 */
@Schema(description = "Request to create or replace an EB monitoring on an errand.")
public class MonitoringRequest {

	@Schema(description = "Provenance, defaults to CASEWORKER when omitted. RPA POSTs LIFECARE (with lifecareId) to surface a "
		+ "monitoring read out of Lifecare onto the errand.", examples = "CASEWORKER", allowableValues = {
			"CASEWORKER", "LIFECARE"
	})
	@OneOf(value = {
		"CASEWORKER", "LIFECARE"
	}, nullable = true)
	private String source;

	@Schema(description = "The monitoring's id in Lifecare. Set by RPA when surfacing a LIFECARE-sourced monitoring (the idempotency "
		+ "key) or when stamping back the id of a mirrored caseworker monitoring.", examples = "987654")
	private String lifecareId;

	@Schema(description = "Short headline for the monitoring", examples = "Follow up income details from CSN", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String title;

	@Schema(description = "Free-text details of what to watch for", examples = "Awaiting supplementary documentation before a decision can be made.")
	private String description;

	@Schema(description = "When the watch becomes relevant (monitoringsdatum)", examples = "2026-07-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@DateTimeFormat(iso = DATE)
	private LocalDate startDate;

	@Schema(description = "When the watch ends — open-ended when omitted. Must not be before the start date.", examples = "2026-07-31")
	@DateTimeFormat(iso = DATE)
	private LocalDate endDate;

	@Schema(description = "The caseworker who created the monitoring", examples = "joe01doe")
	private String createdBy;

	public static MonitoringRequest create() {
		return new MonitoringRequest();
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public MonitoringRequest withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public MonitoringRequest withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public MonitoringRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public MonitoringRequest withDescription(final String description) {
		this.description = description;
		return this;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	public MonitoringRequest withStartDate(final LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	public MonitoringRequest withEndDate(final LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public MonitoringRequest withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MonitoringRequest that = (MonitoringRequest) o;
		return Objects.equals(source, that.source) && Objects.equals(lifecareId, that.lifecareId) && Objects.equals(title, that.title)
			&& Objects.equals(description, that.description) && Objects.equals(startDate, that.startDate)
			&& Objects.equals(endDate, that.endDate) && Objects.equals(createdBy, that.createdBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, lifecareId, title, description, startDate, endDate, createdBy);
	}

	@Override
	public String toString() {
		return "MonitoringRequest{" +
			"source='" + source + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", startDate=" + startDate +
			", endDate=" + endDate +
			", createdBy='" + createdBy + '\'' +
			'}';
	}
}
