package io.github.mpichler94.modulith_app.system

import java.util.UUID

open class Person(id: UUID?, val firstName: String, val lastName: String) : UuidPersistable(id)