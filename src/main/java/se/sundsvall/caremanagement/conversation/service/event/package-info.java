/**
 * Cross-module events published by the conversation module. Exposed as a named interface so other modules (e.g.
 * attachments) may subscribe — mirrors {@code core.service.event}.
 */
@NamedInterface("events")
package se.sundsvall.caremanagement.conversation.service.event;

import org.springframework.modulith.NamedInterface;
