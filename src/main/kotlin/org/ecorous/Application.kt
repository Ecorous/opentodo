package org.ecorous

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ecorous.database.DB
import org.ecorous.plugins.configureRouting
import org.ecorous.plugins.configureSecurity
import org.ecorous.plugins.configureSerialization
import org.ecorous.plugins.configureTemplating

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
    /*routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }*/
    configureSecurity()
    configureSerialization()
    configureTemplating()
    configureRouting()
}
