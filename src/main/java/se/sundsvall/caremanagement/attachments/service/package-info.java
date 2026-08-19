/**
 * Attachments service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can attach a citizen's uploaded files to an errand and have them
 * merged into a single combined PDF, through
 * {@link se.sundsvall.caremanagement.attachments.service.AttachmentService}, without reaching into the attachments
 * persistence layer.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.attachments.service;

import org.springframework.modulith.NamedInterface;
