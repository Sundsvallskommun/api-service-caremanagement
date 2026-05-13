/**
 * Metadata module — lookup tables (categories, types, contact reasons, etc.) and the
 * public {@code /metadata} endpoints used to drive frontend dropdowns.
 *
 * Storage: {@code LookupEntity} + {@code LookupKind}. Read-mostly; modules that need a
 * name → display-name resolution call into {@code MetadataService}.
 */
@ApplicationModule(displayName = "Metadata")
package se.sundsvall.caremanagement.metadata;

import org.springframework.modulith.ApplicationModule;
