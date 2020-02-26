package ru.step.outbox

import brave.Tracer
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.sql.DataSource

@Configuration
class OutboxAutoconfigure {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var datasource: DataSource

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @PostConstruct
    fun initializa() {
        logger.debug("Outbox initialization")
        tableInit()
        logger.debug("Outbox successfully initialize")
    }

    @Bean
    @ConditionalOnMissingBean(TraceProvider::class)
    fun traceProvider(tracer: Tracer): TraceProvider = TraceProviderImpl(tracer)

    @Bean
    @ConditionalOnMissingBean(OutboxService::class)
    fun outboxService(
            objectMapper: ObjectMapper,
            traceProvider: TraceProvider,
            jdbcTemplate: JdbcTemplate
    ): OutboxService = OutboxServiceJpa(traceProvider, objectMapper, entityManager, jdbcTemplate)

    private fun tableInit() {
        val sql = """
            CREATE TABLE IF NOT EXISTS ${datasource.connection.schema}.outbox
            (
            	id uuid not null
            		constraint outbox_pkey
            			primary key,
            	aggregate_type varchar(255) not null,
            	type varchar(255) not null,
            	aggregate_id varchar(255) not null,
            	payload text,
            	author varchar(255) not null,
            	trace_id varchar(255) not null
            );

            comment on table outbox is 'Исходящие события';
            comment on column outbox.id is 'Идентификатор';
            comment on column outbox.aggregate_type is 'Тип события';
            comment on column outbox.type is 'Действие';
            comment on column outbox.aggregate_id is 'Идентификатор события';
            comment on column outbox.payload is 'Тело события';
            comment on column outbox.author is 'Автор изменений';
            comment on column outbox.trace_id is 'Автор изменений';
        """.trimIndent()
        try {
            jdbcTemplate.execute(sql)
        } catch (ex: Exception) {
            logger.error("Cannot initialize outbox table", ex)
        }
    }
}
