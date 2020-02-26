package ru.step.eventservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.*

@SpringBootApplication
class EventServiceApplication

fun main(args: Array<String>) {
    runApplication<EventServiceApplication>(*args)
}

@Configuration
@EnableKafka
class KafkaConfig

@Component
class Listener {
//    @KafkaListener(topicPattern = "outbox.", groupId = "234123412")
    @KafkaListener(topics = ["outbox.Account"], groupId = "12443")
    fun listen(
            @Payload body: String
    ) {
        println("Got msg $body")
    }
}
