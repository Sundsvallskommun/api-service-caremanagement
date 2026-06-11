package se.sundsvall.caremanagement.referral.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.referral.api.model.Referral;
import se.sundsvall.caremanagement.referral.integration.db.ReferralRepository;
import se.sundsvall.caremanagement.referral.integration.db.model.ReferralEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private ReferralRepository referralRepositoryMock;

	@InjectMocks
	private ReferralService service;

	private void errandExists() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create()));
	}

	@Test
	void createDefaultsSentAtAndStatus() {
		errandExists();
		when(referralRepositoryMock.save(any(ReferralEntity.class))).thenReturn(ReferralEntity.create().withId("referral-1"));

		final var id = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Referral.create().withAuthority("ENVIRONMENTAL_OFFICE"));

		assertThat(id).isEqualTo("referral-1");
		final ArgumentCaptor<ReferralEntity> captor = ArgumentCaptor.forClass(ReferralEntity.class);
		verify(referralRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(captor.getValue().getSentAt()).isNotNull();
		assertThat(captor.getValue().getStatus()).isEqualTo("SENT");
	}

	@Test
	void createErrandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Referral.create().withAuthority("X")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verify(referralRepositoryMock, never()).save(any());
	}

	@Test
	void readReturnsReferral() {
		errandExists();
		when(referralRepositoryMock.findByErrandIdAndId(ERRAND_ID, "referral-1")).thenReturn(Optional.of(ReferralEntity.create().withId("referral-1").withStatus("SENT")));

		final var referral = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "referral-1");

		assertThat(referral.getId()).isEqualTo("referral-1");
		assertThat(referral.getStatus()).isEqualTo("SENT");
	}

	@Test
	void readNotFound() {
		errandExists();
		when(referralRepositoryMock.findByErrandIdAndId(ERRAND_ID, "missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAllReturnsMappedList() {
		errandExists();
		when(referralRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			ReferralEntity.create().withId("r1"), ReferralEntity.create().withId("r2")));

		final var referrals = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(referrals).extracting(Referral::getId).containsExactly("r1", "r2");
	}

	@Test
	void registerResponseStoresTextAndSetsResponded() {
		errandExists();
		when(referralRepositoryMock.findByErrandIdAndId(ERRAND_ID, "referral-1")).thenReturn(Optional.of(ReferralEntity.create().withId("referral-1").withStatus("SENT")));

		service.registerResponse(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "referral-1", "No objection");

		final ArgumentCaptor<ReferralEntity> captor = ArgumentCaptor.forClass(ReferralEntity.class);
		verify(referralRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getResponseText()).isEqualTo("No objection");
		assertThat(captor.getValue().getStatus()).isEqualTo("RESPONDED");
	}

	@Test
	void deleteRemovesReferral() {
		errandExists();
		final var entity = ReferralEntity.create().withId("referral-1");
		when(referralRepositoryMock.findByErrandIdAndId(ERRAND_ID, "referral-1")).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "referral-1");

		verify(referralRepositoryMock).delete(entity);
	}
}
