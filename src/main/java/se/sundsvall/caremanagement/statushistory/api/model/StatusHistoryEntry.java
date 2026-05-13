package se.sundsvall.caremanagement.statushistory.api.model;

import java.time.OffsetDateTime;

public record StatusHistoryEntry(
	String id,
	String errandId,
	String fromStatus,
	String toStatus,
	String changedBy,
	OffsetDateTime changedAt) {}
