package se.sundsvall.caremanagement.types.financialassistance.service;

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
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.service.FormSnapshotService;
import se.sundsvall.caremanagement.stakeholders.api.model.ContactChannel;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Person;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private StakeholderService stakeholderServiceMock;

	@Mock
	private AttachmentService attachmentServiceMock;

	@Mock
	private FormSnapshotService formSnapshotServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

	@InjectMocks
	private FinancialAssistanceErrandService service;

	@Test
	void createBuildsEnvelopeAndSavesData() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		// Client sends a mismatched applicationType — the slug must win.
		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Min application")
			.withData(FinancialAssistanceData.create().withApplicationType("SUPPLEMENTARY"));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, null);

		assertThat(result).isEqualTo(ERRAND_ID);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_NEW);
		assertThat(errandCaptor.getValue().getStatus()).isEqualTo(STATUS_RECEIVED);
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Min application");

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("NEW"); // derived from SLUG_NEW, not the client value

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any()); // no attachments supplied
		verify(stakeholderServiceMock, never()).create(any(), any(), any(), any()); // no persons → no stakeholders
	}

	@Test
	void createSavesStakeholdersFromPersons() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withTitle("Renewal")
			.withData(FinancialAssistanceData.create().withPersons(List.of(
				Person.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID).withEmail("anna@example.com").withPhone("+46701234567"),
				Person.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID),
				Person.create().withEmail("noone@example.com")))); // no role → skipped

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, null, null, null);

		final ArgumentCaptor<Stakeholder> stakeholderCaptor = ArgumentCaptor.forClass(Stakeholder.class);
		verify(stakeholderServiceMock, times(2)).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), stakeholderCaptor.capture());

		final var applicant = stakeholderCaptor.getAllValues().getFirst();
		assertThat(applicant.getRole()).isEqualTo("APPLICANT");
		assertThat(applicant.getExternalId()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(applicant.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(applicant.getContactChannels()).extracting(ContactChannel::getKey, ContactChannel::getValue)
			.containsExactly(tuple("EMAIL", "anna@example.com"), tuple("PHONE", "+46701234567"));

		final var coApplicant = stakeholderCaptor.getAllValues().get(1);
		assertThat(coApplicant.getRole()).isEqualTo("CO_APPLICANT");
		assertThat(coApplicant.getExternalId()).isEqualTo(CO_APPLICANT_PARTY_ID);
		assertThat(coApplicant.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(coApplicant.getContactChannels()).isEmpty();
	}

	@Test
	void createDerivesApplicationTypeFromSlug() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Renewal").withData(FinancialAssistanceData.create());

		// Empty attachment list must be treated the same as none — no combine.
		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_RENEWAL, request, List.of(), null, null);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTypeSlug()).isEqualTo(SLUG_RENEWAL);

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");

		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any());
	}

	@Test
	void createDefaultsTitleWhenMissing() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create()
			.withData(FinancialAssistanceData.create().withApplicationType("NEW"));

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, null);

		final ArgumentCaptor<Errand> errandCaptor = ArgumentCaptor.forClass(Errand.class);
		verify(errandServiceMock).createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), errandCaptor.capture());
		assertThat(errandCaptor.getValue().getTitle()).isEqualTo("Financial assistance");
	}

	@Test
	void createWithAttachmentsStoresThemAndCombines() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med bilagor").withData(FinancialAssistanceData.create());
		final List<MultipartFile> attachments = List.of(
			new MockMultipartFile("attachments", "hyreskontrakt.pdf", "application/pdf", "%PDF-1.4".getBytes()),
			new MockMultipartFile("attachments", "hyresavi.png", "image/png", new byte[] {
				1, 2, 3
			}));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, attachments, null, null);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(repositoryMock).save(any(FinancialAssistanceEntity.class));
		verify(attachmentServiceMock).storeAndCombine(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, attachments);
	}

	@Test
	void createWithCaseDataStoresSnapshot() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med ärendeuppgifter").withData(FinancialAssistanceData.create());
		final var caseData = new MockMultipartFile("caseData", "snapshot.pdf", "application/pdf", "%PDF-1.4".getBytes());

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, caseData, null);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(attachmentServiceMock).createCaseDataAttachment(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, caseData);
		verify(attachmentServiceMock, never()).storeAndCombine(any(), any(), any(), any()); // no supporting files supplied
		verifyNoInteractions(formSnapshotServiceMock); // no form snapshot supplied
	}

	@Test
	void createWithFormSnapshotCapturesIt() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withTitle("Med formulärsnapshot").withData(FinancialAssistanceData.create());
		final var payload = "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}";

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, payload);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(formSnapshotServiceMock).saveErrandFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, SLUG_NEW, payload);
	}

	@Test
	void createWithBlankFormSnapshotSkipsCapture() {
		when(errandServiceMock.createTypedErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), any(Errand.class))).thenReturn(ERRAND_ID);

		final var request = CreateFinancialAssistanceRequest.create().withData(FinancialAssistanceData.create());

		service.create(MUNICIPALITY_ID, NAMESPACE, SLUG_NEW, request, null, null, "  ");

		verifyNoInteractions(formSnapshotServiceMock); // blank payload → no capture
	}

	@Test
	void readFormSnapshotScopesAndDelegates() {
		final var snapshot = FormSnapshot.create().withSchemaVersion("form-snapshot/1");
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
		when(formSnapshotServiceMock.readErrandFormSnapshot(ERRAND_ID)).thenReturn(snapshot);

		final var result = service.readFormSnapshot(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).isSameAs(snapshot);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID); // scope check
		verify(formSnapshotServiceMock).readErrandFormSnapshot(ERRAND_ID);
	}

	@Test
	void readAssemblesView() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("NEW")));
		final var approvals = SectionApprovals.create().withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(true));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals);

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNotNull();
		assertThat(view.getData().getApplicationType()).isEqualTo("NEW");
		assertThat(view.getSectionApprovals()).isSameAs(approvals);
	}

	@Test
	void readPopulatesRecommendationFromLatestRecommendationDecision() {
		final var recommendation = Decision.create().withDecisionType("RECOMMENDATION").withValue("OK");
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withStatus("RECEIVED"));
		when(repositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withApplicationType("NEW")));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(recommendation, Decision.create().withDecisionType("ACTUALISATION").withValue("4711")));

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getRecommendation()).isEqualTo(recommendation);
	}

	@Test
	void readWithoutDataReturnsViewWithNullData() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		final var view = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(view.getId()).isEqualTo(ERRAND_ID);
		assertThat(view.getData()).isNull();
	}

	@Test
	void readPropagatesNotFound() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "x"));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void updateDataSavesWhenErrandExists() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID));

		service.updateData(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			FinancialAssistanceData.create().withApplicationType("RENEWAL"));

		final ArgumentCaptor<FinancialAssistanceEntity> entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getApplicationType()).isEqualTo("RENEWAL");
	}
}
