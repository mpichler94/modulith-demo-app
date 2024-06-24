package io.github.mpichler94.modulith_app.system

import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import java.util.UUID

open class UuidPersistable(id: UUID? = null) : Persistable<UUID> {
    @Transient
    private var uuid: UUID? = id

    internal fun setId(id: UUID) {
        this.uuid = id
    }

    @Id
    @AccessType(AccessType.Type.PROPERTY)
    override fun getId(): UUID? {
        return uuid
    }

    override fun isNew(): Boolean {
        return uuid == null
    }
}
