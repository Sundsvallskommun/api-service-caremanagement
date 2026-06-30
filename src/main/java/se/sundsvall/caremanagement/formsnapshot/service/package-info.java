/**
 * Form-snapshot service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can capture a form snapshot when their errand is created, and
 * read it back for re-display, through
 * {@link se.sundsvall.caremanagement.formsnapshot.service.FormSnapshotService}, without reaching into the form-snapshot
 * persistence layer.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.formsnapshot.service;

import org.springframework.modulith.NamedInterface;
