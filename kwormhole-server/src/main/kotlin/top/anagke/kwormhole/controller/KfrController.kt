package top.anagke.kwormhole.controller

import mu.KotlinLogging
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import top.anagke.kio.MiB
import top.anagke.kwormhole.Fraction
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.Range
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.service.KfrService
import top.anagke.kwormhole.service.ThinService
import top.anagke.kwormhole.slice

@RestController
class KfrController(
    private val kfrService: KfrService,
    private val thinService: ThinService,
) {

    private val logger = KotlinLogging.logger { }

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher


    private fun fillForm(thin: ThinKfr, body: Boolean): LinkedMultiValueMap<String, Any> {
        val form = LinkedMultiValueMap<String, Any>()
        form.add("kfr", Kfr(thin))
        form.add("range", thin.range)
        form.add("progress", thin.progress)
        if (body) form.add("body", thin.body!!.toByteArray())
        return form
    }


    @GetMapping("/kfr/{*path}", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun get(
        @PathVariable path: String,
        @RequestParam number: Int,
        @RequestParam body: Boolean,
    ): ResponseEntity<MultiValueMap<String, Any>> {
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)

        val thin = fat.slice(8.MiB, number.toLong())
        val form = fillForm(thin, body = body)

        logger.info { "GET KFR '$path': $thin" }
        return ResponseEntity(form, HttpStatus.OK)
    }

    @PutMapping("/kfr/{*path}")
    fun put(
        @PathVariable path: String,
        @RequestPart kfr: Kfr,
        @RequestPart range: Range,
        @RequestPart progress: Fraction,
        @RequestPart body: ByteArray?,
    ): HttpStatus {
        val thin = if (kfr.exists()) ThinKfr(kfr, range, progress, body?.toByteString() ?: EMPTY) else ThinKfr(kfr)
        val fat = kfrService.put(thin)
        if (fat != null) {
            eventPublisher.publishEvent(KfrEvent(this, Kfr(fat)))
        }
        logger.info { "PUT KFR '$path': $thin" }
        return if (fat != null) HttpStatus.OK else HttpStatus.ACCEPTED
    }

}