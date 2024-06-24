package io.github.mpichler94.modulith_app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@SpringBootApplication
@EnableJdbcRepositories
class ModulithAppApplication

fun main(args: Array<String>) {
	runApplication<ModulithAppApplication>(*args)
}
