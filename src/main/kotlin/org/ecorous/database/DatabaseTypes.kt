package org.ecorous.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

object Todos: UUIDTable() {
    val title = varchar("title", 75)
    val description = varchar("description", 500)
    val accountID = uuid("accountID")
}

object Accounts: Table() {
    val id = uuid("id")
    val username = varchar("username", 50)
    val password = varchar("password", 256).nullable()
    val apiKey = varchar("apiKey", 32)
}