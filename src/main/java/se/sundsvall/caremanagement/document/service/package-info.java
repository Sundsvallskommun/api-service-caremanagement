/**
 * Document service layer.
 *
 * <p>
 * Exposed so the financial assistance supplements ingest can upsert Lifecare document mirrors through
 * {@link se.sundsvall.caremanagement.document.service.DocumentService#mirrorFromLifecare} without reaching into the
 * module's API or persistence layer.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.document.service;

import org.springframework.modulith.NamedInterface;
