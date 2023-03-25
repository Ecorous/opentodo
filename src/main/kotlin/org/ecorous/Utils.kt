package org.ecorous

import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID

data class Account(val id: UUID, val username: String, val apiKey: String)
data class Todo(val id: UUID, val title: String, val description: String, val group: String, val accountID: UUID)
@Serializable
data class SerializableTodo(val id: String, val title: String, val description: String, val group: String)
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
}