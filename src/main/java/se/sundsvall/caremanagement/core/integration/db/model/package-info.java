/**
 * The errand JPA entity. Published as a named interface only so the {@code notifications} module can supply its
 * correlated {@code EXISTS} subquery as a {@code Specification<ErrandEntity>} through the core-owned
 * {@code ErrandNotificationFilter} dependency-inversion seam — a JPA criteria contribution that inherently needs the
 * entity type. No module fetches or mutates the entity through this interface; reads go through {@code core.spi} and
 * writes through {@code core.service}.
 */
@NamedInterface("model")
package se.sundsvall.caremanagement.core.integration.db.model;

import org.springframework.modulith.NamedInterface;
