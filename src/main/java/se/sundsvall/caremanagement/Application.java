package se.sundsvall.caremanagement;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.modulith.Modulithic;
import se.sundsvall.dept44.ServiceApplication;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableFeignClients
@ExcludeFromJacocoGeneratedCoverageReport
@Modulithic(
	sharedModules = {
		"core", "shared", "operaton"
	},
	additionalPackages = {
		"se.sundsvall.caremanagement.types"   // scan nested type modules
	})
public class Application {
	static void main(final String... args) {
		run(Application.class, args);
	}
}
