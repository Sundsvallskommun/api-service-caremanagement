package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefilledChild;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class RenewalPrefillServiceTest {

	private static final String APPLICANT = "198001012389";
	private static final String CO_APPLICANT = "198202022397";
	private static final String CHILD = "201801012380";

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	private RenewalPrefillService service() {
		return new RenewalPrefillService(lifecareEbCaseServiceMock);
	}

	@Test
	void prefillsOnlyChildrenExcludingApplicantAndCoApplicant() {
		final var roster = new LifecareEbRoster(APPLICANT, CO_APPLICANT, List.of(
			new LifecareEbRoster.Member(APPLICANT, "Anna Andersson"),
			new LifecareEbRoster.Member(CO_APPLICANT, "Björn Andersson"),
			new LifecareEbRoster.Member(CHILD, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any())).thenReturn(roster);

		final var prefill = service().prefill(APPLICANT);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPersonalNumber, PrefilledChild::getName)
			.containsExactly(tuple(CHILD, "Kid Andersson"));
	}

	@Test
	void singleApplicantPutsRemainingMembersInChildren() {
		final var roster = new LifecareEbRoster(APPLICANT, null, List.of(
			new LifecareEbRoster.Member(APPLICANT, "Anna Andersson"),
			new LifecareEbRoster.Member(CHILD, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any())).thenReturn(roster);

		final var prefill = service().prefill(APPLICANT);

		assertThat(prefill.getChildren()).extracting(PrefilledChild::getPersonalNumber).containsExactly(CHILD);
	}

	@Test
	void emptyRosterYieldsNoChildren() {
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbRoster(APPLICANT, null, List.of()));

		final var prefill = service().prefill(APPLICANT);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren()).isEmpty();
	}

	@Test
	void lifecareFailureDegradesToEmptyResult() {
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare unreachable"));

		final var prefill = service().prefill(APPLICANT);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
	}
}
