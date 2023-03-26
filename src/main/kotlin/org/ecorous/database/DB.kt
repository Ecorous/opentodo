package org.ecorous.database

import org.ecorous.Account
import org.ecorous.SerializableTodo
import org.ecorous.Todo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object DB {
    var db: Database? = null
    var initialized = false
    fun init() {
        db = Database.connect("jdbc:sqlite:opentodo.db")
        initialized = true
        transaction(db) {
            SchemaUtils.create(Todos)
            SchemaUtils.create(Accounts)
        }
    }

    fun pushAccount(account: Account) {
        transaction(db) {
            Accounts.insert {
                it[id] = account.id
                it[username] = account.username
                it[password] = account.password
                it[apiKey] = account.apiKey
            }
        }
    }

    fun pushTodo(todo: Todo) {
        transaction(db) {
            Todos.insert {
                it[id] = todo.id
                it[title] = todo.title
                it[description] = todo.description
                it[group] = todo.group
                it[accountID] = todo.accountID
            }
        }
    }

    fun getAccountByKeyOrNull(apiKey: String): Account? {
        return transaction(db) {
            Accounts.select { Accounts.apiKey eq apiKey }.singleOrNull()?.accountFromRow()
        }
    }

    fun getAccountByUsernameOrNull(username: String): Account? {
        return transaction(db) {
            Accounts.select { Accounts.username eq username }.singleOrNull()?.accountFromRow()
        }
    }

    fun getAccountByIdOrNull(id: UUID): Account? {
        return transaction(db) {
            Accounts.select { Accounts.id eq id }.singleOrNull()?.accountFromRow()
        }
    }

    private fun ResultRow.accountFromRow(): Account {
        return Account(
            id = this[Accounts.id],
            username = this[Accounts.username],
            password = this[Accounts.password],
            apiKey = this[Accounts.apiKey],
        )
    }

    private fun ResultRow.todoFromRow(): Todo {
        return Todo(
            id = this[Todos.id],
            title = this[Todos.title],
            description = this[Todos.description],
            group = this[Todos.group],
            accountID = this[Todos.accountID],
        )
    }

    fun getTodosForAccount(account: Account): List<Todo> {
        val todos = mutableListOf<Todo>()
        db.apply {
            transaction {
                val selection = Todos.select { Todos.accountID eq account.id }
                selection.forEach {
                    todos.add(
                        Todo(
                            it[Todos.id],
                            it[Todos.title],
                            it[Todos.description],
                            it[Todos.group],
                            it[Todos.accountID]
                        )
                    )
                }
            }
        }
        return todos
    }

    fun getSerializableTodosForAccount(account: Account): List<SerializableTodo> {
        val todos = mutableListOf<SerializableTodo>()
        db.apply {
            transaction {
                val selection = Todos.select { Todos.accountID eq account.id }
                selection.forEach {
                    todos.add(
                        SerializableTodo(
                            it[Todos.id].toString(),
                            it[Todos.title],
                            it[Todos.description],
                            it[Todos.group]
                        )
                    )
                }
            }
        }
        return todos
    }

    fun accountByUsernameExists(username: String): Boolean {
        return transaction(db) {
            !Accounts.select { Accounts.username eq username }.empty()
        }
    }

    fun deleteAccount(account: Account) {
        transaction(db) {
            Accounts.deleteWhere { id eq account.id }
        }
    }

    fun getTodoByIDOrNull(id: UUID): Todo? {
        return transaction(db) {
            Todos.select { Todos.id eq id }.singleOrNull()?.todoFromRow()
        }
    }

    fun Todo.hasPermission(account: Account): Boolean {
        return account.hasPermission(this)
    }

    fun Account.hasPermission(todo: Todo): Boolean {
        return transaction(db) {
            addLogger(StdOutSqlLogger)
            Todos.select { Todos.id eq todo.id }.singleOrNull()?.get(Todos.accountID)
        } == this.id
    }
}