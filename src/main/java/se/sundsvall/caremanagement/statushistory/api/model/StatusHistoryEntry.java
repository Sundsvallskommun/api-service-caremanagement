package se.sundsvall.caremanagement.statushistory.api.model;

import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

public record StatusHistoryEntry(
	String id,
	String errandId,
	String fromStatus,
	String toStatus,
	String changedBy,
	@DateTimeFormat(iso = DATE_TIME) OffsetDateTime changedAt) {}
