package top.anagke.kwormhole.controller

import com.google.gson.Gson
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import top.anagke.kwormhole.model.KwormFile
import top.anagke.kwormhole.model.store.KwormStore
import top.anagke.kwormhole.util.fromJson
import java.util.*
import javax.servlet.http.Part

@RestController
class KwormController(
    val kwormStore: KwormStore
) {

    private val log = mu.KotlinLogging.logger {}

    @GetMapping("/")
    private fun requestList()
            : ResponseEntity<List<KwormFile>> {
        val list = kwormStore.list()
        log.info { "REQUEST: list, OK" }
        return ResponseEntity.ok(list)
    }


    @GetMapping("/{*path}", params = ["peek="])
    private fun requestPeek(
        @PathVariable path: String
    ): ResponseEntity<KwormFile> {
        return if (kwormStore.contains(path)) {
            val metadata = kwormStore.getMetadata(path)
            log.info { "REQUEST: peek path=$path, OK" }
            ResponseEntity.ok()
                .contentType(APPLICATION_JSON)
                .body(metadata)
        } else {
            log.info { "REQUEST: peek path=$path, Not Found" }
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{*path}")
    private fun requestDownload(
        @PathVariable path: String
    ): ResponseEntity<MultiValueMap<String, HttpEntity<*>>> {
        return if (kwormStore.contains(path)) {
            val metadata = kwormStore.getMetadata(path)
            val content = kwormStore.getContent(path)
            val bodyBuilder = MultipartBodyBuilder()
            bodyBuilder.part("metadata", metadata)
            bodyBuilder.part("content", ByteArrayResource(content))
            log.info { "REQUEST: download path=$path, OK" }
            return ResponseEntity.ok()
                .contentType(MULTIPART_FORM_DATA)
                .body(bodyBuilder.build())
        } else {
            log.info { "REQUEST: download path=$path, Not Found" }
            ResponseEntity.notFound()
                .build()
        }
    }

    @PostMapping("/{*path}")
    private fun requestUpload(
        @PathVariable path: String,
        @RequestParam metadata: String,
        @RequestParam content: Optional<Part>
    ): ResponseEntity<Unit> {
        val metadataObj = Gson().fromJson<KwormFile>(metadata)!!
        kwormStore.putMetadata(path, metadataObj)
        if (metadataObj.hash != null) {
            val contentObj = content.get().inputStream.readBytes()
            kwormStore.putContent(path, contentObj)
        }
        log.info { "REQUEST: upload path=$path, OK" }
        return ResponseEntity.noContent()
            .build()
    }

}