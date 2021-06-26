package top.anagke.kwormhole.controller

import mu.KotlinLogging
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
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.ThinKfr
import top.anagke.kwormhole.isSingle
import top.anagke.kwormhole.merge
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
        form.add("kfr", thin.kfr)
        form.add("number", thin.number)
        form.add("count", thin.total)
        form.add("range", thin.range)
        if (body) form.add("body", thin.body)
        return form
    }


    @GetMapping("/kfr/{*path}", produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun get(
        @PathVariable path: String,
        @RequestParam number: Int,
        @RequestParam body: Boolean,
    ): ResponseEntity<MultiValueMap<String, Any>> {
        val fat = kfrService.get(path) ?: throw KfrNotFoundException(path)
        fat.use {
            val thin = fat.slice(8.MiB, number)
            val form = fillForm(thin, body = body)

            logger.info { "GET KFR '$path': ${fat.kfr}" }
            return ResponseEntity(form, HttpStatus.OK)
        }
    }

    @PutMapping("/kfr/{*path}")
    fun put(
        @PathVariable path: String,
        @RequestPart kfr: Kfr,
        @RequestPart number: Int,
        @RequestPart count: Int,
        @RequestPart range: ThinKfr.Range,
        @RequestPart body: ByteArray?,
    ): HttpStatus {
        val thin = ThinKfr(kfr, number, count, range, body ?: byteArrayOf())
        val fat = if (thin.isSingle()) {
            thin.merge()
        } else {
            thinService.merge(thin)
        }
        fat.use {
            if (fat == null) {
                return HttpStatus.ACCEPTED
            } else {
                kfrService.put(fat)
                eventPublisher.publishEvent(KfrEvent(this, fat.kfr))

                logger.info { "PUT KFR '$path': ${fat.kfr}" }
                return HttpStatus.OK
            }
        }
    }

}