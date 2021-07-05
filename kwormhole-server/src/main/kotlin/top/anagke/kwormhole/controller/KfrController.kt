package top.anagke.kwormhole.controller

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import top.anagke.kio.MiB
import top.anagke.kwormhole.*
import top.anagke.kwormhole.service.KfrService

@RestController
class KfrController(
    private val kfrService: KfrService,
) {

    private val logger = KotlinLogging.logger { }

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher


    private fun fillForm(thin: ThinKfr, body: Boolean): LinkedMultiValueMap<String, Any> {
        val form = LinkedMultiValueMap<String, Any>()
        form.add("kfr", Kfr(thin))
        form.add("range", thin.range)
        form.add("progress", thin.progress)
        if (body) form.add("body", thin.move())
        return form
    }


    @GetMapping("/kfr/{*path}", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun get(
        @PathVariable path: String,
        @RequestParam number: Int,
        @RequestParam body: Boolean,
    ): ResponseEntity<MultiValueMap<String, Any>> {
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)

        val thin = fat.slicer(8.MiB).use {
            it.position = number
            it.next()
        }
        val form = fillForm(thin, body = body)

        logger.info { "GET KFR '$path': $thin" }
        return ResponseEntity(form, HttpStatus.OK)
    }

    @PutMapping("/kfr/{*path}")
    fun put(
        @PathVariable path: String,
        @RequestPart kfr: Kfr,
        @RequestPart range: Range,
        @RequestPart progress: Progress,
        @RequestPart body: ByteArray?,
    ): HttpStatus {
        val thin = if (kfr.exists()) unsafeThinKfr(kfr, range, progress, body ?: byteArrayOf()) else absentThinKfr(kfr)
        val fat = kfrService.put(thin)
        if (fat != null) {
            eventPublisher.publishEvent(KfrEvent(this, Kfr(fat)))
        }
        logger.info { "PUT KFR '$path': $thin" }
        return if (fat != null) HttpStatus.OK else HttpStatus.ACCEPTED
    }

}