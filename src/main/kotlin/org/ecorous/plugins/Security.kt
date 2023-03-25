package org.ecorous.plugins

import com.password4j.Password
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.gosimple.nbvcxz.Nbvcxz
import me.gosimple.nbvcxz.resources.ConfigurationBuilder
import me.gosimple.nbvcxz.resources.DictionaryBuilder
import org.ecorous.Account
import org.ecorous.Utils
import org.ecorous.database.DB
import java.util.*

@Serializable
data class AccountInput(
    val username: String?,
    // Generally discouraged, but JSON makes this a bit annoying.
    val password: String?,
)

fun Application.configureSecurity() {
    routing {
        post("/account") {
            val json = call.receiveText()
            val input = Json.decodeFromString<AccountInput>(json)

            if (input.username == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "enter a username"))
                return@post
            }
            if (input.username.length > 50) {

                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "username too long. max chars: 50"))
                return@post
            }
            if (input.password == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to ":catstare: https://cdn.discordapp.com/emojis/1043075191955267665.png"))
                return@post
            }

            var account = DB.getAccountByUsernameOrNull(input.username)

            if (account == null) {
                // Note: If null, createAccount failed the password check
                account = createAccount(input) ?: return@post
            } else if (!Password.check(input.password, account.password).withArgon2()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "incorrect password"))
                return@post
            }

            call.respond(HttpStatusCode.OK, mapOf("id" to account.id.toString(), "key" to account.apiKey))
        }
        post("/account/delete") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    DB.deleteAccount(account)
                    call.respond(mapOf("message" to "account deleted"))
                }
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.createAccount(input: AccountInput): Account? {
    val selfDictionary = DictionaryBuilder()
        .setDictionaryName("user info")
        .setExclusion(true)
        .addWord(input.username, 0)
        .createDictionary()

    val estimatorConfig = ConfigurationBuilder()
        .setDictionaries(ConfigurationBuilder.getDefaultDictionaries() + selfDictionary)
        .createConfiguration()

    val estimator = Nbvcxz(estimatorConfig)

    if (!estimator.estimate(input.password).isMinimumEntropyMet) {
        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "password too weak"))
        return null
    }

    val result = Password.hash(input.password).addRandomSalt().withArgon2().result
    val apiKey = Utils.generateApiKey()

    val account = Account(UUID.randomUUID(), input.username!!, result, apiKey)

    DB.pushAccount(account)

    return account
}
