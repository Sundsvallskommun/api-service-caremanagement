/**
 * Shared infrastructure usable by any module: auditing support, common base types.
 *
 * Declared as a Modulith shared module in {@link
 * se.sundsvall.caremanagement.Application @Modulithic(sharedModules = "shared")} so any module
 * can depend on it without listing it explicitly in {@code allowedDependencies}.
 */
@ApplicationModule(displayName = "Shared")
package se.sundsvall.caremanagement.shared;

import org.springframework.modulith.ApplicationModule;
