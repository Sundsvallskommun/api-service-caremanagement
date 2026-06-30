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
	private static final String APPLICANT_PNR = "198001012389";
	private static final String CO_APPLICANT_PNR = "198202022397";
	private static final String CHILD_PNR = "201805054321";
	private static final String CHILD_PARTY_ID = "c0ffee00-0000-4000-8000-000000000001";

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	private RenewalPrefillService service() {
		return new RenewalPrefillService(citizenServiceMock, lifecareEbCaseServiceMock);
	}

	@Test
	void prefillsOnlyChildrenExcludingApplicantAndCoApplicantAndResolvesPartyId() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT_PNR));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD_PNR)).thenReturn(Optional.of(CHILD_PARTY_ID));
		final var roster = new LifecareEbRoster(APPLICANT_PNR, CO_APPLICANT_PNR, List.of(
			new LifecareEbRoster.Member(APPLICANT_PNR, "Anna Andersson"),
			new LifecareEbRoster.Member(CO_APPLICANT_PNR, "Björn Andersson"),
			new LifecareEbRoster.Member(CHILD_PNR, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT_PNR), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPartyId, PrefilledChild::getName)
			.containsExactly(tuple(CHILD_PARTY_ID, "Kid Andersson"));
	}

	@Test
	void childWithUnresolvablePartyIdKeepsNullPartyId() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT_PNR));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD_PNR)).thenReturn(Optional.empty());
		final var roster = new LifecareEbRoster(APPLICANT_PNR, null, List.of(
			new LifecareEbRoster.Member(APPLICANT_PNR, "Anna Andersson"),
			new LifecareEbRoster.Member(CHILD_PNR, "Kid Andersson")));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT_PNR), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPartyId, PrefilledChild::getName)
			.containsExactly(tuple(null, "Kid Andersson"));
	}

	@Test
	void emptyRosterYieldsNoChildren() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT_PNR));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbRoster(APPLICANT_PNR, null, List.of()));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren()).isEmpty();
	}

	@Test
	void unresolvedApplicantPartyIdYieldsEmptyResult() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.empty());

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
		verifyNoInteractions(lifecareEbCaseServiceMock);
	}

	@Test
	void lifecareFailureDegradesToEmptyResult() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(Optional.of(APPLICANT_PNR));
		when(lifecareEbCaseServiceMock.latestRoster(eq(APPLICANT_PNR), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare unreachable"));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
	}
}
