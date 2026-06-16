package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefilledChild;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class RenewalPrefillServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String APPLICANT = "198001012389";
	private static final String CO_APPLICANT = "198202022397";
	private static final String CHILD = "201805054321";

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	private RenewalPrefillService service() {
		return new RenewalPrefillService(citizenServiceMock, lifecareEbCaseServiceMock);
	}

	@Test
	void prefillsOnlyChildrenExcludingApplicantAndCoApplicant() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT));
		final var roster = new LifecareEbRoster(APPLICANT, CO_APPLICANT, List.of(
			new LifecareEbRoster.Member(APPLICANT, "Anna Andersson"),
			new LifecareEbRoster.Member(CO_APPLICANT, "Björn Andersson"),
			new LifecareEbRoster.Member(CHILD, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPersonalNumber, PrefilledChild::getName)
			.containsExactly(tuple(CHILD, "Kid Andersson"));
	}

	@Test
	void singleApplicantPutsRemainingMembersInChildren() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT));
		final var roster = new LifecareEbRoster(APPLICANT, null, List.of(
			new LifecareEbRoster.Member(APPLICANT, "Anna Andersson"),
			new LifecareEbRoster.Member(CHILD, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.getChildren()).extracting(PrefilledChild::getPersonalNumber).containsExactly(CHILD);
	}

	@Test
	void unresolvedPartyIdYieldsEmptyResult() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.empty());

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
		verifyNoInteractions(lifecareEbCaseServiceMock);
	}

	@Test
	void citizenFailureDegradesToEmptyResult() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Citizen unreachable"));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
		verifyNoInteractions(lifecareEbCaseServiceMock);
	}

	@Test
	void lifecareFailureDegradesToEmptyResult() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare unreachable"));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
	}
}
