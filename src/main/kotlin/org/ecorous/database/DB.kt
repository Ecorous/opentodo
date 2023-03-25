package org.ecorous.database

import org.ecorous.Account
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager

object DB {
    var db: Database? = null
    var initialized = false
    fun init() {
        db = Database.connect("jdbc:sqlite:opentodo.db")
        initialized = true
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Todos)
                SchemaUtils.create(Accounts)
            }
        }
    }

    fun pushAccount(account: Account) {
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                Accounts.insert {
                    it[id] = account.id
                    it[username] = account.username
                    it[apiKey] = account.apiKey
                }
            }
        }
    }

    fun ifExistsUsername(username: String): Boolean {
        var sList = false
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                sList = try {
                    Accounts.select { Accounts.username eq username }.single()[Accounts.username] != ""
                    true
                } catch (e: java.util.NoSuchElementException) {
                    false
                }
            }
        }
        return sList
    }
}