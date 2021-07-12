package top.anagke.kwormhole.controller

import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.HttpStatus.OK
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
    private var eventPublisher: ApplicationEventPublisher,
) {

    private val logger = KotlinLogging.logger { }


    private fun fillForm(thin: ThinKfr): LinkedMultiValueMap<String, Any> {
        val form = LinkedMultiValueMap<String, Any>()
        form.add("kfr", Kfr(thin))
        form.add("range", thin.range)
        form.add("progress", thin.progress)
        if (thin.hasBody()) form.add("body", thin.move())
        return form
    }


    @GetMapping("/kfr/{*path}", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun getKfr(
        @PathVariable path: String,
        @RequestParam position: Int,
    ): ResponseEntity<MultiValueMap<String, Any>> {
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)

        val thin = fat.slice(position, 8.MiB)
        val form = fillForm(thin)

        logger.info { "GET /kfr$path: $thin" }
        return ResponseEntity(form, OK)
    }

    @GetMapping("/kfr_t/{*path}", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun getKfrT(
        @PathVariable path: String,
    ): ResponseEntity<MultiValueMap<String, Any>> {
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)

        val thin = fat.sliceTerminal(8.MiB)
        val form = fillForm(thin)

        logger.info { "GET /kfr_t$path: $thin" }
        return ResponseEntity(form, OK)
    }


    @PutMapping("/kfr/{*path}")
    fun put(
        @PathVariable path: String,
        @RequestPart kfr: Kfr,
        @RequestPart range: Range,
        @RequestPart progress: Progress,
        @RequestPart body: ByteArray?,
    ): HttpStatus {
        val thin = newThinKfr(kfr, range, progress, body)

        val fat = kfrService.put(thin)
        if (fat != null) {
            eventPublisher.publishEvent(KfrEvent(this, Kfr(fat)))
        }

        logger.info { "PUT /kfr$path: $thin" }
        return if (fat != null) OK else ACCEPTED
    }

}