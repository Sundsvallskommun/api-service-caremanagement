/**
 * Messaging integration module — outbound client to the Sundsvall messaging platform (api-service-messaging) for
 * citizen-facing email. A first-class, type-agnostic integration; type modules use it to notify applicants (decisions,
 * payments, requests for complementary information).
 */
@ApplicationModule(displayName = "Messaging")
package se.sundsvall.caremanagement.messaging;

import org.springframework.modulith.ApplicationModule;
