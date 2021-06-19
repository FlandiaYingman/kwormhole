package top.anagke.kwormhole.controller

import mu.KotlinLogging
import okio.ByteString.Companion.toByteString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.Kfr.Companion.HASH_HEADER_NAME
import top.anagke.kwormhole.Kfr.Companion.SIZE_HEADER_NAME
import top.anagke.kwormhole.Kfr.Companion.TIME_HEADER_NAME
import top.anagke.kwormhole.service.KfrService
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private fun HttpServletRequest.kfrPath() = this.servletPath.removePrefix("/kfr")

private fun Kfr.toHttpHeaders(): Map<String, String> {
    return mapOf(
        SIZE_HEADER_NAME to size.toString(),
        TIME_HEADER_NAME to time.toString(),
        HASH_HEADER_NAME to hash.toString(),
    )
}

private fun Map<String, String>.fromHeadersMap(path: String): Kfr {
    val mapCaseInsensitive = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).also { it.putAll(this) }
    val size = mapCaseInsensitive[SIZE_HEADER_NAME]!!.toLong()
    val time = mapCaseInsensitive[TIME_HEADER_NAME]!!.toLong()
    val hash = mapCaseInsensitive[HASH_HEADER_NAME]!!.toLong()
    return Kfr(path, time, size, hash)
}

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "KFR not found")
class KfrNotFoundException(pathNotFound: String) : Exception("$pathNotFound not found")


@RestController
internal class KfrController(
    private val kfrService: KfrService
) {

    private val logger = KotlinLogging.logger { }


    @RequestMapping("/kfr/**", method = [RequestMethod.HEAD])
    fun head(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val path = request.kfrPath()
        val record = kfrService.head(path) ?: throw KfrNotFoundException(path)
        record.toHttpHeaders().forEach { (name, value) -> response.addHeader(name, value) }

        logger.info { "HEAD ${request.servletPath}, response $record" }
    }

    @RequestMapping("/kfr/**", method = [RequestMethod.GET])
    fun get(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ByteArray {
        val path = request.kfrPath()
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)
        fat.kfr.toHttpHeaders().forEach { (name, value) -> response.addHeader(name, value) }

        logger.info { "GET ${request.servletPath}, response ${fat.kfr} and content" }
        return fat.body()?.toByteArray() ?: byteArrayOf()
    }


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @PutMapping("/kfr/**")
    fun put(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestHeader headers: HttpHeaders,
        @RequestBody(required = false) body: ByteArray?,
    ) {
        val path = request.kfrPath()
        response.status = (if (path in kfrService) HttpStatus.OK else HttpStatus.CREATED).value()

        val record = headers
            .mapValues { it.value.first() }
            .fromHeadersMap(path)
        val content = body ?: byteArrayOf()
        val fat = FatKfr(record, content.toByteString())
        kfrService.put(fat)
        eventPublisher.publishEvent(RepoEvent(this, record))

        logger.info { "PUT ${request.servletPath}, request $record and content" }
    }

}