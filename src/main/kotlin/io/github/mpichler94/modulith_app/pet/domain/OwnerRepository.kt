package io.github.mpichler94.modulith_app.pet.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
interface OwnerRepository : org.springframework.data.repository.Repository<Owner, UUID> {

    @Transactional(readOnly = true)
    fun findByLastName(lastName: String, pageable: Pageable): Page<Owner>

    @Transactional(readOnly = true)
    fun findById(id: UUID): Owner?

    fun save(owner: Owner)

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<Owner>

}