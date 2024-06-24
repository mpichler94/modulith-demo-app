package io.github.mpichler94.modulith_app

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<ModulithAppApplication>().with(TestcontainersConfiguration::class).run(*args)
}
