package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaSectionApprovalRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaSectionApprovalEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.types.financialassistance.service.SectionApprovalService.SECTION_CALCULATION;
import static se.sundsvall.caremanagement.types.financialassistance.service.SectionApprovalService.SECTION_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.service.SectionApprovalService.SECTION_PAYMENT;

@ExtendWith(MockitoExtension.class)
class SectionApprovalServiceTest {

	private static final String ERRAND_ID = "errand-1";

	@Mock
	private FaSectionApprovalRepository repositoryMock;

	@InjectMocks
	private SectionApprovalService service;

	private static FaSectionApprovalEntity approved(final String section, final String by) {
		return FaSectionApprovalEntity.create().withId("a-" + section).withErrandId(ERRAND_ID)
			.withSection(section).withApproved(true).withApprovedBy(by).withApprovedAt(OffsetDateTime.parse("2026-06-18T09:00:00Z"));
	}

	@Test
	void approvalsBundlesAllThreeSectionsDefaultingMissingToNotApproved() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(approved(SECTION_CALCULATION, "jane02doe")));

		final var approvals = service.approvals(ERRAND_ID);

		assertThat(approvals.getCalculation().getSection()).isEqualTo(SECTION_CALCULATION);
		assertThat(approvals.getCalculation().isApproved()).isTrue();
		assertThat(approvals.getCalculation().getApprovedBy()).isEqualTo("jane02doe");
		assertThat(approvals.getCalculation().getApprovedAt()).isEqualTo(OffsetDateTime.parse("2026-06-18T09:00:00Z"));

		assertThat(approvals.getPayment().getSection()).isEqualTo(SECTION_PAYMENT);
		assertThat(approvals.getPayment().isApproved()).isFalse();
		assertThat(approvals.getPayment().getApprovedBy()).isNull();

		assertThat(approvals.getDecision().getSection()).isEqualTo(SECTION_DECISION);
		assertThat(approvals.getDecision().isApproved()).isFalse();
	}

	@Test
	void setApprovalCreatesRowAndStampsApprover() {
		when(repositoryMock.findByErrandIdAndSection(ERRAND_ID, SECTION_PAYMENT)).thenReturn(Optional.empty());
		when(repositoryMock.save(any(FaSectionApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.setApproval(ERRAND_ID, SECTION_PAYMENT, true, "jane02doe");

		final var captor = ArgumentCaptor.forClass(FaSectionApprovalEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(saved.getSection()).isEqualTo(SECTION_PAYMENT);
		assertThat(saved.isApproved()).isTrue();
		assertThat(saved.getApprovedBy()).isEqualTo("jane02doe");
		assertThat(saved.getApprovedAt()).isNotNull();

		assertThat(result.getSection()).isEqualTo(SECTION_PAYMENT);
		assertThat(result.isApproved()).isTrue();
		assertThat(result.getApprovedBy()).isEqualTo("jane02doe");
	}

	@Test
	void setApprovalWithdrawalClearsApproverOnExistingRow() {
		when(repositoryMock.findByErrandIdAndSection(ERRAND_ID, SECTION_DECISION)).thenReturn(Optional.of(approved(SECTION_DECISION, "jane02doe")));
		when(repositoryMock.save(any(FaSectionApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.setApproval(ERRAND_ID, SECTION_DECISION, false, "jane02doe");

		final var captor = ArgumentCaptor.forClass(FaSectionApprovalEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.isApproved()).isFalse();
		assertThat(saved.getApprovedBy()).isNull();
		assertThat(saved.getApprovedAt()).isNull();

		assertThat(result.isApproved()).isFalse();
		assertThat(result.getApprovedBy()).isNull();
	}

	@Test
	void setApprovalRejectsUnknownSection() {
		assertThatThrownBy(() -> service.setApproval(ERRAND_ID, "NONSENSE", true, "jane02doe"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: section must be CALCULATION, PAYMENT or DECISION");

		verify(repositoryMock, never()).save(any());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		" "
	})
	void setApprovalRejectsApprovalWithoutApprover(final String approvedBy) {
		assertThatThrownBy(() -> service.setApproval(ERRAND_ID, SECTION_CALCULATION, true, approvedBy))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: a section can only be approved by an identified user - the X-Sent-By header is required");

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void setApprovalWithdrawalNeedsNoApprover() {
		when(repositoryMock.findByErrandIdAndSection(ERRAND_ID, SECTION_DECISION)).thenReturn(Optional.of(approved(SECTION_DECISION, "jane02doe")));
		when(repositoryMock.save(any(FaSectionApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.setApproval(ERRAND_ID, SECTION_DECISION, false, null);

		assertThat(result.isApproved()).isFalse();
		assertThat(result.getApprovedBy()).isNull();
	}
}
