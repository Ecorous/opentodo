package org.ecorous.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ecorous.Todo
import org.ecorous.database.DB
import java.util.*

@Serializable
data class TodoInput(val title: String, val description: String, val group: String)
fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(mapOf("error" to "exception caught: $cause"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(mapOf("error" to "not found"))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(mapOf("error" to "method not allowed"))
        }
        status(HttpStatusCode.InternalServerError) { call, status ->
            call.respond(mapOf("error" to "internal server error"))
        }
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(mapOf("error" to "unauthorized"))
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/todo") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val json = call.receiveText()
                    val input = Json.decodeFromString<TodoInput>(json)
                    if (input.title.length > 75) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "title too long. max chars: 75"))
                    } else if (input.description.length > 2000) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "description too long. max chars: 2000"))
                    } else if (input.group.length > 75) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "group too long. max chars: 75"))
                    } else {
                        val todoID = UUID.randomUUID()
                        val todo = Todo(todoID, input.title, input.description, input.group, account.id)
                        DB.pushTodo(todo)
                        call.respond(mapOf("id" to todoID.toString())) // // 1aa25a42-8f16-4c23-a452-0e5d8498d819
                    }
                }
            }

        }
        get("/todos") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    call.respond(DB.getSerializableTodosForAccount(account))
                }
            }
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}
