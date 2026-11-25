package se.sundsvall.caremanagement.usersettings.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.service.UserSettingsService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping("/{municipalityId}/user-settings/{adAccount}")
@Tag(name = "User settings", description = "Per-user (AD account) settings")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class UserSettingsResource {

	private static final String AD_ACCOUNT_REGEXP = "^[A-Za-z0-9._-]{1,64}$";
	private static final String AD_ACCOUNT_VALIDATION_MESSAGE = "must be 1-64 characters of A-Z, a-z, 0-9, '.', '_' and '-'";

	private final UserSettingsService service;

	UserSettingsResource(final UserSettingsService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read user settings", description = "Returns the defaults for a user who has never saved any settings — never 404.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<UserSettings> readUserSettings(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "adAccount", example = "joe01doe") @Pattern(regexp = AD_ACCOUNT_REGEXP, message = AD_ACCOUNT_VALIDATION_MESSAGE) @PathVariable final String adAccount) {

		return ok(service.read(municipalityId, adAccount));
	}

	@PutMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create or replace user settings", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> saveUserSettings(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "adAccount", example = "joe01doe") @Pattern(regexp = AD_ACCOUNT_REGEXP, message = AD_ACCOUNT_VALIDATION_MESSAGE) @PathVariable final String adAccount,
		@Valid @NotNull @RequestBody final UserSettings settings) {

		service.save(municipalityId, adAccount, settings);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(produces = ALL_VALUE)
	@Operation(summary = "Reset user settings to the defaults", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> deleteUserSettings(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "adAccount", example = "joe01doe") @Pattern(regexp = AD_ACCOUNT_REGEXP, message = AD_ACCOUNT_VALIDATION_MESSAGE) @PathVariable final String adAccount) {

		service.delete(municipalityId, adAccount);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
