package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;

public record DecisionCreated(
	String decisionId,
	String errandId,
	String municipalityId,
	String namespace,
	String typeSlug,
	String outcome,
	String decidedBy,
	OffsetDateTime timestamp) {}
