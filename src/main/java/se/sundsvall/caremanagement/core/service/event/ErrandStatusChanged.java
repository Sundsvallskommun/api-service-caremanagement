package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;

public record ErrandStatusChanged(
	String errandId,
	String typeSlug,
	String municipalityId,
	String namespace,
	String fromStatus,
	String toStatus,
	String changedBy,
	OffsetDateTime timestamp)
	implements
	ErrandEvent {}
