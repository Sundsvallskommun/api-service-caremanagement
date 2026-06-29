/**
 * Read-only cross-module view of the conversation's file attachments, exposed as a named interface so the attachments
 * module can fold them into the unified errand attachment list and rebuild the consolidated client-attachment PDF —
 * without reaching into the conversation persistence layer. Blobs are fully materialised to {@code byte[]} inside this
 * module's own transaction; no {@code Blob}/stream ever crosses the boundary.
 */
@NamedInterface("spi")
package se.sundsvall.caremanagement.conversation.spi;

import org.springframework.modulith.NamedInterface;
