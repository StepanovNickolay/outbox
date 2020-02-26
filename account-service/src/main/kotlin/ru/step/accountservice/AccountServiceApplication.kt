package ru.step.accountservice

import kotlinx.coroutines.coroutineScope
import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import ru.step.outbox.Action.*
import ru.step.outbox.Aggregate
import ru.step.outbox.EnableOutbox
import ru.step.outbox.OutboxService
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.sql.DataSource

@SpringBootApplication
class AccountServiceApplication

fun main(args: Array<String>) {
    runApplication<AccountServiceApplication>(*args)
}

@Configuration
@EnableOutbox
class OutboxConfig

@Configuration
class LiquibaseConfig {
    @Autowired
    lateinit var currentDataSource: DataSource

    @Bean
    fun liquibase() = SpringLiquibase().apply {
        changeLog = "classpath:/db/liquibase-changelog.xml"
        dataSource = currentDataSource
        defaultSchema = currentDataSource.connection.schema
        isDropFirst = false
    }
}

@RestController
class AccountController(
        val accountRepository: AccountRepository,
        val outboxService: OutboxService
) {
    @GetMapping("/fill")
    suspend fun fillData() = coroutineScope {
        for (i in 1..10) {
            accountRepository.save(Account(UUID.randomUUID(), "test$i"))
                    .also { outboxService.send(CREATE, AccountAggregate, it.id, it, "test") }
        }
    }

    @GetMapping("/edit")
    suspend fun changeData() = coroutineScope {
        accountRepository.findAll()
                .map {
                    it.copy(
                            name = UUID.randomUUID().toString()
                    )
                }.map{
                    accountRepository.save(it)
                            .also { outboxService.send(CHANGE, AccountAggregate, it.id, it, "test") }
                }
    }

    @GetMapping("/del")
    suspend fun delData() = coroutineScope {
        accountRepository.findAll()
                .forEach { acc ->
                    accountRepository.deleteById(acc.id)
                            .also { outboxService.send(DELETE, AccountAggregate, acc.id,  "test") }
                }
    }


    @GetMapping
    suspend fun getAll(): List<Account> = accountRepository.findAll()
}

object AccountAggregate : Aggregate<Account>("Account")

@Repository
interface AccountRepository : JpaRepository<Account, UUID>

@Entity
data class Account(@Id val id: UUID = UUID.randomUUID(), val name: String)
