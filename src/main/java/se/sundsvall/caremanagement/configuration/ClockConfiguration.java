package se.sundsvall.caremanagement.configuration;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single {@link Clock} every time-reading component in this service is constructor-injected with. One bean for
 * the whole application, in Swedish time: components that only compare {@link java.time.Instant}s are unaffected by
 * the zone, and components that read a zoned date (working days, decision deadlines) need Swedish time to be correct.
 */
@Configuration
public class ClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of("Europe/Stockholm"));
	}
}
