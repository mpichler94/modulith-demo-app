package io.github.mpichler94.modulith_app

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.modulith.core.ApplicationModules

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class ModulithAppApplicationTests {

	@Test
	fun verifyStructure() {
		ApplicationModules.of(ModulithAppApplication::class.java).verify()
	}

	@Test
	fun contextLoads() {
	}

}
