package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Request to create or replace an EB bevakning on an errand. The same body is used for create (POST) and update (PUT);
 * the bevakning has no acknowledge lifecycle, so every mutable field is supplied each time.
 */
@Schema(description = "Request to create or replace an EB bevakning on an errand.")
public class BevakningRequest {

	@Schema(description = "Short headline for the bevakning", examples = "Följ upp inkomstuppgifter från CSN", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String title;

	@Schema(description = "Free-text details of what to watch for", examples = "Inväntar kompletterande underlag innan beslut kan fattas.")
	private String description;

	@Schema(description = "When the watch becomes relevant (bevakningsdatum)", examples = "2026-07-01", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@DateTimeFormat(iso = DATE)
	private LocalDate startDate;

	@Schema(description = "When the watch ends — open-ended when omitted. Must not be before the start date.", examples = "2026-07-31")
	@DateTimeFormat(iso = DATE)
	private LocalDate endDate;

	@Schema(description = "The handläggare who created the bevakning", examples = "joe01doe")
	private String createdBy;

	public static BevakningRequest create() {
		return new BevakningRequest();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public BevakningRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public BevakningRequest withDescription(final String description) {
		this.description = description;
		return this;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(final LocalDate startDate) {
		this.startDate = startDate;
	}

	public BevakningRequest withStartDate(final LocalDate startDate) {
		this.startDate = startDate;
		return this;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(final LocalDate endDate) {
		this.endDate = endDate;
	}

	public BevakningRequest withEndDate(final LocalDate endDate) {
		this.endDate = endDate;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public BevakningRequest withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final BevakningRequest that = (BevakningRequest) o;
		return Objects.equals(title, that.title) && Objects.equals(description, that.description) && Objects.equals(startDate, that.startDate)
			&& Objects.equals(endDate, that.endDate) && Objects.equals(createdBy, that.createdBy);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, startDate, endDate, createdBy);
	}

	@Override
	public String toString() {
		return "BevakningRequest{" +
			"title='" + title + '\'' +
			", description='" + description + '\'' +
			", startDate=" + startDate +
			", endDate=" + endDate +
			", createdBy='" + createdBy + '\'' +
			'}';
	}
}
