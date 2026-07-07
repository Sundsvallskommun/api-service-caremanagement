/**
 * Core-internal persistence layer for the errand envelope — the {@code ErrandRepository} and errand-number sequence
 * repository. Deliberately <em>not</em> a named interface: other modules must not depend on core's repositories. They
 * go through the read-side {@code core.spi} query facade, the write-side {@code core.service} {@code ErrandService}, or
 * the {@code shared} {@code ErrandAccessGuard} existence port instead.
 */
package se.sundsvall.caremanagement.core.integration.db;
