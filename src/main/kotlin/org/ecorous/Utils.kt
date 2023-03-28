package org.ecorous

import kotlinx.serialization.Serializable
import org.ecorous.Utils.serializable
import java.security.SecureRandom
import java.util.*

data class Account(val id: UUID, val username: String, val password: String?, val apiKey: String)
data class Todo(val id: UUID, val title: String, val description: String, val group: String, val accountID: UUID)
data class Group(val id: UUID, val title: String, val owner: UUID, val members: List<UUID>)
@Serializable
data class SerializableTodo(val id: String, val title: String, val description: String, val group: String)
@Serializable
data class SerializableGroup(val id: String, val title: String, val owner: String, val members: List<String>)
object Utils {
    fun generateApiKey(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()
        val random = SecureRandom()
        val sb = StringBuilder(32)
        for (i in 0 until 32) {
            sb.append(chars[random.nextInt(chars.size)])
        }
        return sb.toString()
    }

    fun Todo.serializable(): SerializableTodo {
        return SerializableTodo(this.id.toString(), this.title, this.description, this.group)
    }
    fun Group.serializable(): SerializableGroup {
        val list = mutableListOf<String>()
        this.members.forEach {
            list.add(this.members.indexOf(it), it.toString())
        }
        return SerializableGroup(this.id.toString(), this.title, this.owner.toString(), list)
    }
    fun String.validUUID(): Boolean {
        return try {
            UUID.fromString(this)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}