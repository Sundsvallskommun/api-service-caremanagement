package se.sundsvall.caremanagement.service.event;

import se.sundsvall.caremanagement.api.model.Notification;

public record NotificationRequestedEvent(
	String municipalityId,
	String namespace,
	String errandId,
	Notification notification) {}
