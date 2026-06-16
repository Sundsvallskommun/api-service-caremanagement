/**
 * Lifecare service-layer domain models.
 *
 * <p>
 * Exposed so type modules (e.g. financial assistance) can consume what the lifecare service layer returns — the
 * normberäkning outcome ({@code NormberakningResult}) and the SSBTEK income warnings it carries
 * ({@code SsbtekChangeWarning}, {@code UnhandledIncome}) — without reaching into the integration layer or the generated
 * FC DTOs.
 * </p>
 */
@NamedInterface("model")
package se.sundsvall.caremanagement.lifecare.service.model;

import org.springframework.modulith.NamedInterface;
