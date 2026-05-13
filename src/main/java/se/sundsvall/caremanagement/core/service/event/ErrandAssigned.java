package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;

public record ErrandAssigned(
	String errandId,
	String typeSlug,
	String municipalityId,
	String namespace,
	String previousAssignee,
	String newAssignee,
	String changedBy,
	OffsetDateTime timestamp)
	implements
	ErrandEvent {}
