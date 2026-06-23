/**
 * RPA service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) and the inbound RPA resource can enqueue UiPath Orchestrator
 * tasks through {@link se.sundsvall.caremanagement.rpa.service.RpaService} without reaching into the integration layer
 * or the UiPath DTOs. {@link se.sundsvall.caremanagement.rpa.service.RpaAction} holds the recognised action codes.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.rpa.service;

import org.springframework.modulith.NamedInterface;
