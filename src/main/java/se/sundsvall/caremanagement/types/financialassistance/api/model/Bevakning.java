package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * An EB bevakning on an errand — a date-bound watch/reminder the handläggare manages in Draken. Unlike the income
 * warnings it has no acknowledge lifecycle: it is created, edited and removed directly, and carries a start date plus
 * an
 * optional end date.
 */
@Schema(description = "An EB bevakning (date-bound watch/reminder) on an errand.")
public class Bevakning {

	@Schema(description = "The bevakning id", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Short headline for the bevakning", examples = "Följ upp inkomstuppgifter från CSN")
	private String title;

	@Schema(description = "Free-text details of what to watch for", examples = "Inväntar kompletterande underlag innan beslut kan fattas.")
	private String description;

	@Schema(description = "When the watch becomes relevant (bevakningsdatum)", examples = "2026-07-01")
	private LocalDate startDate;

	@Schema(description = "When the watch ends — open-ended when omitted", examples = "2026-07-31")
	private LocalDate endDate;

	@Schema(description = "The handläggare who created the bevakning", examples = "joe01doe")
	private String createdBy;

	@Schema(description = "When the bevakning was created", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "When the bevakning was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime updated;

	public static Bevakning create() {
		return new Bevakning();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Bevakning withId(final String id) {
		this.id = id;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Bevakning withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Bevakning withDescription(final String description) {
		this.description = description;
		return this;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	public Bevakning withStartDate(final LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	public Bevakning withEndDate(final LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Bevakning withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Bevakning withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public Bevakning withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Bevakning that = (Bevakning) o;
		return Objects.equals(id, that.id) && Objects.equals(title, that.title) && Objects.equals(description, that.description)
			&& Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate) && Objects.equals(createdBy, that.createdBy)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, title, description, startDate, endDate, createdBy, created, updated);
	}

	@Override
	public String toString() {
		return "Bevakning{" +
			"id='" + id + '\'' +
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
