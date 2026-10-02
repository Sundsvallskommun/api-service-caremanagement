package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

/**
 * Lifecare's answer as api-service-lifecare-integrator carries it: as data, so the gateway cannot rewrite Lifecare's
 * own
 * status codes or a PDF on the way.
 *
 * @param status      Lifecare's status
 * @param contentType Lifecare's content type
 * @param body        the raw body, base64
 */
public record ProfessionalWebExchangeResponse(int status, String contentType, String body) {
}
