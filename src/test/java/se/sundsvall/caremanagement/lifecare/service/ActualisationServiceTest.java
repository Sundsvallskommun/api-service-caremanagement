package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
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

	@InjectMocks
	private ActualisationService service;

	@Test
	void createFetchesProposalAssemblesAndPosts() {
		final var proposal = new PersonBasedAktualiseringProposalDTO()
			.addActualisationTypesItem(new PersonBasedAktualiseringsInfoDTO().id(3));

		when(lifecareFcIntegrationMock.getActualisationProposal(APPLICANT)).thenReturn(proposal);
		when(lifecareFcIntegrationMock.createActualisation(any(PostAktualiseringsBodyRequest.class))).thenReturn(5012);

		final var actualisationId = service.create(APPLICANT, DATE);

		assertThat(actualisationId).isEqualTo(5012);

		final ArgumentCaptor<PostAktualiseringsBodyRequest> captor = ArgumentCaptor.forClass(PostAktualiseringsBodyRequest.class);
		verify(lifecareFcIntegrationMock).createActualisation(captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getDate()).isEqualTo("2026-06-01");
		assertThat(captor.getValue().getType()).isEqualTo(3);
	}
}
