package org.ecorous

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID

data class Account(val id: UUID, val username: String, val apiKey: String)
object Utils {
    fun randomBytes(length: Int): String {
        val buffer = ByteBuffer.allocate(length)
        SecureRandom().nextBytes(buffer.array())
        return buffer.array().toString()
    }
}