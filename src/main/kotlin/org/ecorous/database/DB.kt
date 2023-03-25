package org.ecorous.database

import org.ecorous.Account
import org.ecorous.SerializableTodo
import org.ecorous.Todo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.util.*

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

    fun pushTodo(todo: Todo) {
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                Todos.insert {
                    it[id] = todo.id
                    it[title] = todo.title
                    it[description] = todo.description
                    it[group] = todo.group
                    it[accountID] = todo.accountID
                }
            }
        }
    }

    fun getAccountByKeyOrNull(apiKey: String): Account? {
        var account: Account? = null
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                val selection = Accounts.select {Accounts.apiKey eq apiKey}.singleOrNull()
                if (selection != null) {
                    account = Account(selection[Accounts.id], selection[Accounts.username], selection[Accounts.apiKey])
                }
            }
        }
        return account
    }

    fun getAccountByUsernameOrNull(username: String): Account? {
        var account: Account? = null
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                val selection = Accounts.select {Accounts.username eq username}.singleOrNull()
                if (selection != null) {
                    account = Account(selection[Accounts.id], selection[Accounts.username], selection[Accounts.apiKey])
                }
            }
        }
        return account
    }

    fun getAccountByUUIDOrNull(id: UUID): Account? {
        var account: Account? = null
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                val selection = Accounts.select {Accounts.id eq id}.singleOrNull()
                if (selection != null) {
                    account = Account(selection[Accounts.id], selection[Accounts.username], selection[Accounts.apiKey])
                }
            }
        }
        return account
    }

    fun getTodosForAccount(account: Account): List<Todo> {
        val todos = mutableListOf<Todo>()
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                val selection = Todos.select {Todos.accountID eq account.id}
                selection.forEach {
                    todos.add(Todo(it[Todos.id], it[Todos.title], it[Todos.description], it[Todos.group], it[Todos.accountID]))
                }
            }
        }
        return todos
    }

    fun getSerializableTodosForAccount(account: Account): List<SerializableTodo> {
        val todos = mutableListOf<SerializableTodo>()
        db.apply {
            transaction {
                addLogger(StdOutSqlLogger)
                val selection = Todos.select {Todos.accountID eq account.id}
                selection.forEach {
                    todos.add(SerializableTodo(it[Todos.id].toString(), it[Todos.title], it[Todos.description], it[Todos.group]))
                }
            }
        }
        return todos
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