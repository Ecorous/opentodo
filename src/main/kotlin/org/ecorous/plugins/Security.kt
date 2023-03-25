package org.ecorous.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.Identity.encode
import kotlinx.css.input
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.ecorous.Account
import org.ecorous.Utils
import org.ecorous.database.Accounts
import org.ecorous.database.DB
import java.util.*

@Serializable
data class AccountInput(val username: String)
fun Application.configureSecurity() {
    routing {
        post("/account") {
            val json = call.receiveText()
            val input = Json.decodeFromString<AccountInput>(json)
            println(DB.ifExistsUsername(input.username))
            if (input.username.length > 50) {
                call.respond(mapOf("error" to "username too long. max chars: 50"))
            } else if (DB.ifExistsUsername(input.username)) {
                call.respond(mapOf("error" to "username already exists. must be unique"))
            } else {
                var apiKey = Utils.generateApiKey()
                val account = Account(UUID.randomUUID(), input.username, apiKey)
                try {
                    DB.pushAccount(account)
                } catch (e: SerializationException) {

                }
                call.respond(mapOf("id" to account.id, "key" to apiKey))
            }
        }
    }
}
