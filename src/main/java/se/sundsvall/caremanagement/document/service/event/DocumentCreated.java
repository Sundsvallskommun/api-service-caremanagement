package se.sundsvall.caremanagement.document.service.event;

import java.time.OffsetDateTime;

public record DocumentCreated(
	String documentId,
	String errandId,
	String municipalityId,
	String namespace,
	String type,
	String createdBy,
	OffsetDateTime timestamp) {}
