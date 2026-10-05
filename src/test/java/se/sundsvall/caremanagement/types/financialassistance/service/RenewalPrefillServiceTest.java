package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefilledChild;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class RenewalPrefillServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "c0ffee00-0000-4000-8000-000000000002";
	private static final String CHILD_PARTY_ID = "c0ffee00-0000-4000-8000-000000000001";
	private static final String OTHER_CHILD_PARTY_ID = "c0ffee00-0000-4000-8000-000000000003";

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	private RenewalPrefillService service() {
		return new RenewalPrefillService(lifecareCaseServiceMock);
	}

	@Test
	void prefillsOnlyChildrenExcludingApplicantAndCoApplicant() {
		final var roster = new LifecareRoster(PARTY_ID, CO_APPLICANT_PARTY_ID, List.of(
			new LifecareRoster.Member(PARTY_ID, "Anna Andersson"),
			new LifecareRoster.Member(CO_APPLICANT_PARTY_ID, "Björn Andersson"),
			new LifecareRoster.Member(CHILD_PARTY_ID, "Kid Andersson")));
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPartyId, PrefilledChild::getName)
			.containsExactly(tuple(CHILD_PARTY_ID, "Kid Andersson"));
	}

	/** The roster resolves the party ids; an unresolvable one arrives as null and the child keeps only its name. */
	@Test
	void childWithUnresolvablePartyIdKeepsNullPartyId() {
		final var roster = new LifecareRoster(PARTY_ID, null, List.of(
			new LifecareRoster.Member(PARTY_ID, "Anna Andersson"),
			new LifecareRoster.Member(null, "Kid Andersson")));
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any())).thenReturn(roster);

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.getChildren())
			.extracting(PrefilledChild::getPartyId, PrefilledChild::getName)
			.containsExactly(tuple(null, "Kid Andersson"));
	}

	/**
	 * Two unidentifiable people are not the same person. Comparing null to null would drop the child from the form
	 * whenever the applicant could not be resolved either — a silent loss the citizen has no way to notice.
	 */
	@Test
	void anUnidentifiableChildIsNotMistakenForAnUnidentifiableApplicant() {
		final var roster = new LifecareRoster(null, null, List.of(
			new LifecareRoster.Member(null, "Kid Andersson"),
			new LifecareRoster.Member(OTHER_CHILD_PARTY_ID, "Kid Two")));
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any())).thenReturn(roster);

		assertThat(service().prefill(MUNICIPALITY_ID, PARTY_ID).getChildren())
			.extracting(PrefilledChild::getPartyId, PrefilledChild::getName)
			.containsExactly(tuple(null, "Kid Andersson"), tuple(OTHER_CHILD_PARTY_ID, "Kid Two"));
	}

	@Test
	void emptyRosterYieldsNoChildren() {
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any()))
			.thenReturn(new LifecareRoster(PARTY_ID, null, List.of()));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill.getChildren()).isEmpty();
	}

	@Test
	void unknownApplicantPartyIdYieldsEmptyResult() {
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any()))
			.thenThrow(Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + PARTY_ID));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
	}

	@Test
	void lifecareFailureDegradesToEmptyResult() {
		when(lifecareCaseServiceMock.latestRoster(eq(MUNICIPALITY_ID), eq(PARTY_ID), any()))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare unreachable"));

		final var prefill = service().prefill(MUNICIPALITY_ID, PARTY_ID);

		assertThat(prefill.isLifecareChecked()).isFalse();
		assertThat(prefill.getChildren()).isEmpty();
	}
}
