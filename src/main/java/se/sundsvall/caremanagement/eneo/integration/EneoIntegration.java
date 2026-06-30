package se.sundsvall.caremanagement.eneo.integration;

import generated.se.sundsvall.eneo.AskAssistant;
import generated.se.sundsvall.eneo.AskResponse;
import generated.se.sundsvall.eneo.FilePublic;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Thin wrapper over the {@link EneoClient}. Translates any transport/Eneo failure into a {@code BAD_GATEWAY} problem
 * (so
 * callers can treat Eneo as a non-blocking decision-support dependency) while carrying the upstream status/message into
 * the log and the problem detail for diagnosis.
 */
@Component
public class EneoIntegration {

	public static final String CLIENT_ID = "eneo";

	private static final Logger LOG = LoggerFactory.getLogger(EneoIntegration.class);

	private final EneoClient eneoClient;

	public EneoIntegration(final EneoClient eneoClient) {
		this.eneoClient = eneoClient;
	}

	public AskResponse askAssistant(final UUID assistantId, final AskAssistant request) {
		try {
			LOG.debug("Asking Eneo assistant {} with {} file(s)", assistantId, ofNullable(request.getFiles()).map(List::size).orElse(0));
			return eneoClient.askAssistant(assistantId, request);
		} catch (final Exception e) {
			LOG.error("Error asking Eneo assistant {}: {}", assistantId, describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error asking Eneo assistant %s: %s".formatted(assistantId, describe(e)));
		}
	}

	public FilePublic uploadFile(final MultipartFile file) {
		try {
			LOG.debug("Uploading file '{}' to Eneo", sanitizeForLogging(file.getOriginalFilename()));
			return eneoClient.uploadFile(file).getBody();
		} catch (final Exception e) {
			LOG.error("Error uploading file '{}' to Eneo: {}", sanitizeForLogging(file.getOriginalFilename()), describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error uploading file to Eneo: %s".formatted(describe(e)));
		}
	}

	public void deleteFile(final UUID fileId) {
		try {
			eneoClient.deleteFile(fileId);
		} catch (final Exception e) {
			// Best-effort cleanup — log and swallow so a failed delete never fails the validation result.
			LOG.warn("Error deleting Eneo file {}: {}", fileId, describe(e));
		}
	}

	/** Short upstream descriptor (HTTP status when available) to make failures self-diagnosing. */
	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName() + ": " + e.getMessage();
	}
}
