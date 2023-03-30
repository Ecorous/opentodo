package org.ecorous

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.ecorous.database.DB
import org.ecorous.plugins.configureRouting
import org.ecorous.plugins.configureSecurity

fun main() {
    with(DB) {
        init()
        if (!initialized) {
            error("Database did not initialise! Please check the logs for more information")
        }
    }
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    configureSecurity()
    configureRouting()
}
