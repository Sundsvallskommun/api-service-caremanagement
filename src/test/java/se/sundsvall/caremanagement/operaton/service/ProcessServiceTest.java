package se.sundsvall.caremanagement.operaton.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.ProcessInstanceResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.operaton.integration.OperatonClient;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private OperatonClient operatonClientMock;

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
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void startProcessNullDefinitionsResponseThrowsBadRequest() {
		when(operatonClientMock.getProcessDefinitionsByName(MUNICIPALITY_ID, "X")).thenReturn(null);

		assertThatThrownBy(() -> service.startProcess(MUNICIPALITY_ID, "X", "biz", Map.of()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void correlateMessagePassesVariables() {
		service.correlateMessage(MUNICIPALITY_ID, "msg-1", "biz-1", Map.of("k", "v"));

		final ArgumentCaptor<CorrelationMessageRequest> captor = ArgumentCaptor.forClass(CorrelationMessageRequest.class);
		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getMessageName()).isEqualTo("msg-1");
		assertThat(captor.getValue().getBusinessKey()).isEqualTo("biz-1");
		assertThat(captor.getValue().getProcessVariables()).containsEntry("k", "v");
	}

	@Test
	void correlateMessageNullVariablesDefaultsToEmptyMap() {
		service.correlateMessage(MUNICIPALITY_ID, "msg", "biz", null);

		final ArgumentCaptor<CorrelationMessageRequest> captor = ArgumentCaptor.forClass(CorrelationMessageRequest.class);
		verify(operatonClientMock).correlateMessage(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getProcessVariables()).isEmpty();
	}
}
