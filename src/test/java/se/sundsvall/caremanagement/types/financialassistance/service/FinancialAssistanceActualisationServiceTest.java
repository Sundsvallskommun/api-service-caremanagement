package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceActualisationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private ActualisationService actualisationServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@InjectMocks
	private FinancialAssistanceActualisationService service;

	@Test
	void createActualisationResolvesPartyDelegatesAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, "anna01ker"));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06");

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(actualisationServiceMock).create("199001011234", LocalDate.of(2026, JUNE, 1));
		// No errandId on the request → nothing recorded on an errand and no assignment.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void createActualisationWithErrandIdRecordsActualisationDecisionAndAssignsCaseworker() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, "anna01ker"));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("ACTUALISATION");
		assertThat(decision.getValue()).isEqualTo("5012");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription()).contains("id 5012");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getAssignedUserId()).isEqualTo("anna01ker");
	}

	@Test
	void createActualisationWithErrandIdButNoResolvedCaseworkerRecordsDecisionWithoutAssigning() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.create("199001011234", LocalDate.of(2026, JUNE, 1))).thenReturn(new ActualisationResult(5012, null));

		final var request = ActualisationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID)
			.withApplicationMonth("2026-06")
			.withErrandId(ERRAND_ID);

		final var response = service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getActualisationId()).isEqualTo(5012);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void createActualisationUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		final var request = ActualisationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		assertThatThrownBy(() -> service.createActualisation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).create(any(), any());
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void listActualisationsResolvesPartyDefaultsPeriodAndMaps() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var summary = new ActualisationSummary(5012, "Ansökan", "Ekonomiskt bistånd", "2026-06-01", "Nyansökan", "Försörjningsstöd",
			"Den enskilde", "Anna Andersson", "IFO", "Pågående", 8801, 7700, 9900);
		when(actualisationServiceMock.list(eq("199001011234"), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(summary));

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null);

		assertThat(result).singleElement().satisfies(actualisation -> {
			assertThat(actualisation.getId()).isEqualTo(5012);
			assertThat(actualisation.getType()).isEqualTo("Ansökan");
			assertThat(actualisation.getName()).isEqualTo("Ekonomiskt bistånd");
			assertThat(actualisation.getDate()).isEqualTo("2026-06-01");
			assertThat(actualisation.getReason()).isEqualTo("Nyansökan");
			assertThat(actualisation.getRegards()).isEqualTo("Försörjningsstöd");
			assertThat(actualisation.getFromWho()).isEqualTo("Den enskilde");
			assertThat(actualisation.getCaseworker()).isEqualTo("Anna Andersson");
			assertThat(actualisation.getOrganization()).isEqualTo("IFO");
			assertThat(actualisation.getStatus()).isEqualTo("Pågående");
			assertThat(actualisation.getInvestigationId()).isEqualTo(8801);
			assertThat(actualisation.getServiceId()).isEqualTo(7700);
			assertThat(actualisation.getDecisionId()).isEqualTo(9900);
		});

		final var fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
		final var toCaptor = ArgumentCaptor.forClass(LocalDate.class);
		verify(actualisationServiceMock).list(eq("199001011234"), fromCaptor.capture(), toCaptor.capture());
		assertThat(toCaptor.getValue()).isEqualTo(LocalDate.now());
		assertThat(fromCaptor.getValue()).isEqualTo(toCaptor.getValue().minusMonths(24));
	}

	@Test
	void listActualisationsUsesExplicitPeriod() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.list("199001011234", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30))).thenReturn(List.of());

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).isEmpty();
		verify(actualisationServiceMock).list("199001011234", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void listActualisationsUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.listActualisations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).list(any(), any(), any());
	}

	private void applicantOwnsActualisation5012() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(actualisationServiceMock.list(eq("199001011234"), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(new ActualisationSummary(5012, "Ansökan", "EB", "2026-06-01", "Nyansökan", "Försörjningsstöd", "Den enskilde", "Anna", "IFO", "Pågående", 8801, 7700, 9900)));
	}

	@Test
	void archiveToActualisationForwardsFileWithDefaultsWhenNoMetadata() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, null);

		verify(actualisationServiceMock).uploadAttachment(5012, "tillaggsansokan.pdf", new byte[] {
			1, 2, 3
		}, "ANSOKAN", "ENSKILD", "tillaggsansokan.pdf", "Draken");
		// No errandId → nothing recorded on an errand.
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationUsesRequestMetadataWhenProvided() {
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			9
		});
		final var request = ArchiveActualisationRequest.create()
			.withTitle("Tilläggsansökan juni")
			.withDocumentType("KOMPLETTERING")
			.withDocumentSenderType("MYNDIGHET")
			.withSenderName("Sundsvalls kommun");
		applicantOwnsActualisation5012();

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, request);

		verify(actualisationServiceMock).uploadAttachment(5012, "tillaggsansokan.pdf", new byte[] {
			9
		}, "KOMPLETTERING", "MYNDIGHET", "Tilläggsansökan juni", "Sundsvalls kommun");
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationRecordsActualisationDecisionWhenErrandIdPresent() {
		final var file = new MockMultipartFile("file", "tillaggsansokan.pdf", "application/pdf", new byte[] {
			7
		});
		final var request = ArchiveActualisationRequest.create().withErrandId(ERRAND_ID);
		applicantOwnsActualisation5012();

		service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, request);

		verify(actualisationServiceMock).uploadAttachment(eq(5012), eq("tillaggsansokan.pdf"), any(), eq("ANSOKAN"), eq("ENSKILD"), eq("tillaggsansokan.pdf"), eq("Draken"));

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("ACTUALISATION");
		assertThat(decision.getValue()).isEqualTo("5012");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription()).contains("id 5012");
	}

	@Test
	void archiveToActualisationWrapsUnreadableFileAs400() throws IOException {
		applicantOwnsActualisation5012();
		final var file = mock(MultipartFile.class);
		when(file.getOriginalFilename()).thenReturn("tillaggsansokan.pdf");
		when(file.getBytes()).thenThrow(new IOException("stream closed"));

		assertThatThrownBy(() -> service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 5012, file, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(actualisationServiceMock, never()).uploadAttachment(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void archiveToActualisationForeignActualisationYields404() {
		applicantOwnsActualisation5012();
		final var file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] {
			1
		});

		assertThatThrownBy(() -> service.archiveToActualisation(MUNICIPALITY_ID, NAMESPACE, APPLICANT_PARTY_ID, 9999, file, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(actualisationServiceMock, never()).uploadAttachment(any(), any(), any(), any(), any(), any(), any());
	}
}
