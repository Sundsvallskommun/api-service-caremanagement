package se.sundsvall.caremanagement.lifecare.service.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapperUtilTest {

	@Test
	void toLifecareNoteCutsOnlyWhatLifecareCannotTake() {
		assertThat(MapperUtil.toLifecareNote(null)).isNull();
		assertThat(MapperUtil.toLifecareNote("SSBTEK: Bostadsbidrag")).isEqualTo("SSBTEK: Bostadsbidrag");
		assertThat(MapperUtil.toLifecareNote("x".repeat(80))).isEqualTo("x".repeat(80));
		assertThat(MapperUtil.toLifecareNote("y".repeat(81))).isEqualTo("y".repeat(80));
	}
}
