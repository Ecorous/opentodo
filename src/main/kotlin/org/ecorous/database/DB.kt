package org.ecorous.database

import org.ecorous.*
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
            SchemaUtils.create(Tasks)
            SchemaUtils.create(Groups)
            SchemaUtils.create(GroupMembers)
            SchemaUtils.create(Accounts)
        }
    }

   fun pushGroup(group: Group) {
        transaction(db) {
            Groups.insert {
                it[id] = group.id
                it[title] = group.title
            }
            GroupMembers.insert {
                it[groupID] = group.id
                it[accountID] = group.owner
                it[owner] = true
            }
            group.members.forEach { id: UUID ->
                GroupMembers.insert {
                    it[groupID] = group.id
                    it[accountID] = id
                    it[owner] = false
                }
            }
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

    fun Account.setAPIKey(key: String) {
        var out = key
        if (key == "") {
            out = this.apiKey
        }
        transaction(db) {
            val id = this@setAPIKey.id
            Accounts.select { Accounts.id eq id }.singleOrNull()!![Accounts.apiKey] = out
        }
    }

    fun pushTask(task: Task) {
        transaction(db) {
            Tasks.insert {
                it[id] = task.id
                it[title] = task.title
                it[description] = task.description
                it[group] = task.group
                it[accountID] = task.accountID
                it[flags] = task.flags
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

    private fun ResultRow.taskFromRow(): Task {
        return Task(
            id = this[Tasks.id],
            title = this[Tasks.title],
            description = this[Tasks.description],
            group = this[Tasks.group],
            accountID = this[Tasks.accountID],
            flags = this[Tasks.flags],
        )
    }

    private fun ResultRow.groupFromRow(): Group {
        return Group(
            id = this[Groups.id],
            title = this[Groups.title],
            permissions = this[Groups.permissions],
            owner = UUID(0L, 0L),
            members = emptyList(),
        )
    }

    fun getTasksForAccount(account: Account): List<Task> {
        val tasks = mutableListOf<Task>()
        db.apply {
            transaction {
                val selection = Tasks.select { Tasks.accountID eq account.id }
                selection.forEach {
                    tasks.add(it.taskFromRow())
                }
            }
        }
        return tasks
    }

    fun getSerializableTasksForAccount(account: Account): List<SerializableTask> {
        val tasks = mutableListOf<SerializableTask>()
        db.apply {
            transaction {
                val selection = Tasks.select { Tasks.accountID eq account.id }
                selection.forEach {
                    tasks.add(
                        SerializableTask(
                            it[Tasks.id].toString(),
                            it[Tasks.title],
                            it[Tasks.description],
                            it[Tasks.group]
                        )
                    )
                }
            }
        }
        return tasks
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

    fun deleteGroup(group: Group) {
        transaction(db) {
            Groups.deleteWhere { id eq group.id }
        }
    }

    fun deleteTask(task: Task) {
        transaction(db) {
            Tasks.deleteWhere { id eq task.id }
        }
    }

    fun getTaskByIDOrNull(id: UUID): Task? {
        return transaction(db) {
            Tasks.select { Tasks.id eq id }.singleOrNull()?.taskFromRow()
        }
    }

    fun Task.hasPermission(account: Account): Boolean {
        return account.hasPermission(this)
    }

    fun Account.hasPermission(task: Task): Boolean {
        return transaction(db) {
            addLogger(StdOutSqlLogger)
            Tasks.select { Tasks.id eq task.id }.singleOrNull()?.get(Tasks.accountID)
        } == this.id
    }
}