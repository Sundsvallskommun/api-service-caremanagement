/**
 * User settings module — per-user (AD account) preferences for the handläggare-facing frontend, such as whether the
 * SSBTEK view opens in a new window. A user without a stored row gets the defaults.
 */
@ApplicationModule(displayName = "User Settings")
package se.sundsvall.caremanagement.usersettings;

import org.springframework.modulith.ApplicationModule;
