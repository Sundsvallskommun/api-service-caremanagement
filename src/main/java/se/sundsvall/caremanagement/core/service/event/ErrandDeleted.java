package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;

public record ErrandDeleted(
	String errandId,
	String typeSlug,
	String municipalityId,
	String namespace,
	String deletedBy,
	OffsetDateTime timestamp)
	implements
	ErrandEvent {}
