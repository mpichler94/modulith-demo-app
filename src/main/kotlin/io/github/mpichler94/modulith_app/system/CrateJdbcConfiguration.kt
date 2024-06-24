package io.github.mpichler94.modulith_app.system

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.relational.core.dialect.Dialect
import org.springframework.data.relational.core.dialect.PostgresDialect
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID

@Configuration
internal class CrateJdbcConfiguration : AbstractJdbcConfiguration() {

    override fun userConverters(): List<*> {
        return listOf(UuidReadConverter(), UuidWriteConverter())
    }

    override fun jdbcDialect(operations: NamedParameterJdbcOperations): Dialect {
        return operations.jdbcOperations.execute { connection: Connection ->
            if (isCrateDB(connection)) {
                PostgresDialect.INSTANCE
            } else {
                super.jdbcDialect(operations)
            }
        }!!
    }

    @Throws(SQLException::class)
    private fun isCrateDB(connection: Connection): Boolean {
        return connection.metaData.databaseProductName.lowercase().contains("crate")
    }

    @Bean
    internal fun persistableConverter(): BeforeConvertCallback<UuidPersistable> {
        return BeforeConvertCallback {
            it.apply {
                if (id == null) {
                    setId(UUID.randomUUID())
                }
            }
        }
    }

    @ReadingConverter
    private class UuidReadConverter : Converter<String, UUID> {
        override fun convert(source: String): UUID {
            return UUID.fromString(source)
        }
    }

    @WritingConverter
    private class UuidWriteConverter : Converter<UUID, String> {
        override fun convert(source: UUID): String {
            return source.toString()
        }
    }
}
