package se.sundsvall.caremanagement.eneo.integration;

import generated.se.sundsvall.eneo.AskAssistant;
import generated.se.sundsvall.eneo.AskResponse;
import generated.se.sundsvall.eneo.FilePublic;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static se.sundsvall.caremanagement.eneo.integration.EneoIntegration.CLIENT_ID;

/**
 * Feign contract for the subset of the Eneo (Sundsvall LLM platform) API used for LLM document/decision support: upload
 * a file, ask an assistant about the uploaded files, and delete the file afterwards. The client is built
 * programmatically
 * in {@link se.sundsvall.caremanagement.eneo.integration.configuration.EneoConfiguration} (api-key header + OAuth2),
 * hence the url is supplied there rather than via {@code @FeignClient}.
 */
@CircuitBreaker(name = CLIENT_ID)
public interface EneoClient {

	@PostMapping(value = "/assistants/{assistantId}/sessions/", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	AskResponse askAssistant(@PathVariable UUID assistantId, @RequestBody AskAssistant request);

	@PostMapping(value = "/files/", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<FilePublic> uploadFile(@RequestPart(name = "upload_file") MultipartFile file);

	@DeleteMapping("/files/{fileId}/")
	ResponseEntity<Void> deleteFile(@PathVariable UUID fileId);
}
