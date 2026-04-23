package se.sundsvall.caremanagement;

import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableFeignClients
@ExcludeFromJacocoGeneratedCoverageReport
public class Application {
	static void main(final String... args) {
		run(Application.class, args);
	}
}
