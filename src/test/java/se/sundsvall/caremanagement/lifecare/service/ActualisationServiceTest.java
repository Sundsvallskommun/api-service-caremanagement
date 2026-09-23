package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;

import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualisationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String APPLICANT = "199001011234";
	private static final LocalDate DATE = LocalDate.of(2026, JUNE, 1);

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@Mock
	private CaseworkerResolver caseworkerResolverMock;

	/** The real record, not a mock: it is configuration, and the assembler reads every field off it. */
	private final ActualisationProperties names = new ActualisationProperties(
		"Ek Återansökan Digital Ekonomiskt bistånd", "Den enskilde", "Ekonomiskt bistånd", "Ekonomiskt bistånd");

	private ActualisationService service;

	@BeforeEach
	void setUp() {
		service = new ActualisationService(lifecareFamilyCareIntegrationMock, caseworkerResolverMock, names);
	}

	@Test
	void createReturnsTheInsatsTheActualisationWasLinkedTo() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3).name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27));
		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE);

		assertThat(result.serviceId()).isEqualTo(7700);
	}

	@Test
	void findFinancialAssistanceServiceIdPicksTheServiceTheActualisationTypeAccepts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3).name("Ek Återansökan Digital Ekonomiskt bistånd")
				.addServiceTypesItem(new PersonBasedAktualiseringsServiceTypeDTO().id(27)))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(5).type(3))
			.addServicesItem(new PersonBasedAktualiseringsServiceDTO().id(7700).type(27));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);

		assertThat(service.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).contains(7700);
	}

	@Test
	void findFinancialAssistanceServiceIdIsEmptyWhenThePersonHasNoOpenEbInsats() {
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(new PersonBasedAktualiseringProposalDTO());

		assertThat(service.findFinancialAssistanceServiceId(MUNICIPALITY_ID, APPLICANT)).isEmpty();
	}

	@Test
	void createResolvesCaseworkerAssemblesAndPosts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.of(new ResolvedCaseworker("9001", "anna01ker", "Anna Andersson")));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isEqualTo("anna01ker");

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getDate()).isEqualTo("2026-06-01T00:00:00");
		assertThat(captor.getValue().getType()).isEqualTo(3);
		assertThat(captor.getValue().getCaseworkerId()).isEqualTo("9001");
	}

	@Test
	void createWithoutResolvableCaseworkerStillPostsWithoutCaseworkerOrAssignee() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void createTreatsCaseworkerResolutionFailureAsBestEffort() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(MUNICIPALITY_ID, APPLICANT, DATE)).thenThrow(new RuntimeException("FamilyCare down"));
		when(lifecareFamilyCareIntegrationMock.getActualisationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal);
		when(lifecareFamilyCareIntegrationMock.createActualisation(eq(MUNICIPALITY_ID), any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.createActualisation(MUNICIPALITY_ID, APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFamilyCareIntegrationMock).createActualisation(eq(MUNICIPALITY_ID), captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void listFormatsDatesMapsResultAndDropsPersonId() {
		final var dto = new PersonBasedAktualiseringDTO()
			.id(5012).type("Ansökan").personId(APPLICANT).name("Ekonomiskt bistånd").date("2026-06-01")
			.reason("Nyansökan").regards("Försörjningsstöd").fromWho("Den enskilde").caseworker("Anna Andersson")
			.organization("IFO").status("Pågående").investigationId(8801).serviceId(7700).decisionId(9900);
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30")))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(dto));

		final var result = service.listActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30));

		assertThat(result).singleElement().satisfies(summary -> {
			assertThat(summary.id()).isEqualTo(5012);
			assertThat(summary.type()).isEqualTo("Ansökan");
			assertThat(summary.name()).isEqualTo("Ekonomiskt bistånd");
			assertThat(summary.date()).isEqualTo("2026-06-01");
			assertThat(summary.reason()).isEqualTo("Nyansökan");
			assertThat(summary.regards()).isEqualTo("Försörjningsstöd");
			assertThat(summary.fromWho()).isEqualTo("Den enskilde");
			assertThat(summary.caseworker()).isEqualTo("Anna Andersson");
			assertThat(summary.organization()).isEqualTo("IFO");
			assertThat(summary.status()).isEqualTo("Pågående");
			assertThat(summary.investigationId()).isEqualTo(8801);
			assertThat(summary.serviceId()).isEqualTo(7700);
			assertThat(summary.decisionId()).isEqualTo(9900);
		});
	}

	@Test
	void listReturnsEmptyWhenFamilyCareHasNoPage() {
		when(lifecareFamilyCareIntegrationMock.getActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30"))).thenReturn(null);

		assertThat(service.listActualisations(MUNICIPALITY_ID, APPLICANT, LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, JUNE, 30))).isEmpty();
	}

	@Test
	void uploadAttachmentSendsPdfToTheActualisation() {
		final var content = new byte[] {
			1, 2, 3
		};

		service.uploadAttachment(MUNICIPALITY_ID, 5012, "EB-26060001_meddelandehistorik.pdf", content, "MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik", "Sundsvalls kommun");

		verify(lifecareFamilyCareIntegrationMock).postActualisationAttachment(MUNICIPALITY_ID, 5012, "MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik",
			"Sundsvalls kommun", "EB-26060001_meddelandehistorik.pdf", content);
	}
}
