package io.github.mpichler94.modulith_app.pet.domain

import io.github.mpichler94.modulith_app.system.Person
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("owners")
class Owner(id: UUID, firstName: String, lastName: String, val address: String, val city: String, val telephone: String, val petIds: List<UUID>) :
    Person(id, firstName, lastName) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Owner

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
