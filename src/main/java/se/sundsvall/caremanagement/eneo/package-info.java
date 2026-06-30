/**
 * Eneo (LLM) integration module — outbound client to Sundsvall's Eneo LLM platform for document/decision support:
 * upload a file, ask a named assistant a question (optionally referencing uploaded files), delete the file. A
 * first-class, type-agnostic integration; type modules feed application attachments or basis documents to an assistant
 * and persist the structured answer as decision support (never auto-deciding — routed to the caseworker).
 *
 * <p>
 * The client is built programmatically (dual auth: an {@code api-key} header for the Eneo app + OAuth2
 * client-credentials
 * for the api gateway); {@code EneoIntegration} treats Eneo as a non-blocking dependency (BAD_GATEWAY on failure,
 * best-effort file cleanup).
 * </p>
 */
@ApplicationModule(displayName = "Eneo")
package se.sundsvall.caremanagement.eneo;

import org.springframework.modulith.ApplicationModule;
