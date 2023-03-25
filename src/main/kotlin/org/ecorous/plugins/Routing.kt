package org.ecorous.plugins

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.application.*
import org.ecorous.database.DB

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(mapOf("error" to "exception caught: $cause", "stacktrace" to cause.stackTrace))
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
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}
