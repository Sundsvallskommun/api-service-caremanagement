/**
 * Templating integration module — outbound client to the Sundsvall templating platform (api-service-templating) for
 * rendering stored templates to PDF. A first-class, type-agnostic integration; type modules use it to produce decision
 * documents, payment notices and receipts. {@code TemplatingIntegration} decodes the BASE64 output to PDF bytes.
 */
@ApplicationModule(displayName = "Templating")
package se.sundsvall.caremanagement.templating;

import org.springframework.modulith.ApplicationModule;
