/**
 * The co-caseworker JPA entity. Published as a named interface only so the {@code notifications} module can widen
 * its "notifications visible to this recipient" query and its errand-level unacknowledged/unhandled
 * {@code Specification<ErrandEntity>} filters to also match a user who is a co-caseworker on the errand, not just
 * its direct {@code ownerId} / {@code assignedUserId}. No module fetches or mutates the entity through this
 * interface; reads and writes go through {@code cocaseworkers.service}.
 */
@NamedInterface("model")
package se.sundsvall.caremanagement.cocaseworkers.integration.db.model;

import org.springframework.modulith.NamedInterface;
