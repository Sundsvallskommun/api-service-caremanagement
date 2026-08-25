/**
 * Lifecare service layer.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can ask Lifecare FamilyCare domain questions — whether a
 * person has an open financial assistance case, a decision for a given month or a previous calculation — through
 * {@link se.sundsvall.caremanagement.lifecare.service.LifecareCaseService} without reaching into the integration layer
 * or the generated FamilyCare DTOs.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.lifecare.service;

import org.springframework.modulith.NamedInterface;
