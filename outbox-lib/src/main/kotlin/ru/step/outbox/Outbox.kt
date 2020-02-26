package ru.step.outbox

import brave.Tracer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id

data class Outbox(
        @Id val id: UUID = UUID.randomUUID(),
        val aggregateType: String,
        val aggregateId: String,
        @Enumerated(EnumType.STRING) val type: Action,
        val payload: String? = null,
        val author: String,
        val traceId: String
)

class OutboxServiceJpa(
        private val traceProvider: TraceProvider,
        private val objectMapper: ObjectMapper,
        private val entityManager: EntityManager,
        private val jdbcTemplate: JdbcTemplate
) : OutboxService {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun <T> send(
            action: Action,
            aggregate: Aggregate<T>,
            aggregateId: Any,
            payload: T?,
            author: String
    ): Outbox = try {
        val outbox = Outbox(
                aggregateType = aggregate.typeName,
                type = action,
                aggregateId = aggregateId.toString(),
                payload = objectMapper.writeValueAsString(processPayload(payload)),
                author = author,
                traceId = traceProvider.getTrace().toString()
        )
        logger.debug("Writting to outbox: $outbox")
        jdbcTemplate.update {
            it.prepareStatement("""
            INSERT INTO outbox (id, aggregate_type, type, aggregate_id, payload, author, trace_id)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()).apply {
                        setObject(1, outbox.id)
                        setObject(2, aggregate.typeName)
                        setObject(3, outbox.type.name)
                        setObject(4, outbox.aggregateId)
                        setObject(5, outbox.payload)
                        setObject(6, outbox.author)
                        setObject(7, outbox.traceId)
                    }
        }
        outbox
    } catch (ex: Exception) {
        logger.error("Err while trying to save to outbox", ex)
        throw ex
    }

    override fun <T> send(
            action: Action,
            aggregate: Aggregate<T>,
            aggregateId: Any,
            author: String
    ): Outbox = send(action, aggregate, aggregateId, null, author)

    private fun <T> processPayload(payload: T?): T? =
            if (payload != null && isEntity(payload)) {
                @Suppress("UNCHECKED_CAST")
                Hibernate.unproxy(payload) as T
            } else payload

    private fun isEntity(obj: Any): Boolean =
            try {
                entityManager.metamodel.entity(obj::class.java)
                true
            } catch (ex: Exception) {
                false
            }
}

class TraceProviderImpl(val tracer: Tracer) : TraceProvider {
    override fun getTrace() = tracer.currentSpan().context().traceId()
}
