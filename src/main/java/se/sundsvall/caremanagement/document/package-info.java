/**
 * Document module — the catalogue of Lifecare document types (Typ/Dokumenttyp), served at
 * {@code /documents/metadata}. The Draken template admin tags each document template with one of these codes.
 *
 * <p>
 * The documents themselves live in Lifecare: careM reads and writes them on the errand's insats through ProfessionalWeb
 * (the financial assistance {@code /lifecare/documents} routes) and keeps no copy here.
 * </p>
 *
 * <p>
 * The catalogue is backed by the core metadata lookup store (seeded {@code DOCUMENT_TYPE} lookups for the namespace),
 * with a built-in provisional set as fallback.
 * </p>
 */
@ApplicationModule(displayName = "Documents")
package se.sundsvall.caremanagement.document;

import org.springframework.modulith.ApplicationModule;
