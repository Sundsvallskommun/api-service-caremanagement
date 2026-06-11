package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;

public record DecisionRecorded(
	String decisionId,
	String errandId,
	String typeSlug,
	String outcome,
	String decidedBy,
	OffsetDateTime timestamp) {}
