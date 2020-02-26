
# Debezium Outbox Proof of concept

Async service communication based on Outbox pattern using: Kotlin, Kafka, Debezium Kafka Connect, Spring and docker.

**Delivery guaranties** - transactional persistence and idempotent consumer.

**Flexibility** - payload has abstract type so it's detalization is configurable.

**Low coupling** - communication via kafka.

**Low performance overhead** - the only performance overhead here is writing to outbox table in 
the same transaction

## How it works

Account-service write something to DB and in the same transaction writes to outbox table.
This table is listening by Debezium, which transform it's content and write to kafka topic.
History-service listen to kafka topic and can aggregate and process events.

Account-service -> Db -> KConnect (Debezium) -> Kafka -> History-service

## How to run

To run it you need java 8+, maven and docker. For http calls httpie was used

```shell script
mvn -f account-service clean install
mvn -f event-service clean install
docker-compose up
```

After all apps are running register a connector

```shell script
http PUT http://localhost:8083/connectors/outbox-connector/config < register-postgres.json^C
```

Add/modify/delete data with
```shell script
http localhost:8080/fill  # add 10 records
http localhost:8080/edit  # change all records
http localhost:8080/del   # delete all records
```

View changes in kafdrop (`localhost:9000`) and in event-service logs
