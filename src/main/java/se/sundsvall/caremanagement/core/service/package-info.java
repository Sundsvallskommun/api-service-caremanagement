/**
 * Exposed so type modules can create and read errand envelopes via {@link ErrandService}
 * (per the modulith migration plan — type modules depend directly on the concrete service).
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.core.service;

import org.springframework.modulith.NamedInterface;
