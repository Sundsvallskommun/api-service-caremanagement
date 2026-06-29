package se.sundsvall.caremanagement.conversation.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SenderRoleTest {

	@Test
	void fromDirectionMapsInboundToClientAndOutboundToCaseworker() {
		assertThat(SenderRole.fromDirection(Direction.INBOUND)).isEqualTo(SenderRole.CLIENT);
		assertThat(SenderRole.fromDirection(Direction.OUTBOUND)).isEqualTo(SenderRole.CASEWORKER);
	}

	@Test
	void hasExactlyClientAndCaseworker() {
		assertThat(SenderRole.values()).containsExactly(SenderRole.CLIENT, SenderRole.CASEWORKER);
	}
}
