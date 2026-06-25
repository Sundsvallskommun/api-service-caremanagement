package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualisationServiceTest {

	private static final String APPLICANT = "199001011234";
	private static final LocalDate DATE = LocalDate.of(2026, 6, 1);

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@Mock
	private CaseworkerResolver caseworkerResolverMock;

	@InjectMocks
	private ActualisationService service;

	@Test
	void createResolvesCaseworkerAssemblesAndPosts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(APPLICANT, DATE)).thenReturn(Optional.of(new ResolvedCaseworker("9001", "anna01ker", "Anna Andersson")));
		when(lifecareFcIntegrationMock.getActualisationProposal(APPLICANT)).thenReturn(proposal);
		when(lifecareFcIntegrationMock.createActualisation(any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.create(APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isEqualTo("anna01ker");

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFcIntegrationMock).createActualisation(captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getDate()).isEqualTo("2026-06-01");
		assertThat(captor.getValue().getType()).isEqualTo(3);
		assertThat(captor.getValue().getCaseworkerId()).isEqualTo("9001");
	}

	@Test
	void createWithoutResolvableCaseworkerStillPostsWithoutCaseworkerOrAssignee() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(APPLICANT, DATE)).thenReturn(Optional.empty());
		when(lifecareFcIntegrationMock.getActualisationProposal(APPLICANT)).thenReturn(proposal);
		when(lifecareFcIntegrationMock.createActualisation(any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.create(APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFcIntegrationMock).createActualisation(captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void createTreatsCaseworkerResolutionFailureAsBestEffort() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(caseworkerResolverMock.resolve(APPLICANT, DATE)).thenThrow(new RuntimeException("FC down"));
		when(lifecareFcIntegrationMock.getActualisationProposal(APPLICANT)).thenReturn(proposal);
		when(lifecareFcIntegrationMock.createActualisation(any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var result = service.create(APPLICANT, DATE);

		assertThat(result.actualisationId()).isEqualTo(5012);
		assertThat(result.assignedUserId()).isNull();

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFcIntegrationMock).createActualisation(captor.capture());
		assertThat(captor.getValue().getCaseworkerId()).isNull();
	}

	@Test
	void listFormatsDatesMapsResultAndDropsPersonId() {
		final var dto = new PersonBasedAktualiseringDTO()
			.id(5012).type("Ansökan").personId(APPLICANT).name("Ekonomiskt bistånd").date("2026-06-01")
			.reason("Nyansökan").regards("Försörjningsstöd").fromWho("Den enskilde").caseworker("Anna Andersson")
			.organization("IFO").status("Pågående").investigationId(8801).serviceId(7700).decisionId(9900);
		when(lifecareFcIntegrationMock.getActualisations(APPLICANT, "2026-01-01", "2026-06-30", null, null, false))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(dto));

		final var result = service.list(APPLICANT, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

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
	void listReturnsEmptyWhenFcHasNoPage() {
		when(lifecareFcIntegrationMock.getActualisations(APPLICANT, "2026-01-01", "2026-06-30", null, null, false)).thenReturn(null);

		assertThat(service.list(APPLICANT, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))).isEmpty();
	}

	@Test
	void uploadAttachmentSendsPdfToTheActualisation() {
		final var content = new byte[] {
			1, 2, 3
		};

		service.uploadAttachment(5012, "EB-26060001_meddelandehistorik.pdf", content, "MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik", "Sundsvalls kommun");

		verify(lifecareFcIntegrationMock).postActualisationAttachment(5012, "MEDDELANDEHISTORIK", "MYNDIGHET", "Meddelandehistorik",
			"Sundsvalls kommun", "EB-26060001_meddelandehistorik.pdf", "application/pdf", content);
	}
}
