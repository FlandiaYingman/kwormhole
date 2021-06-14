package top.anagke.kwormhole.controller

import com.google.gson.Gson
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.util.UriComponentsBuilder
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.service.KfrService
import java.util.*


@Component
class WSHandler : TextWebSocketHandler(), ApplicationListener<RepoEvent> {

    private val logger = KotlinLogging.logger { }


    @Autowired
    private lateinit var kfrService: KfrService

    private val sessions: MutableList<WebSocketSession> = Collections.synchronizedList(mutableListOf())


    override fun afterConnectionEstablished(session: WebSocketSession) {
        synchronized(sessions) {
            sessions.add(session)
        }
        val all = kfrService.all()
        all.forEach {
            session.sendRecord(it)
        }
    }

    override fun onApplicationEvent(event: RepoEvent) {
        synchronized(sessions) {
            sessions.removeIf { it.isOpen.not() }
            sessions.forEach { session ->
                if (session.isOpen) {
                    session.sendRecord(event.record)
                }
            }
        }
    }


    private fun WebSocketSession.sendRecord(record: Kfr) {
        val uriComponents = UriComponentsBuilder.fromUri(uri!!).build()
        val before = uriComponents.queryParams["before"]?.first()?.toLong() ?: Long.MAX_VALUE
        val after = uriComponents.queryParams["after"]?.first()?.toLong() ?: Long.MIN_VALUE
        if (record.time in (after until before)) {
            logger.info { "Send record $record to $remoteAddress through websocket" }
            this.sendMessage(TextMessage(Gson().toJson(record)))
        }
    }

}

class RepoEvent(
    source: Any,
    val record: Kfr
) : ApplicationEvent(source)