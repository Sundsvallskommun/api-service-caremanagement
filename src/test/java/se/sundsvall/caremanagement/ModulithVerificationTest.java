package se.sundsvall.caremanagement;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Build-time enforcement of module boundaries.
 *
 * - {@code verify()} fails if any module reaches across a boundary it didn't declare.
 * - {@code Documenter} emits PlantUML + AsciiDoc to {@code target/spring-modulith-docs/}
 * on every test run — open it at {@code target/spring-modulith-docs/components.puml}.
 */

class ModulithVerificationTest {

	private final ApplicationModules modules = ApplicationModules.of(Application.class);

	@Test
	void modulesAreWellFormed() {
		modules.verify();
	}

	@Test
	void writeDocumentation() {
		new Documenter(modules)
			.writeModulesAsPlantUml()
			.writeIndividualModulesAsPlantUml()
			.writeDocumentation();
	}
}
