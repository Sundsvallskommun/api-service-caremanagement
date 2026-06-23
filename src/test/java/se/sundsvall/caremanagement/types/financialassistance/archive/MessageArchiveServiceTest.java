package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;
import se.sundsvall.caremanagement.conversation.spi.ConversationThreadQueryService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageArchiveServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String CLOSED = "CLOSED";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String ERRAND_NUMBER = "EB-26060001";

	private static final MessageArchiveProperties PROPERTIES = new MessageArchiveProperties(
		MUNICIPALITY_ID, NAMESPACE, 30, "MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik", "Sundsvalls kommun");

	@Mock
	private ErrandService errandServiceMock;
	@Mock
	private ConversationThreadQueryService conversationThreadQueryServiceMock;
	@Mock
	private DecisionService decisionServiceMock;
	@Mock
	private AttachmentService attachmentServiceMock;
	@Mock
	private ActualisationService actualisationServiceMock;

	private MessageArchiveService service;

	@BeforeEach
	void setUp() {
		service = new MessageArchiveService(errandServiceMock, conversationThreadQueryServiceMock, decisionServiceMock,
			attachmentServiceMock, actualisationServiceMock, PROPERTIES);
	}

	private static Errand errand() {
		return Errand.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
	}

	private static List<ConversationMessageView> thread() {
		return List.of(new ConversationMessageView("INBOUND", "Hej, jag bifogar mitt intyg.", "joe01doe", OffsetDateTime.now(), List.of("intyg.pdf")));
	}

	private static Decision actualisationDecision(final String value) {
		return Decision.create().withDecisionType("ACTUALISATION").withValue(value);
	}

	@Test
	void archivesEligibleErrand() {
		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(List.of(errand()));
		when(attachmentServiceMock.messageHistoryExists(ERRAND_ID)).thenReturn(false);
		when(conversationThreadQueryServiceMock.threadForErrand(ERRAND_ID)).thenReturn(thread());
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(actualisationDecision("5012")));

		service.archiveClosedErrands();

		verify(actualisationServiceMock).uploadAttachment(eq(5012), eq(ERRAND_NUMBER + "_meddelandehistorik.pdf"), any(byte[].class),
			eq("MEDDELANDEHISTORIK"), eq("MYNDIGHET"), eq("Meddelandehistorik"), eq("Sundsvalls kommun"));
		verify(attachmentServiceMock).createMessageHistoryAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(byte[].class));
	}

	@Test
	void skipsAlreadyArchivedErrand() {
		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(List.of(errand()));
		when(attachmentServiceMock.messageHistoryExists(ERRAND_ID)).thenReturn(true);

		service.archiveClosedErrands();

		verifyNoInteractions(conversationThreadQueryServiceMock, decisionServiceMock, actualisationServiceMock);
		verify(attachmentServiceMock, never()).createMessageHistoryAttachment(any(), any(), any(), any());
	}

	@Test
	void skipsErrandWithoutConversation() {
		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(List.of(errand()));
		when(attachmentServiceMock.messageHistoryExists(ERRAND_ID)).thenReturn(false);
		when(conversationThreadQueryServiceMock.threadForErrand(ERRAND_ID)).thenReturn(emptyList());

		service.archiveClosedErrands();

		verifyNoInteractions(decisionServiceMock, actualisationServiceMock);
		verify(attachmentServiceMock, never()).createMessageHistoryAttachment(any(), any(), any(), any());
	}

	@Test
	void skipsErrandWithoutActualisationId() {
		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(List.of(errand()));
		when(attachmentServiceMock.messageHistoryExists(ERRAND_ID)).thenReturn(false);
		when(conversationThreadQueryServiceMock.threadForErrand(ERRAND_ID)).thenReturn(thread());
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("PAYMENT").withValue("999"),
			actualisationDecision("not-a-number")));

		service.archiveClosedErrands();

		verifyNoInteractions(actualisationServiceMock);
		verify(attachmentServiceMock, never()).createMessageHistoryAttachment(any(), any(), any(), any());
	}

	@Test
	void continuesAfterFailureOnOneErrand() {
		final var failing = Errand.create().withId("22222222-2222-2222-2222-222222222222").withErrandNumber("EB-26060002")
			.withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);

		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(List.of(failing, errand()));
		when(attachmentServiceMock.messageHistoryExists(any())).thenReturn(false);
		when(conversationThreadQueryServiceMock.threadForErrand(any())).thenReturn(thread());
		when(decisionServiceMock.readAll(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(List.of(actualisationDecision("5012")));
		doThrow(new RuntimeException("Lifecare down")).when(actualisationServiceMock)
			.uploadAttachment(any(), eq("EB-26060002_meddelandehistorik.pdf"), any(), any(), any(), any(), any());

		service.archiveClosedErrands();

		// The second (healthy) errand is still archived despite the first throwing.
		verify(attachmentServiceMock).createMessageHistoryAttachment(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(byte[].class));
		verify(actualisationServiceMock, times(2)).uploadAttachment(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void noEligibleErrandsIsANoOp() {
		when(errandServiceMock.findByStatusTouchedBefore(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(CLOSED), any())).thenReturn(emptyList());

		service.archiveClosedErrands();

		verifyNoInteractions(conversationThreadQueryServiceMock, decisionServiceMock, actualisationServiceMock, attachmentServiceMock);
	}
}
