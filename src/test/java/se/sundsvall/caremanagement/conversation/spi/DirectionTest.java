package se.sundsvall.caremanagement.conversation.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DirectionTest {

	@Test
	void hasExactlyInboundAndOutbound() {
		assertThat(Direction.values()).containsExactly(Direction.INBOUND, Direction.OUTBOUND);
		assertThat(Direction.valueOf("INBOUND")).isEqualTo(Direction.INBOUND);
	}

	@Test
	void roleIsTheAuthoringParty() {
		assertThat(Direction.INBOUND.role()).isEqualTo("CLIENT");
		assertThat(Direction.OUTBOUND.role()).isEqualTo("CASEWORKER");
	}

	@Test
	void oppositeFlipsDirection() {
		assertThat(Direction.INBOUND.opposite()).isEqualTo(Direction.OUTBOUND);
		assertThat(Direction.OUTBOUND.opposite()).isEqualTo(Direction.INBOUND);
	}
}
