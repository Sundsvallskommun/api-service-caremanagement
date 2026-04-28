package se.sundsvall.caremanagement.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ModifyVariablesRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.ProcessInstanceResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.operaton.OperatonClient;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String BUSINESS_KEY = "errand-1";
	private static final String PROCESS_INSTANCE_ID = "pi-1";

	@Mock
	private OperatonClient operatonClientMock;

	@Captor
	private ArgumentCaptor<StartProcessInstanceRequest> startCaptor;

	@Captor
	private ArgumentCaptor<ModifyVariablesRequest> modifyCaptor;

	@Captor
	private ArgumentCaptor<CorrelationMessageRequest> correlateCaptor;

	@InjectMocks
	private ProcessService service;

	@Test
	void startProcess_noName_returnsEmpty() {
		final var result = service.startProcess(MUNICIPALITY_ID, null, BUSINESS_KEY, List.of());

		assertThat(result).isEmpty();
		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void startProcess_nameResolved_startsAndReturnsInstanceId() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(new ProcessDefinitionsResponse().addProcessDefinitionsItem(new ProcessDefinitionResponse().id("key:1:1").key("handling").name("Handläggning")));
		when(operatonClientMock.startProcessInstance(eq(MUNICIPALITY_ID), any(StartProcessInstanceRequest.class)))
			.thenReturn(new ProcessInstanceResponse().id(PROCESS_INSTANCE_ID));

		final var parameters = List.of(
			Parameter.create().withKey("k1").withValues(List.of("v1")),
			Parameter.create().withKey("k2").withValues(List.of("a", "b")));

		final var result = service.startProcess(MUNICIPALITY_ID, "Handläggning", BUSINESS_KEY, parameters);

		assertThat(result).contains(PROCESS_INSTANCE_ID);
		verify(operatonClientMock).startProcessInstance(eq(MUNICIPALITY_ID), startCaptor.capture());
		final var request = startCaptor.getValue();
		assertThat(request.getProcessDefinitionKey()).isEqualTo("handling");
		assertThat(request.getBusinessKey()).isEqualTo(BUSINESS_KEY);
		assertThat(request.getVariables())
			.containsEntry("k1", List.of("v1"))
			.containsEntry("k2", List.of("a", "b"));
	}

	@Test
	void startProcess_nameNotFound_throwsBadRequest() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Unknown"))
			.thenReturn(new ProcessDefinitionsResponse());

		assertThatThrownBy(() -> service.startProcess(MUNICIPALITY_ID, "Unknown", BUSINESS_KEY, List.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(operatonClientMock, never()).startProcessInstance(any(), any());
	}

	@Test
	void startProcess_nullResponse_throwsBadRequest() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Unknown"))
			.thenReturn(null);

		assertThatThrownBy(() -> service.startProcess(MUNICIPALITY_ID, "Unknown", BUSINESS_KEY, List.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void startProcess_parametersWithNullKey_filtered() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "Handläggning"))
			.thenReturn(new ProcessDefinitionsResponse().addProcessDefinitionsItem(new ProcessDefinitionResponse().key("handling")));
		when(operatonClientMock.startProcessInstance(eq(MUNICIPALITY_ID), any(StartProcessInstanceRequest.class)))
			.thenReturn(new ProcessInstanceResponse().id(PROCESS_INSTANCE_ID));

		final var parameters = java.util.Arrays.asList(
			Parameter.create().withKey(null).withValues(List.of("v")),
			Parameter.create().withKey("k").withValues(null));

		service.startProcess(MUNICIPALITY_ID, "Handläggning", BUSINESS_KEY, parameters);

		verify(operatonClientMock).startProcessInstance(eq(MUNICIPALITY_ID), startCaptor.capture());
		assertThat(startCaptor.getValue().getVariables())
			.containsOnlyKeys("k")
			.containsEntry("k", List.of());
	}

	@Test
	void updateVariable_noInstance_noOp() {
		service.updateVariable(MUNICIPALITY_ID, null, Parameter.create().withKey("k"));

		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void updateVariable_nullParameter_noOp() {
		service.updateVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID, null);

		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void updateVariable_missingKey_noOp() {
		service.updateVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID, Parameter.create().withValues(List.of("v")));

		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void updateVariable_sendsModification() {
		service.updateVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID,
			Parameter.create().withKey("k").withValues(List.of("a", "b")));

		verify(operatonClientMock).modifyProcessInstanceVariables(eq(MUNICIPALITY_ID), eq(PROCESS_INSTANCE_ID), modifyCaptor.capture());
		assertThat(modifyCaptor.getValue().getModifications()).containsEntry("k", List.of("a", "b"));
		assertThat(modifyCaptor.getValue().getDeletions()).isEmpty();
	}

	@Test
	void updateVariable_nullValues_sendsEmptyList() {
		service.updateVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID, Parameter.create().withKey("k"));

		verify(operatonClientMock).modifyProcessInstanceVariables(eq(MUNICIPALITY_ID), eq(PROCESS_INSTANCE_ID), modifyCaptor.capture());
		assertThat(modifyCaptor.getValue().getModifications()).containsEntry("k", List.of());
	}

	@Test
	void deleteVariable_noInstance_noOp() {
		service.deleteVariable(MUNICIPALITY_ID, null, "k");

		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void deleteVariable_nullKey_noOp() {
		service.deleteVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID, null);

		verifyNoInteractions(operatonClientMock);
	}

	@Test
	void deleteVariable_sendsDeletion() {
		service.deleteVariable(MUNICIPALITY_ID, PROCESS_INSTANCE_ID, "k");

		verify(operatonClientMock).modifyProcessInstanceVariables(eq(MUNICIPALITY_ID), eq(PROCESS_INSTANCE_ID), modifyCaptor.capture());
		assertThat(modifyCaptor.getValue().getDeletions()).containsExactly("k");
	}

	@Test
	void correlateMessage_withVariables() {
		service.correlateMessage(MUNICIPALITY_ID, "PaymentDecisionReceived", BUSINESS_KEY, Map.of("paymentDecision", "APPROVED"));

		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), correlateCaptor.capture());
		final var request = correlateCaptor.getValue();
		assertThat(request.getMessageName()).isEqualTo("PaymentDecisionReceived");
		assertThat(request.getBusinessKey()).isEqualTo(BUSINESS_KEY);
		assertThat(request.getProcessVariables()).containsEntry("paymentDecision", "APPROVED");
	}

	@Test
	void correlateMessage_nullVariables_sendsEmptyMap() {
		service.correlateMessage(MUNICIPALITY_ID, "MessageName", BUSINESS_KEY, null);

		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), correlateCaptor.capture());
		assertThat(correlateCaptor.getValue().getProcessVariables()).isEmpty();
	}
}
