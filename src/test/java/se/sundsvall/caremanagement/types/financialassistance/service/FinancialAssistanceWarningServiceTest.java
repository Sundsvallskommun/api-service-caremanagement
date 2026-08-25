package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceWarningServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@InjectMocks
	private FinancialAssistanceWarningService service;

	@Test
	void createWarningScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var warning = Warning.create().withId("w-1").withType("NEW_INCOME").withStatus("OPEN");
		when(warningServiceMock.create(ERRAND_ID, "NEW_INCOME", "src", "msg")).thenReturn(warning);

		final var request = CreateWarningRequest.create().withType("NEW_INCOME").withSourceKey("src").withMessage("msg");
		assertThat(service.createWarning(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).isSameAs(warning);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(warningServiceMock).create(ERRAND_ID, "NEW_INCOME", "src", "msg");
	}

	@Test
	void listWarningsScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var warnings = List.of(Warning.create().withId("w-1").withType("NEW_INCOME").withStatus("OPEN"));
		when(warningServiceMock.list(ERRAND_ID)).thenReturn(warnings);

		assertThat(service.listWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(warnings);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void countActiveWarningsScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		when(warningServiceMock.countActive(ERRAND_ID)).thenReturn(3L);

		assertThat(service.countActiveWarnings(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(3L);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(warningServiceMock).countActive(ERRAND_ID);
	}

	@Test
	void updateWarningScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var warning = Warning.create().withId("w-1").withStatus("ACKNOWLEDGED");
		when(warningServiceMock.updateStatus(ERRAND_ID, "w-1", "ACKNOWLEDGED")).thenReturn(warning);

		assertThat(service.updateWarning(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "w-1", "ACKNOWLEDGED")).isSameAs(warning);
		verify(warningServiceMock).updateStatus(ERRAND_ID, "w-1", "ACKNOWLEDGED");
	}
}
