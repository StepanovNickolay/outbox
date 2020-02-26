package ru.step.outbox

interface OutboxService {
    fun <T> send(
            action: Action,
            aggregate: Aggregate<T>,
            aggregateId: Any,
            payload: T? = null,
            author: String
    ): Outbox

    fun <T> send(
            action: Action,
            aggregate: Aggregate<T>,
            aggregateId: Any,
            author: String
    ): Outbox
}

abstract class Aggregate<T>(val typeName: String)

enum class Action {
    CREATE,
    CHANGE,
    DELETE
}

interface TraceProvider {
    fun getTrace(): Long
}
