package org.ecorous

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.*

data class Account(
    val id: UUID,
    val username: String,
    val password: String?,
    val colour: Int,
    val apiKey: String,
)

data class Task(
    val id: UUID,
    val title: String,
    val description: String,
    val colour: Int,
    val group: String,
    val accountID: UUID,
    val flags: Long = 0L,
) {
    val completed: Boolean
        get() = (flags and TodoFlags.COMPLETED) != 0L
}

data class Project(
    val id: UUID,
    val title: String,
    val owner: Account,
    val groups: List<Group>,
    val colour: Int,
    val projectColumns: List<ProjectColumn>
)

data class ProjectColumn(
    val id: UUID,
    val project: Project,
    val title: String,
    val colour: Int,
    val tasks: List<Task>
)

data class Group(
    val id: UUID,
    val title: String,
    val owner: UUID,
    val colour: Int,
    val members: List<UUID>,
    val permissions: Long = 0L,
)

@Serializable
data class SerializableTask(
    val id: String,
    val title: String,
    val description: String,
    val group: String,
)

@Serializable
data class SerializableGroup(
    val id: String,
    val title: String,
    val owner: String,
    val members: List<String>,
)
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

    fun Task.serializable(): SerializableTask {
        return SerializableTask(this.id.toString(), this.title, this.description, this.group)
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

object TodoFlags {
    const val COMPLETED = 0x1L
}