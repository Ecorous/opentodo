package org.ecorous.database

import org.jetbrains.exposed.sql.Table

object Tasks : Table() {
    val id = uuid("id")
    val title = varchar("title", 75)
    val description = varchar("description", 2000)
    val group = varchar("group", 75)
    val colour = integer("colour")
    val accountID = uuid("accountID")
    val flags = long("flags")
}

object Accounts : Table() {
    val id = uuid("id")
    val username = varchar("username", 50)
    val password = varchar("password", 256).nullable()
    val colour = integer("colour")
    val apiKey = varchar("apiKey", 32)
}

object Groups : Table() {
    val id = uuid("id")
    val title = varchar("title", 75)
    val colour = integer("colour")
    val permissions = long("permissions")
}

object GroupMembers : Table() {
    val groupID = uuid("groupID")
    val accountID = uuid("accountID")
    val owner = bool("owner")
}

object Projects : Table() {
    val id = uuid("id")
    val title = varchar("title", 75)
    val colour = integer("colour")
}