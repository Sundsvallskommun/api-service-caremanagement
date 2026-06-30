package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * An EB monitoring on an errand — a date-bound watch/reminder the caseworker manages in Draken. Unlike the income
 * warnings it has no acknowledge lifecycle: it is created, edited and removed directly, and carries a start date plus
 * an
 * optional end date.
 */
@Schema(description = "An EB monitoring (date-bound watch/reminder) on an errand.")
public class Monitoring {

	@Schema(description = "The monitoring id", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Provenance: CASEWORKER for one authored in Draken (RPA mirrors it onto the person in Lifecare), "
		+ "LIFECARE for one read out of Lifecare by RPA and surfaced here on the errand.", examples = "CASEWORKER", allowableValues = {
			"CASEWORKER", "LIFECARE"
	})
	private String source;

	@Schema(description = "The monitoring's id in Lifecare once it exists there — null until RPA has mirrored a caseworker-authored "
		+ "monitoring; always set for a LIFECARE-sourced one.", examples = "987654")
	private String lifecareId;

	@Schema(description = "Short headline for the monitoring", examples = "Follow up income details from CSN")
	private String title;

	@Schema(description = "Free-text details of what to watch for", examples = "Awaiting supplementary documentation before a decision can be made.")
	private String description;

	@Schema(description = "When the watch becomes relevant (monitoringsdatum)", examples = "2026-07-01")
	private LocalDate startDate;

	@Schema(description = "When the watch ends — open-ended when omitted", examples = "2026-07-31")
	private LocalDate endDate;

	@Schema(description = "The caseworker who created the monitoring", examples = "joe01doe")
	private String createdBy;

	@Schema(description = "When the monitoring was created", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "When the monitoring was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime updated;

	public static Monitoring create() {
		return new Monitoring();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Monitoring withId(final String id) {
		this.id = id;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public Monitoring withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public Monitoring withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Monitoring withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Monitoring withDescription(final String description) {
		this.description = description;
		return this;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	public Monitoring withStartDate(final LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	public Monitoring withEndDate(final LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Monitoring withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Monitoring withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public Monitoring withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Monitoring that = (Monitoring) o;
		return Objects.equals(id, that.id) && Objects.equals(source, that.source) && Objects.equals(lifecareId, that.lifecareId)
			&& Objects.equals(title, that.title) && Objects.equals(description, that.description)
			&& Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate) && Objects.equals(createdBy, that.createdBy)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, source, lifecareId, title, description, startDate, endDate, createdBy, created, updated);
	}

	@Override
	public String toString() {
		return "Monitoring{" +
			"id='" + id + '\'' +
			", source='" + source + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", title='" + title + '\'' +
			", description='" + description + '\'' +
			", startDate=" + startDate +
			", endDate=" + endDate +
			", createdBy='" + createdBy + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
