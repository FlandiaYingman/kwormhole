package top.anagke.kwormhole.controller

import org.hibernate.engine.jdbc.BlobProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import top.anagke.kwormhole.controller.EventController.RecordEvent
import top.anagke.kwormhole.model.ContentEntity
import top.anagke.kwormhole.model.ContentRepository
import top.anagke.kwormhole.model.RecordEntity
import top.anagke.kwormhole.model.RecordRepository
import java.util.concurrent.CopyOnWriteArrayList
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private fun HttpServletRequest.kfrPath() = pathInfo.removePrefix("/kfr")

private fun RecordEntity.toHeadersMap(): Map<String, String> {
    return mapOf(
        "Record-Size" to size.toString(),
        "Record-Time" to time.toString(),
        "Record-Hash" to hash.toString(),
    )
}

private fun Map<String, String>.fromHeadersMap(path: String): RecordEntity {
    val map = this.mapKeys { it.key.toLowerCase() }
    return RecordEntity(
        path,
        map["Record-Size".toLowerCase()]!!.toLong(),
        map["Record-Time".toLowerCase()]!!.toLong(),
        map["Record-Hash".toLowerCase()]!!.toLong(),
    )
}

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "KFR not found")
class KfrNotFoundException(pathNotFound: String) : Exception("$pathNotFound not found")

@RestController
internal class KwormController(
    private val recordRepo: RecordRepository,
    private val contentRepo: ContentRepository
) {

    @RequestMapping("/all", method = [RequestMethod.GET])
    fun all(): List<String> {
        return recordRepo.findAll()
            .map(RecordEntity::path)
    }


    @RequestMapping("/kfr/**", method = [RequestMethod.HEAD])
    fun head(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val path = request.kfrPath()
        val record = recordRepo.findById(path).orElseThrow { KfrNotFoundException(path) }
        record.toHeadersMap().forEach { (name, value) -> response.addHeader(name, value) }
    }

    @RequestMapping("/kfr/**", method = [RequestMethod.GET])
    fun get(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ByteArray {
        val path = request.kfrPath()
        println(recordRepo.findAll())
        val record = recordRepo.findById(path).orElseThrow { KfrNotFoundException(path) }
        record.toHeadersMap().forEach { (name, value) -> response.addHeader(name, value) }
        return contentRepo.findById(path).get().content.binaryStream.use { it.readBytes() }
    }


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @PutMapping("/kfr/**")
    fun put(
        request: HttpServletRequest,
        @RequestHeader headers: HttpHeaders,
        @RequestBody body: ByteArray,
    ): ResponseEntity<Unit> {
        val path = request.kfrPath()
        val response = if (recordRepo.existsById(path)) {
            ResponseEntity<Unit>(HttpStatus.OK)
        } else {
            ResponseEntity<Unit>(HttpStatus.CREATED)
        }
        recordRepo.save(headers.mapValues { it.value.first() }.fromHeadersMap(path))
        contentRepo.save(ContentEntity(path, BlobProxy.generateProxy(body)))
        eventPublisher.publishEvent(RecordEvent(this, path))
        return response
    }

}

@Controller
class EventController {

    class RecordEvent(source: Any, val path: String) : ApplicationEvent(source)


    private val emitterList: MutableList<SseEmitter> = CopyOnWriteArrayList()


    @GetMapping("/event")
    fun listen(): SseEmitter {
        val emitter = SseEmitter()
        emitterList += emitter
        return emitter
    }

    @EventListener
    fun handleEvent(event: RecordEvent) {
        emitterList.forEach { emitter ->
            try {
                val eventBuilder = SseEmitter.event()
                    .data(event.path)
                emitter.send(eventBuilder)
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }
    }

}