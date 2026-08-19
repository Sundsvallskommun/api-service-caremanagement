package se.sundsvall.caremanagement.shared;

/**
 * Cross-module event: any module can publish this to request a notification.
 * The notifications module subscribes and handles persistence + dispatch.
 *
 * All fields are plain strings — producers don't need to know about notification-specific
 * enums or DTOs.
 */
public record NotificationRequest(
	String municipalityId,
	String namespace,
	String errandId,
	String ownerId,
	String createdBy,
	String type,
	String subType,
	String description) {}
