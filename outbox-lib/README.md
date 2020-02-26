# Outbox

Библиотека для реализации паттерна outbox (https://microservices.io/patterns/data/transactional-outbox.html)

Позволяет записывать события, произошедшие в системе, в отдельную таблицу outbox.
Таблицу могут прослушивать другие сервисы и реагировать на произошедшие события.

Текущая версия: **1.2.2**

## Как пользоваться

Чтобы включить библиотеку добавляем зависимость
```xml
<dependency>
            <groupId>ru.ursip.libs</groupId>
            <artifactId>outbox</artifactId>
            <version>${outbox-version}</version>
 </dependency>
```

И включаем библиотеку
```kotlin
@Configuration
@EnableOutbox
class OutboxConfig
```

Определяем какие события хотим фиксировать (в зависимости от бизнес требований).
Для каждого события создаем наследника AggregateType. 
В качестве параметризующего типа указываем класс агрегата, который планируется для сериализации.
Агрегат может быть как сущностью, так и просто классом.
Если агрегат сущность, билиотека автоматически подтянет все lazy-поля.

Например, для бизнес-домена Organization создаем наследника:

```kotlin
data class Organization(
    //data
)
object OrganizationAggregate : AggregateType<Organization>("Organization")
```

Отправка сообщений происходит с помощью OutboxService. Инжектим себе сервис и 
отпарвялем события в текущей транзакции

```kotlin
outboxService.send(Action.CREATE, OrganizationAggregate, organization.id, organization, author)
```
где:
- Action.CREATE - действие
- OrganizationAggregate - класс агрегата
- organization.id - идентификатор агрегата
- organization - объект агрегата
- author - автор изменения

После запуска приложения, библиотека проверит наличие таблицы outbox в текущей схеме, и если не найдет, создаст ее.

Для работы требует JPA

## Расширяемость 

При наличии в контексте TraceProvider, OutboxService будет использовать системные.

