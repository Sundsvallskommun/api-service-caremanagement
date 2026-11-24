package se.sundsvall.caremanagement.lifecare.professionalweb.integrator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every call the ProfessionalWeb client makes is one of lifecare-integrator's resources, as its OpenAPI spec in the
 * repo describes them: the method, the path and the parameter names.
 */
class ProfessionalWebIntegratorContractTest {

	private static final String PREFIX = "/{municipalityId}/professional-web/";

	/** Each client method and the resource it calls. */
	private static final Map<String, String> EXPECTED = Map.ofEntries(
		Map.entry("listReminders", "GET services/{serviceId}/reminders"),
		Map.entry("readReminderProposal", "GET services/{serviceId}/reminders/proposal"),
		Map.entry("readReminder", "GET reminders/{reminderId}"),
		Map.entry("createReminder", "POST reminders"),
		Map.entry("updateReminder", "PUT reminders/{reminderId}"),
		Map.entry("removeReminder", "DELETE reminders/{reminderId}"),
		Map.entry("readJobStimulus", "GET services/{serviceId}/job-stimulus"),
		Map.entry("readJobStimulusEndDate", "GET job-stimulus/end-date"),
		Map.entry("saveJobStimulus", "POST job-stimulus"),
		Map.entry("readDecisionProposal", "GET services/{serviceId}/decisions/proposal"),
		Map.entry("readDecisionReasons", "GET decision-codes/{decisionCode}/reasons"),
		Map.entry("readDecision", "GET decisions/{decisionId}"),
		Map.entry("createDecision", "POST services/{serviceId}/decisions"),
		Map.entry("updateDecision", "PUT decisions/{decisionId}"),
		Map.entry("readPaymentProposal", "GET services/{serviceId}/payments/proposal"),
		Map.entry("readLatestPayments", "GET services/{serviceId}/payments/latest"),
		Map.entry("hasHousehold", "GET services/{serviceId}/household"),
		Map.entry("createPayment", "POST services/{serviceId}/payments"),
		Map.entry("createPayee", "POST payees"),
		Map.entry("readCalculationProposal", "GET services/{serviceId}/calculations/proposal"),
		Map.entry("listLatestCalculations", "GET services/{serviceId}/calculations/latest"),
		Map.entry("readCalculation", "GET calculations/{calculationId}"),
		Map.entry("readCalculationForEdit", "GET calculations/{calculationId}/edit"),
		Map.entry("placePersons", "POST calculations/place-persons"),
		Map.entry("readAmount", "POST calculations/amount"),
		Map.entry("readSharedCost", "POST calculations/shared-cost"),
		Map.entry("readJobStimuliForCalculation", "POST calculations/job-stimuli"),
		Map.entry("createCalculation", "POST services/{serviceId}/calculations"),
		Map.entry("updateCalculation", "PUT calculations/{calculationId}"),
		Map.entry("deleteCalculation", "DELETE calculations/{calculationId}"),
		Map.entry("listDocuments", "GET persons/{partyId}/documents"),
		Map.entry("readJournalNote", "GET journal-notes/{id}"),
		Map.entry("readJournalNoteForEdit", "GET journal-notes/{id}/edit"),
		Map.entry("readDocument", "GET documents/{id}"),
		Map.entry("readDocumentForEdit", "GET documents/{id}/edit"),
		Map.entry("readJournalNoteProposal", "GET services/{serviceId}/journal-notes/proposal"),
		Map.entry("readDocumentProposal", "GET services/{serviceId}/documents/proposal"),
		Map.entry("createJournalNote", "POST journal-notes"),
		Map.entry("createDocument", "POST documents"),
		Map.entry("updateJournalNote", "PUT journal-notes/{id}"),
		Map.entry("updateDocument", "PUT documents/{id}"),
		Map.entry("printDecision", "GET decisions/{decisionId}/pdf"),
		Map.entry("printDocument", "GET documents/{id}/pdf"),
		Map.entry("printCalculation", "GET calculations/{calculationId}/pdf"));

