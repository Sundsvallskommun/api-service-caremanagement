package se.sundsvall.caremanagement.operaton.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.ProcessInstanceResponse;
import generated.se.sundsvall.operaton.ProcessInstancesResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.caremanagement.operaton.integration.OperatonClient;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;
import se.sundsvall.caremanagement.operaton.integration.model.EvaluateDecisionRequest;
import se.sundsvall.caremanagement.operaton.integration.model.EvaluateDecisionResponse;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";

	@Mock
	private OperatonClient operatonClientMock;

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private ProcessMessageRetryRepository processMessageRetryRepositoryMock;

	@InjectMocks
	private ProcessService service;

	@Test
	void startProcessReturnsInstanceId() {
		final var definitionsResponse = new ProcessDefinitionsResponse()
			.processDefinitions(List.of(new ProcessDefinitionResponse().key("the-key")));
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(definitionsResponse);
		when(operatonClientMock.startProcessInstance(eq(MUNICIPALITY_ID), any(StartProcessInstanceRequest.class)))
			.thenReturn(new ProcessInstanceResponse().id("pi-1"));

		final var result = service.startProcess(MUNICIPALITY_ID, "Handläggning", "biz-1", Map.of("k", "v"));

		assertThat(result).contains("pi-1");

		final ArgumentCaptor<StartProcessInstanceRequest> captor = ArgumentCaptor.forClass(StartProcessInstanceRequest.class);
		verify(operatonClientMock).startProcessInstance(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getProcessDefinitionKey()).isEqualTo("the-key");
		assertThat(captor.getValue().getBusinessKey()).isEqualTo("biz-1");
		assertThat(captor.getValue().getVariables()).containsEntry("k", "v");
	}

	@Test
	void startProcessWithNullDefinitionReturnsEmpty() {
		assertThat(service.startProcess(MUNICIPALITY_ID, null, "biz", Map.of())).isEmpty();
		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void startProcessWithNullVariablesSendsEmptyMap() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "DefName"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of(new ProcessDefinitionResponse().key("k"))));
		when(operatonClientMock.startProcessInstance(eq(MUNICIPALITY_ID), any(StartProcessInstanceRequest.class)))
			.thenReturn(new ProcessInstanceResponse().id("pi"));

		service.startProcess(MUNICIPALITY_ID, "DefName", "biz", null);

		final ArgumentCaptor<StartProcessInstanceRequest> captor = ArgumentCaptor.forClass(StartProcessInstanceRequest.class);
		verify(operatonClientMock).startProcessInstance(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getVariables()).isEmpty();
	}

	@Test
	void startProcessUnknownDefinitionThrowsBadRequest() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Unknown"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of()));

		assertThatThrownBy(() -> service.startProcess(MUNICIPALITY_ID, "Unknown", "biz", Map.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: No Operaton process definition found with name 'Unknown'");
	}

	@Test
	void startProcessNullDefinitionsResponseThrowsBadRequest() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "X")).thenReturn(null);

		assertThatThrownBy(() -> service.startProcess(MUNICIPALITY_ID, "X", "biz", Map.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: No Operaton process definition found with name 'X'");
	}

	@Test
	void correlateMessagePassesVariables() {
		service.correlateMessage(MUNICIPALITY_ID, NAMESPACE, "msg-1", "biz-1", Map.of("k", "v"));

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, "biz-1");
		final ArgumentCaptor<CorrelationMessageRequest> captor = ArgumentCaptor.forClass(CorrelationMessageRequest.class);
		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getMessageName()).isEqualTo("msg-1");
		assertThat(captor.getValue().getBusinessKey()).isEqualTo("biz-1");
		assertThat(captor.getValue().getProcessVariables()).containsEntry("k", "v");
	}

	@Test
	void correlateMessageNullVariablesDefaultsToEmptyMap() {
		service.correlateMessage(MUNICIPALITY_ID, NAMESPACE, "msg", "biz", null);

		final ArgumentCaptor<CorrelationMessageRequest> captor = ArgumentCaptor.forClass(CorrelationMessageRequest.class);
		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getProcessVariables()).isEmpty();
	}

	@Test
	void evaluateDecisionReturnsResultRows() {
		when(operatonClientMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_x"), any(EvaluateDecisionRequest.class)))
			.thenReturn(new EvaluateDecisionResponse(List.of(Map.of("out", "v"))));

		assertThat(service.evaluateDecision(MUNICIPALITY_ID, "Decision_x", Map.of("in", 1))).containsExactly(Map.of("out", "v"));

		final ArgumentCaptor<EvaluateDecisionRequest> captor = ArgumentCaptor.forClass(EvaluateDecisionRequest.class);
		verify(operatonClientMock).evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_x"), captor.capture());
		assertThat(captor.getValue().variables()).containsEntry("in", 1);
	}

	@Test
	void evaluateDecisionReturnsEmptyWhenResponseNullOrNoVariables() {
		when(operatonClientMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_x"), any(EvaluateDecisionRequest.class))).thenReturn(null);

		assertThat(service.evaluateDecision(MUNICIPALITY_ID, "Decision_x", null)).isEmpty();

		final ArgumentCaptor<EvaluateDecisionRequest> captor = ArgumentCaptor.forClass(EvaluateDecisionRequest.class);
		verify(operatonClientMock).evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_x"), captor.capture());
		assertThat(captor.getValue().variables()).isEmpty();
	}

	@Test
	void anEngineAnswerOfNothingWaitingIsNothingToEndForTheWithdrawalMessage() {
		assertThat(ProcessService.isNothingToEnd(ProcessService.MESSAGE_ERRAND_WITHDRAWN, Problem.valueOf(NOT_FOUND, "No process instance is waiting"))).isTrue();
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {
		"BAD_GATEWAY", "SERVICE_UNAVAILABLE", "BAD_REQUEST", "INTERNAL_SERVER_ERROR"
	})
	void anyOtherEngineFailureIsStillAFailureForTheWithdrawalMessage(final HttpStatus status) {
		assertThat(ProcessService.isNothingToEnd(ProcessService.MESSAGE_ERRAND_WITHDRAWN, Problem.valueOf(status, "engine"))).isFalse();
	}

	@Test
	void aFailureThatIsNoProblemAtAllIsStillAFailureForTheWithdrawalMessage() {
		assertThat(ProcessService.isNothingToEnd(ProcessService.MESSAGE_ERRAND_WITHDRAWN, new IllegalStateException("connection reset"))).isFalse();
		assertThat(ProcessService.isNothingToEnd(ProcessService.MESSAGE_ERRAND_WITHDRAWN, null)).isFalse();
	}

	@Test
	void aNotFoundForAnyOtherMessageIsStillAFailureBecauseTheProcessMayNotHaveGotThereYet() {
		assertThat(ProcessService.isNothingToEnd("PaymentDecisionReceived", Problem.valueOf(NOT_FOUND, "No process instance is waiting"))).isFalse();
		assertThat(ProcessService.isNothingToEnd(null, Problem.valueOf(NOT_FOUND, "No process instance is waiting"))).isFalse();
	}

	@Test
	void queueMessageRetryStoresAPendingRowDueInAMinute() {
		service.queueMessageRetry(MUNICIPALITY_ID, NAMESPACE, "PaymentDecisionReceived", "errand-1", Map.of("paymentDecision", "APPROVED"), "engine down");

		final var captor = ArgumentCaptor.forClass(ProcessMessageRetryEntity.class);
		verify(processMessageRetryRepositoryMock).save(captor.capture());
		final var retry = captor.getValue();
		assertThat(retry.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(retry.getNamespace()).isEqualTo(NAMESPACE);
		assertThat(retry.getErrandId()).isEqualTo("errand-1");
		assertThat(retry.getMessageName()).isEqualTo("PaymentDecisionReceived");
		assertThat(retry.getStatus()).isEqualTo("PENDING");
		assertThat(retry.getAttempts()).isEqualTo(1);
		assertThat(retry.getLastError()).isEqualTo("engine down");
		assertThat(retry.getCreated()).isCloseTo(OffsetDateTime.now(), within(5, SECONDS));
		assertThat(retry.getNextAttempt()).isCloseTo(retry.getCreated().plusMinutes(1), within(1, SECONDS));
		assertThat(ProcessService.variablesOf(retry)).isEqualTo(Map.of("paymentDecision", "APPROVED"));
	}

	@Test
	void queueMessageRetryWithoutVariablesStoresAnEmptyObject() {
		service.queueMessageRetry(MUNICIPALITY_ID, NAMESPACE, "M", "errand-1", null, null);

		final var captor = ArgumentCaptor.forClass(ProcessMessageRetryEntity.class);
		verify(processMessageRetryRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getVariables()).isEqualTo("{}");
		assertThat(captor.getValue().getLastError()).isNull();
	}

	@Test
	void variablesOfARowWithoutVariablesIsEmpty() {
		assertThat(ProcessService.variablesOf(ProcessMessageRetryEntity.create())).isEmpty();
	}

	@Test
	void truncateCutsLongErrorsToTheColumn() {
		assertThat(ProcessService.truncate("x".repeat(2000))).hasSize(1024);
		assertThat(ProcessService.truncate("short")).isEqualTo("short");
		assertThat(ProcessService.truncate(null)).isNull();
	}

	@Test
	void activeBusinessKeysAreThoseOfTheDefinitionsRunningInstances() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of(new ProcessDefinitionResponse().key("the-key"))));
		when(operatonClientMock.getProcessInstances(MUNICIPALITY_ID)).thenReturn(new ProcessInstancesResponse().processInstances(List.of(
			new ProcessInstanceResponse().id("pi-1").processDefinitionId("the-key:3:abc").businessKey("errand-1"),
			// an older version of the same definition still counts
			new ProcessInstanceResponse().id("pi-2").processDefinitionId("the-key:1:def").businessKey("errand-2"),
			// another process on the same errand does not
			new ProcessInstanceResponse().id("pi-3").processDefinitionId("another-key:1:ghi").businessKey("errand-3"),
			// a key that merely starts with ours is another definition
			new ProcessInstanceResponse().id("pi-4").processDefinitionId("the-key-2:1:jkl").businessKey("errand-4"),
			new ProcessInstanceResponse().id("pi-5").processDefinitionId("the-key:3:mno"),
			new ProcessInstanceResponse().id("pi-6").businessKey("errand-6"))));

		final var result = service.activeBusinessKeys(MUNICIPALITY_ID, "Handläggning");

		assertThat(result).containsExactlyInAnyOrder("errand-1", "errand-2");
		verify(operatonClientMock).getProcessInstances(MUNICIPALITY_ID);
	}

	@Test
	void activeBusinessKeysEmptyWhenNothingIsRunning() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of(new ProcessDefinitionResponse().key("the-key"))));
		when(operatonClientMock.getProcessInstances(MUNICIPALITY_ID)).thenReturn(new ProcessInstancesResponse());

		assertThat(service.activeBusinessKeys(MUNICIPALITY_ID, "Handläggning")).isEqualTo(Set.of());
	}

	@Test
	void activeBusinessKeysEmptyWhenTheEngineAnswersNothing() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of(new ProcessDefinitionResponse().key("the-key"))));
		when(operatonClientMock.getProcessInstances(MUNICIPALITY_ID)).thenReturn(null);

		assertThat(service.activeBusinessKeys(MUNICIPALITY_ID, "Handläggning")).isEmpty();
	}

	@Test
	void activeBusinessKeysUnknownDefinitionThrowsBadRequestWithoutListingInstances() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Unknown"))
			.thenReturn(new ProcessDefinitionsResponse().processDefinitions(List.of()));

		assertThatThrownBy(() -> service.activeBusinessKeys(MUNICIPALITY_ID, "Unknown"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: No Operaton process definition found with name 'Unknown'");
		verify(operatonClientMock, never()).getProcessInstances(any());
	}
}
