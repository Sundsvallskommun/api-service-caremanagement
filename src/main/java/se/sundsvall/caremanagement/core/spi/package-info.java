/**
 * Read-side cross-module query facade over the errand envelope, exposed as a named interface so other modules can look
 * an errand up (as the {@link se.sundsvall.caremanagement.core.api.model.Errand} API model) or aggregate errand
 * read-model fields <em>without</em> reaching into core's persistence layer — the {@code ErrandRepository} and the JPA
 * entity are core-internal. Mirrors the {@code conversation.spi} query-service pattern: the entity never crosses the
 * boundary, only API models and small view records do.
 */
@NamedInterface("spi")
package se.sundsvall.caremanagement.core.spi;

import org.springframework.modulith.NamedInterface;
