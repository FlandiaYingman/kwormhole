package top.anagke.kwormhole.test

import io.ktor.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer

fun testServer(module: Application.() -> Unit): CIOApplicationEngine {
    return embeddedServer(CIO, port = TEST_SERVER_PORT, module = module)
}

inline fun ApplicationEngine.use(block: () -> Unit) {
    try {
        start()
        block()
    } finally {
        stop(0, 0)
    }
}