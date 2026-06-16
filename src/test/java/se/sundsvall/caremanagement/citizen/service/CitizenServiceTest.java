package se.sundsvall.caremanagement.citizen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.integration.CitizenClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String PERSONAL_NUMBER = "198001012389";

	@Mock
	private CitizenClient citizenClientMock;

	private CitizenService service() {
		return new CitizenService(citizenClientMock);
	}

	@Test
	void resolvesPersonalNumber() {
		when(citizenClientMock.getPersonNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(PERSONAL_NUMBER);

		assertThat(service().getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).contains(PERSONAL_NUMBER);
	}

	@Test
	void emptyWhenNoContent() {
		when(citizenClientMock.getPersonNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn(null);

		assertThat(service().getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).isEmpty();
	}

	@Test
	void emptyWhenBlank() {
		when(citizenClientMock.getPersonNumber(MUNICIPALITY_ID, PARTY_ID)).thenReturn("   ");

		assertThat(service().getPersonalNumber(MUNICIPALITY_ID, PARTY_ID)).isEmpty();
	}

	@Test
	void resolvesPartyId() {
		when(citizenClientMock.getGuid(MUNICIPALITY_ID, PERSONAL_NUMBER)).thenReturn(PARTY_ID);

		assertThat(service().getPartyId(MUNICIPALITY_ID, PERSONAL_NUMBER)).contains(PARTY_ID);
	}

	@Test
	void emptyPartyIdWhenNoContent() {
		when(citizenClientMock.getGuid(MUNICIPALITY_ID, PERSONAL_NUMBER)).thenReturn(null);

		assertThat(service().getPartyId(MUNICIPALITY_ID, PERSONAL_NUMBER)).isEmpty();
	}

	@Test
	void emptyPartyIdWhenBlank() {
		when(citizenClientMock.getGuid(MUNICIPALITY_ID, PERSONAL_NUMBER)).thenReturn("   ");

		assertThat(service().getPartyId(MUNICIPALITY_ID, PERSONAL_NUMBER)).isEmpty();
	}
}