	private static Map<String, Object> paths;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void readSpec() throws IOException {
		try (var spec = new ClassPathResource("integrations/lifecare-integrator-api.yaml").getInputStream()) {
			paths = (Map<String, Object>) new Yaml().<Map<String, Object>>load(spec).get("paths");
		}
	}

	@Test
	void eachMethodCallsItsResource() {
		final var actual = new TreeMap<String, String>();
		resourceMethods().forEach(method -> actual.put(method.getName(), verb(method) + " " + path(method).substring(PREFIX.length())));

		assertThat(actual).isEqualTo(new TreeMap<>(EXPECTED));
	}

	@Test
	void thePrintsAskForAPdf() {
		resourceMethods().stream()
			.filter(method -> method.getName().startsWith("print"))
			.forEach(method -> {
				assertThat(method.getAnnotation(GetMapping.class).produces()).as(method.getName()).containsExactly("application/pdf");
				assertThat(method.getReturnType()).as(method.getName()).isEqualTo(byte[].class);
			});
	}

	@Test
	void eachResourceAndItsParametersAreInTheIntegratorsSpec() {
		resourceMethods().forEach(method -> {
			final var operation = operation(method);
			assertThat(operation).as(method.getName()).isNotNull();
			final var specParameters = parameters(operation);
			for (final var parameter : method.getParameters()) {
				parameterName(parameter, PathVariable.class).ifPresent(name -> assertThat(specParameters).as(method.getName()).containsEntry(name, "path"));
				parameterName(parameter, RequestParam.class).ifPresent(name -> assertThat(specParameters).as(method.getName()).containsEntry(name, "query"));
			}
		});
	}

	private static List<Method> resourceMethods() {
		return Arrays.stream(ProfessionalWebIntegratorClient.class.getDeclaredMethods()).toList();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> operation(final Method method) {
		final var path = (Map<String, Object>) paths.get(path(method));
		if (path == null) {
			return null;
		}
		return (Map<String, Object>) path.get(verb(method).toLowerCase());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> parameters(final Map<String, Object> operation) {
		final var result = new TreeMap<String, String>();
		((List<Map<String, Object>>) operation.getOrDefault("parameters", List.of()))
			.forEach(parameter -> result.put((String) parameter.get("name"), (String) parameter.get("in")));
		return result;
	}

	private static java.util.Optional<String> parameterName(final Parameter parameter, final Class<? extends java.lang.annotation.Annotation> type) {
		if (!parameter.isAnnotationPresent(type)) {
			return java.util.Optional.empty();
		}
		final String value;
		if (type == PathVariable.class) {
			value = parameter.getAnnotation(PathVariable.class).value();
		} else {
			value = parameter.getAnnotation(RequestParam.class).value();
		}
		if (value.isEmpty()) {
			return java.util.Optional.of(parameter.getName());
		}
		return java.util.Optional.of(value);
	}

	private static String verb(final Method method) {
		if (method.isAnnotationPresent(GetMapping.class)) {
			return "GET";
		}
		if (method.isAnnotationPresent(PostMapping.class)) {
			return "POST";
		}
		if (method.isAnnotationPresent(PutMapping.class)) {
			return "PUT";
		}
		if (method.isAnnotationPresent(DeleteMapping.class)) {
			return "DELETE";
		}
		throw new IllegalStateException(method.getName() + " has no mapping");
	}

	private static String path(final Method method) {
		return switch (verb(method)) {
			case "GET" -> method.getAnnotation(GetMapping.class).path()[0];
			case "POST" -> method.getAnnotation(PostMapping.class).path()[0];
			case "PUT" -> method.getAnnotation(PutMapping.class).path()[0];
			default -> method.getAnnotation(DeleteMapping.class).path()[0];
		};
	}
}
