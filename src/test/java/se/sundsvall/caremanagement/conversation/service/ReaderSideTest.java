package se.sundsvall.caremanagement.conversation.service;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.conversation.spi.Direction;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderSideTest {

	@Test
	void caseworkerReadsInbound() {
		assertThat(ReaderSide.CASEWORKER.addressedDirection()).isEqualTo(Direction.INBOUND);
	}

	@Test
	void clientReadsOutbound() {
		assertThat(ReaderSide.CLIENT.addressedDirection()).isEqualTo(Direction.OUTBOUND);
	}
}
