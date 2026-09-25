package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapperTest.SAVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapperTest.tree;

/**
 * Ported from the Draken BFF's errand-lifecare-decision.service.test.ts.
 */
@ExtendWith(MockitoExtension.class)
class LifecareDecisionServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";

	private static final String PROPOSAL = """
		{
		  "decision": {"decisionId": 0, "date": "2026-09-23",
		    "decisionPersons": [{"personId": "19880209T050", "name": "Testsson, Test", "coApplicant": false, "personIdFormatted": "880209-T050"}],
		    "sharedCustody": false},
		  "decisionMakers": [{"id": "TEST", "name": "Test Handläggare", "title": "Testhandläggare"}],
		  "decisionTypes": [
		    {"code": 152, "name": "EK Ekonomiskt bistånd 12 kap 1, 7 §§ SoL, avslag", "type": 10, "isActive": true, "requiresFromDate": false, "requiresToDate": false},
		    {"code": 153, "name": "Ek Ekonomiskt bistånd 12 kap 1, 7 §§ SoL, bifall", "type": 0, "isActive": true, "requiresFromDate": true, "requiresToDate": true}
		  ]
		}""";

	private static final LifecareDecisionSaveRequest BIFALL = new LifecareDecisionSaveRequest(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
		new BigDecimal("3000"), 19, "<p>Beslut</p>", null);

	@Mock
	private LifecareErrandService errandServiceMock;

	@Mock
	private LifecareDecisionClient lifecareMock;

	@Mock
	private LifecareAccessRecorder accessRecorderMock;

	@Mock
	private ProfessionalWebProperties propertiesMock;

	@Captor
	private ArgumentCaptor<JsonNode> bodyCaptor;

	@InjectMocks
	private LifecareDecisionService service;

	private static LifecareErrand errand(final Integer decisionId) {
		return new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, decisionId, null, 2026, 9);
	}

	private LifecareErrand loaded(final Integer decisionId) {
		final var errand = errand(decisionId);
		when(errandServiceMock.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);
		return errand;
	}

	private void caseworker() {
		when(errandServiceMock.caller()).thenReturn("test");
	}

	@Test
	void hasNoBeslutToShowBeforeOneIsSaved() {
		loaded(null);

		assertThat(service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
		verifyNoInteractions(lifecareMock, accessRecorderMock);
	}

	@Test
	void showsTheSavedBeslutAsLifecareHasItAndLogsTheRead() {
		final var errand = loaded(98);
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view).hasValueSatisfying(decision -> {
			assertThat(decision.id()).isEqualTo(98);
			assertThat(decision.outcome()).isEqualTo("BIFALL");
			assertThat(decision.decisionMaker()).isEqualTo("Test Handläggare");
		});
		verify(accessRecorderMock).read(errand, "DECISION", "Läste beslutet i Lifecare", "98");
	}

	@Test
	void servesNoBeslutWhoseReadCouldNotBeLogged() {
		final var errand = loaded(98);
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));
		doThrow(new IllegalStateException("log down")).when(accessRecorderMock).read(errand, "DECISION", "Läste beslutet i Lifecare", "98");

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void createsTheBeslutTheFirstTimeAndLinksTheErrandToIt() {
		final var errand = loaded(null);
		caseworker();
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), bodyCaptor.capture())).thenReturn(tree("{\"decisionId\": 98}"));
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));

		final var view = service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL);

		final var body = bodyCaptor.getValue();
		assertThat(body.path("decisionCode").asInt()).isEqualTo(153);
		assertThat(body.path("reasonCode").asInt()).isEqualTo(19);
		assertThat(body.path("amount").asInt()).isEqualTo(3000);
		assertThat(body.path("decisionMaker").stringValue()).isEqualTo("TEST");
		verify(lifecareMock, never()).update(anyInt(), any());
		verify(errandServiceMock).linkDecision(errand, 98);
		verify(accessRecorderMock).read(errand, "DECISION", "Läste beslutsunderlag i Lifecare");
		verify(accessRecorderMock).written(errand, "CREATE", "DECISION", "Registrerade beslutet i Lifecare", "98");
		assertThat(view.id()).isEqualTo(98);
	}

	@Test
	void namesTheConfiguredTestDecisionMakerInsteadOfTheCaseworker() {
		loaded(null);
		when(propertiesMock.testDecisionMaker()).thenReturn("test");
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), bodyCaptor.capture())).thenReturn(tree("{\"decisionId\": 98}"));
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));

		service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL);

		assertThat(bodyCaptor.getValue().path("decisionMaker").stringValue()).isEqualTo("TEST");
		verify(errandServiceMock, never()).caller();
	}

	@Test
	void changesTheSameBeslutEveryTimeAfterNeverMakingASecondOne() {
		final var errand = loaded(98);
		caseworker();
		final var changed = (ObjectNode) tree(SAVED);
		changed.put("message", "<p>Ändrat</p>");
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));
		when(lifecareMock.update(eq(98), bodyCaptor.capture())).thenReturn(changed);

		final var view = service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareDecisionSaveRequest(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), new BigDecimal("3000"), 19, "<p>Ändrat</p>", false));

		verify(lifecareMock, never()).create(anyInt(), any());
		assertThat(bodyCaptor.getValue().path("decisionId").asInt()).isEqualTo(98);
		assertThat(bodyCaptor.getValue().path("message").stringValue()).isEqualTo("<p>Ändrat</p>");
		assertThat(bodyCaptor.getValue().path("lockedMessage").booleanValue()).isFalse();
		verify(accessRecorderMock).written(errand, "UPDATE", "DECISION", "Ändrade beslutet i Lifecare", "98");
		verify(errandServiceMock, never()).linkDecision(any(), anyInt());
		assertThat(view.message()).isEqualTo("<p>Ändrat</p>");
	}

	@Test
	void savesTheBeslutWriteProtectedWhenAsked() {
		final var errand = loaded(98);
		caseworker();
		final var locked = (ObjectNode) tree(SAVED);
		locked.put("lockedMessage", true);
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));
		when(lifecareMock.update(eq(98), bodyCaptor.capture())).thenReturn(locked);

		final var view = service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareDecisionSaveRequest(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), new BigDecimal("3000"), 19, "<p>Beslut</p>", true));

		assertThat(bodyCaptor.getValue().path("lockedMessage").booleanValue()).isTrue();
		verify(accessRecorderMock).written(errand, "UPDATE", "DECISION", "Ändrade och skrivskyddade beslutet i Lifecare", "98");
		assertThat(view.locked()).isTrue();
	}

	@Test
	void describesAWriteProtectedCreate() {
		final var errand = loaded(null);
		caseworker();
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), any())).thenReturn(tree("{\"decisionId\": 98}"));
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));

		service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareDecisionSaveRequest(153, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), new BigDecimal("3000"), 19, null, true));

		verify(accessRecorderMock).written(errand, "CREATE", "DECISION", "Registrerade och skrivskyddade beslutet i Lifecare", "98");
	}

	@Test
	void handsBackWhyABeslutCannotBeSaved() {
		loaded(null);
		caseworker();
		final var proposal = (ObjectNode) tree(PROPOSAL);
		proposal.putArray("decisionTypes").addObject()
			.put("code", 161).put("name", "EK Återkrav").put("type", 9).put("isActive", true).put("requiresFromDate", false).put("requiresToDate", false);
		when(lifecareMock.readProposal(1)).thenReturn(proposal);

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			new LifecareDecisionSaveRequest(161, null, null, null, null, null, null, null)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("EK Återkrav");
		verify(lifecareMock, never()).create(anyInt(), any());
	}

	@Test
	void refusesAHouseholdCaremKnowsHasACoApplicantBeforeAskingLifecare() {
		final var errand = loaded(null);
		when(errandServiceMock.coApplicantPresent(errand)).thenReturn(true);

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("medsökande");
		verifyNoInteractions(lifecareMock);
	}

	@Test
	void refusesAnErrandWithoutInsats() {
		when(errandServiceMock.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);
		verifyNoInteractions(lifecareMock);
	}

	@Test
	void warnsAgainstSavingAgainWhenTheBeslutWasMadeButTheErrandCouldNotBeLinked() {
		final var errand = loaded(null);
		caseworker();
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), any())).thenReturn(tree("{\"decisionId\": 98}"));
		doThrow(Problem.valueOf(SERVICE_UNAVAILABLE, "db down")).when(errandServiceMock).linkDecision(errand, 98);

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.hasMessageContaining("Spara inte igen")
			.hasMessageContaining("98");
		// The write is logged although the link failed: the beslut exists in Lifecare.
		verify(accessRecorderMock).written(errand, "CREATE", "DECISION", "Registrerade beslutet i Lifecare", "98");
	}

	@Test
	void passesOnLifecaresOwnRefusalOfACreate() {
		loaded(null);
		caseworker();
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), any())).thenThrow(Problem.valueOf(UNPROCESSABLE_CONTENT, "Beslutsdatum saknas"));

		assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Beslutsdatum saknas");
		verify(errandServiceMock, never()).linkDecision(any(), anyInt());
	}

	@Test
	void tellsTheCaseworkerToCheckLifecareWhenACreateWentUnanswered() {
		loaded(null);
		caseworker();
		when(lifecareMock.readProposal(1)).thenReturn(tree(PROPOSAL));
		when(lifecareMock.create(eq(1), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare timed out"))
			.thenThrow(new IllegalStateException("socket closed"))
			.thenReturn(tree("{}"));

		for (var attempt = 0; attempt < 3; attempt++) {
			assertThatThrownBy(() -> service.save(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, BIFALL))
				.isInstanceOf(ThrowableProblem.class)
				.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
				.hasMessageContaining("Kontrollera i Lifecare");
		}
		verify(errandServiceMock, never()).linkDecision(any(), anyInt());
	}

	@Test
	void listsTheBeslutstyperTheInsatsOffers() {
		final var errand = loaded(null);
		final var proposal = (ObjectNode) tree(PROPOSAL);
		proposal.withArrayProperty("decisionTypes").addObject()
			.put("code", 161).put("name", "EK Återkrav").put("type", 9).put("isActive", true).put("requiresFromDate", false).put("requiresToDate", false);
		proposal.withArrayProperty("decisionTypes").addObject()
			.put("code", 9).put("name", "Utgången").put("type", 9).put("isActive", false);
		when(lifecareMock.readProposal(1)).thenReturn(proposal);

		final var types = service.types(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(types).extracting(LifecareDecisionType::code, LifecareDecisionType::outcome)
			.containsExactly(tuple(152, "AVSLAG"), tuple(153, "BIFALL"), tuple(161, null));
		verify(accessRecorderMock).read(errand, "DECISION", "Läste beslutstyper i Lifecare");
	}

	@Test
	void listsTheOrsakerOfABeslutstyp() {
		when(lifecareMock.readReasons(153)).thenReturn(tree("""
			[{"header": "Arbetar deltid, ofrivilligt", "name": "Arbetar deltid, ofrivilligt", "reasonCode": null,
			  "options": [{"header": "", "name": "Arbetar deltid ofrivilligt, otillräcklig inkomst", "reasonCode": 19, "options": []}]}]"""));

		assertThat(service.reasons(153)).isEqualTo(List.of(
			new LifecareDecisionReason(19, "Arbetar deltid ofrivilligt, otillräcklig inkomst", "Arbetar deltid, ofrivilligt")));
		verifyNoInteractions(errandServiceMock, accessRecorderMock);
	}

	@Test
	void hasNoPdfToGiveBeforeABeslutIsSaved() {
		loaded(null);

		assertThatThrownBy(() -> service.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verifyNoInteractions(lifecareMock);
	}

	@Test
	void printsTheSavedBeslutAndLogsTheRead() {
		final var errand = loaded(98);
		final var pdf = "%PDF-1.7".getBytes();
		when(lifecareMock.printDecision(98)).thenReturn(pdf);

		assertThat(service.pdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(pdf);
		verify(accessRecorderMock).read(errand, "DECISION", "Hämtade beslutet som PDF från Lifecare", "98");
	}
}
