//package io.github.mpichler94.modulith_app.system
//
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.BeanClassLoaderAware
//import org.springframework.context.annotation.Primary
//import org.springframework.data.jdbc.repository.query.Query
//import org.springframework.jdbc.core.JdbcOperations
//import org.springframework.modulith.events.core.EventPublicationRepository
//import org.springframework.modulith.events.core.EventSerializer
//import org.springframework.modulith.events.core.PublicationTargetIdentifier
//import org.springframework.modulith.events.core.TargetEventPublication
//import org.springframework.stereotype.Component
//import org.springframework.transaction.annotation.Transactional
//import org.springframework.util.ClassUtils
//import java.sql.ResultSet
//import java.sql.SQLException
//import java.sql.Timestamp
//import java.time.Instant
//import java.util.Optional
//import java.util.UUID
//import java.util.stream.Collectors
//import java.util.stream.IntStream
//
//@Component
//@Primary
//class CrateEventPublicationRepository(
//    private val operations: JdbcOperations,
//    private val serializer: EventSerializer
//) : EventPublicationRepository, BeanClassLoaderAware {
//    private lateinit var classLoader: ClassLoader
//
//    companion object {
//        private const val DELETE_BATCH_SIZE = 100
//
//        private val LOGGER = LoggerFactory.getLogger(CrateEventPublicationRepository::class.java)
//
//        private fun batch(input: List<*>, batchSize: Int): List<List<Any?>> {
//            return input.windowed(batchSize, batchSize, true)
//        }
//
//        private fun toParameterPlaceholders(length: Int): String {
//            return IntStream.range(0, length).mapToObj { "?" }
//                .collect(Collectors.joining(", ", "(", ")"))
//        }
//    }
//
//    override fun setBeanClassLoader(classLoader: ClassLoader) {
//        this.classLoader = classLoader
//    }
//
//    @Transactional
//    @Query(
//        """insert into EVENT_PUBLICATION (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT)
//        values (
//          :#{#publication.getIdentifier()},
//          :#{#publication.getEvent().getClass().getName()},
//          :#{#publication.getTargetIdentifier().getValue()},
//          :#{#Timestamp.from(publication.getPublicationDate()},
//          :#{#serializeEvent(publication.getEvent())}
//          )"""
//    )
//    override fun create(publication: TargetEventPublication): TargetEventPublication {
//        val serializedEvent = serializeEvent(publication.event)
//
//        operations.update(
//            """
//			INSERT INTO EVENT_PUBLICATION (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT)
//			VALUES (?, ?, ?, ?, ?)
//			""",
//            uuidToDatabase(publication.identifier), //
//            publication.event.javaClass.getName(), //
//            publication.targetIdentifier.value, //
//            Timestamp.from(publication.publicationDate), //
//            serializedEvent
//        )
//
//        return publication
//    }
//
//    @Transactional
//    override fun markCompleted(event: Any, identifier: PublicationTargetIdentifier, completionDate: Instant) {
//        operations.update(
//            """
//			UPDATE EVENT_PUBLICATION
//			SET COMPLETION_DATE = ?
//			WHERE
//					LISTENER_ID = ?
//					AND SERIALIZED_EVENT = ?
//			""",
//            Timestamp.from(completionDate), //
//            identifier.value, //
//            serializer.serialize(event)
//        )
//    }
//
//    @Transactional
//    override fun findIncompletePublicationsByEventAndTargetIdentifier(
//        event: Any,
//        targetIdentifier: PublicationTargetIdentifier
//    ): Optional<TargetEventPublication> {
//        val result = operations.query(
//            """SELECT *
//                FROM EVENT_PUBLICATION
//                WHERE
//        SERIALIZED_EVENT = ?
//        AND LISTENER_ID = ?
//        AND COMPLETION_DATE IS NULL
//                ORDER BY PUBLICATION_DATE
//        """,
//            this::resultSetToPublications, //
//            serializeEvent(event), //
//            targetIdentifier.value
//        )
//
//        return Optional.ofNullable(result?.firstOrNull())
//    }
//
//    override fun findCompletedPublications(): List<TargetEventPublication> {
//        val result = operations.query(
//            """
//			SELECT ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT
//			FROM EVENT_PUBLICATION
//			WHERE COMPLETION_DATE IS NOT NULL
//			ORDER BY PUBLICATION_DATE ASC
//			""",
//            this::resultSetToPublications
//        )
//
//        return result ?: listOf()
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    @SuppressWarnings("null")
//    override fun findIncompletePublications(): List<TargetEventPublication> {
//        return operations.query(
//            """
//			SELECT ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT
//			FROM EVENT_PUBLICATION
//			WHERE COMPLETION_DATE IS NULL
//			ORDER BY PUBLICATION_DATE ASC
//			""",
//            this::resultSetToPublications
//        )!!
//    }
//
//    override fun findIncompletePublicationsPublishedBefore(instant: Instant): List<TargetEventPublication> {
//        val result = operations.query(
//            """
//			SELECT ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT
//			FROM EVENT_PUBLICATION
//			WHERE
//					COMPLETION_DATE IS NULL
//					AND PUBLICATION_DATE < ?
//			ORDER BY PUBLICATION_DATE ASC
//			""",
//            this::resultSetToPublications,
//            Timestamp.from(instant)
//        )
//
//        return result ?: listOf()
//    }
//
//    override fun deletePublications(identifiers: List<UUID>) {
//        val dbIdentifiers = identifiers.stream().map(::uuidToDatabase).toList()
//
//        batch(dbIdentifiers, DELETE_BATCH_SIZE)
//            .forEach {
//                operations.update(
//                    """
//			DELETE
//			FROM EVENT_PUBLICATION
//			WHERE
//					ID IN
//			""" + toParameterPlaceholders(it.size),
//                    it
//                )
//            }
//    }
//
//    override fun deleteCompletedPublications() {
//        operations.execute(
//            """
//			DELETE
//			FROM EVENT_PUBLICATION
//			WHERE
//					COMPLETION_DATE IS NOT NULL
//			"""
//        )
//    }
//
//    override fun deleteCompletedPublicationsBefore(instant: Instant) {
//        operations.update(
//            """
//			DELETE
//			FROM EVENT_PUBLICATION
//			WHERE
//					COMPLETION_DATE < ?
//			""",
//            Timestamp.from(instant)
//        )
//    }
//
//    private fun serializeEvent(event: Any): String {
//        return serializer.serialize(event).toString()
//    }
//
//    /**
//     * Effectively a {@link ResultSetExtractor} to drop {@link TargetEventPublication}s that cannot be deserialized.
//     *
//     * @param resultSet must not be {@literal null}.
//     * @return will never be {@literal null}.
//     * @throws SQLException
//     */
//    @Throws(SQLException::class)
//    private fun resultSetToPublications(resultSet: ResultSet): List<TargetEventPublication> {
//        val result = mutableListOf<TargetEventPublication>()
//
//        while (resultSet.next()) {
//            val publication = resultSetToPublication(resultSet)
//
//            if (publication != null) {
//                result.add(publication)
//            }
//        }
//
//        return result
//    }
//
//    /**
//     * Effectively a {@link RowMapper} to turn a single row into an {@link TargetEventPublication}.
//     *
//     * @param rs must not be {@literal null}.
//     * @return can be {@literal null}.
//     * @throws SQLException
//     */
//    @Throws(SQLException::class)
//    private fun resultSetToPublication(rs: ResultSet): TargetEventPublication? {
//        val id = getUuidFromResultSet(rs)
//        val eventClass: Class<*> = loadClass(id, rs.getString("EVENT_TYPE")) ?: return null
//
//        val completionDate = rs.getTimestamp("COMPLETION_DATE")
//        val publicationDate = rs.getTimestamp("PUBLICATION_DATE").toInstant()
//        val listenerId = rs.getString("LISTENER_ID")
//        val serializedEvent = rs.getString("SERIALIZED_EVENT")
//
//        return JdbcEventPublication(
//            id,
//            publicationDate,
//            listenerId,
//            serializedEvent,
//            eventClass,
//            serializer,
//            completionDate?.toInstant()
//        )
//    }
//
//    private fun uuidToDatabase(id: UUID): Any {
//        return id.toString()
//    }
//
//    @Throws(SQLException::class)
//    private fun getUuidFromResultSet(rs: ResultSet): UUID {
//        return UUID.fromString(rs.getString("ID"))
//    }
//
//    private fun loadClass(id: UUID?, className: String): Class<*>? {
//        try {
//            return ClassUtils.forName(className, classLoader)
//        } catch (e: ClassNotFoundException) {
//            LOGGER.warn("Event '$id' of unknown type '$className' found", e)
//            return null
//        }
//    }
//}
//private class JdbcEventPublication(
//    private val id: UUID,
//    private val publicationDate: Instant,
//    private val listenerId: String,
//    private val serializedEvent: String,
//    private val eventType: Class<*>,
//    private val serializer: EventSerializer,
//    private var completionDate: Instant?
//) : TargetEventPublication {
//    override fun getIdentifier(): UUID {
//        return id
//    }
//
//    override fun getEvent(): Any {
//        return serializer.deserialize(serializedEvent, eventType)
//    }
//
//    override fun getTargetIdentifier(): PublicationTargetIdentifier {
//        return PublicationTargetIdentifier.of(listenerId)
//    }
//
//    override fun getPublicationDate(): Instant {
//        return publicationDate
//    }
//
//    override fun getCompletionDate(): Optional<Instant> {
//        return Optional.ofNullable(completionDate)
//    }
//
//    override fun isPublicationCompleted(): Boolean {
//        return completionDate != null
//    }
//    override fun markCompleted(instant: Instant) {
//        this.completionDate = instant
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as JdbcEventPublication
//
//        if (id != other.id) return false
//        if (publicationDate != other.publicationDate) return false
//        if (listenerId != other.listenerId) return false
//        if (serializedEvent != other.serializedEvent) return false
//        if (eventType != other.eventType) return false
//        if (serializer != other.serializer) return false
//        if (completionDate != other.completionDate) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = id.hashCode()
//        result = 31 * result + publicationDate.hashCode()
//        result = 31 * result + listenerId.hashCode()
//        result = 31 * result + serializedEvent.hashCode()
//        result = 31 * result + eventType.hashCode()
//        result = 31 * result + serializer.hashCode()
//        result = 31 * result + (completionDate?.hashCode() ?: 0)
//        return result
//    }
//}
