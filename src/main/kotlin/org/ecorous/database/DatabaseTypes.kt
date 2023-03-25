package org.ecorous.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.stringLiteral
import java.util.UUID

object Todos: UUIDTable() {
    val title = varchar("title", 75)
    val description = varchar("description", 500)
    val accountID = uuid("accountID")
}

object Accounts: Table() {
    val id = uuid("id")
    val username = varchar("username", 50)
    val apiKey = varchar("apiKey", 32)
}