package org.ecorous.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.ecorous.Group
import org.ecorous.Task
import org.ecorous.Utils.serializable
import org.ecorous.Utils.validUUID
import org.ecorous.database.DB
import org.ecorous.database.DB.hasPermission
import java.util.*

@Serializable
data class TaskInput(val title: String, val description: String, val group: String)
@Serializable
data class GroupInput (val title: String, val members: List<String>)

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(mapOf("error" to "exception caught: $cause"))
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(mapOf("error" to "not found"))
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respond(mapOf("error" to "method not allowed"))
        }
        status(HttpStatusCode.InternalServerError) { call, _ ->
            call.respond(mapOf("error" to "internal server error"))
        }
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(mapOf("error" to "unauthorized"))
        }
        status(HttpStatusCode.Forbidden) { call, _ ->
            call.respond(mapOf("error" to "access denied"))
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/task") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val json = call.receiveText()
                    val input = Json.decodeFromString<TaskInput>(json)
                    if (input.title.length > 75) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "title too long. max chars: 75"))
                    } else if (input.description.length > 2000) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "description too long. max chars: 2000")
                        )
                    } else if (input.group.length > 75) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "group too long. max chars: 75"))
                    } else {
                        val taskID = UUID.randomUUID()
                        val task = Task(taskID, input.title, input.description, input.group, account.id)
                        DB.pushTask(task)
                        call.respond(mapOf("id" to taskID.toString())) // // 1aa25a42-8f16-4c23-a452-0e5d8498d819
                    }
                }
            }

        }
        get("/tasks") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized)
                } else {
                    call.respond(DB.getSerializableTasksForAccount(account))
                }
            }
        }

        get("/task/{id}") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val idStr: String? = call.parameters["id"]
                    if (idStr == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else if (!idStr.validUUID()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                    } else {
                        val id = UUID.fromString(idStr)
                        val task = DB.getTaskByIDOrNull(id)
                        if (task == null) {
                            call.respond(HttpStatusCode.NotFound, HttpStatusCode.NotFound)
                        } else if (account.hasPermission(task)) {
                            call.respond(task.serializable())
                        } else {
                            call.respond(HttpStatusCode.Forbidden, HttpStatusCode.Forbidden)
                        }
                    }
                }
            }
        }
        delete("/task/{id}") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized)
                } else {
                    val idStr: String? = call.parameters["id"]
                    if (idStr == null) {
                        call.respond(HttpStatusCode.NotFound, HttpStatusCode.NotFound)
                    } else {
                        try {
                            val id = UUID.fromString(idStr)
                            val task = DB.getTaskByIDOrNull(id)
                            if (task == null) {
                                call.respond(HttpStatusCode.NotFound, HttpStatusCode.NotFound)
                            } else if (account.hasPermission(task)) {
                                DB.deleteTask(task)
                            } else {
                                call.respond(HttpStatusCode.Forbidden, HttpStatusCode.Forbidden)
                            }
                        } catch (_: IllegalArgumentException) {
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                        }
                    }
                }
            }
        }

        post("/group") {
            val apiKey = call.request.headers["Authorization"]
            if (apiKey == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val account = DB.getAccountByKeyOrNull(apiKey)
                if (account == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                } else {
                    val json = call.receiveText()
                    val input = Json.decodeFromString<GroupInput>(json)
                    if (input.title.length > 75) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "title too long. max chars: 75"))
                    } else {
                        var inputValid: Boolean = true
                        val list = mutableListOf<UUID>()
                        input.members.forEach {
                            if (!it.validUUID()) {
                                inputValid = false
                                call.respond(HttpStatusCode.BadRequest, call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id")))
                            } else {
                                list.add(input.members.indexOf(it), UUID.fromString(it))
                            }
                        }
                        if (inputValid) {
                            val id = UUID.randomUUID()
                            val group = Group(id, input.title, account.id, list)
                            DB.pushGroup(group)
                        }
                    }
                }
            }
        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}
