package top.anagke.kwormhole.controller

import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import top.anagke.kwormhole.model.KwormFile
import top.anagke.kwormhole.model.store.KwormStore

@RestController
class KwormController(
    val kwormStore: KwormStore
) {

    private val log = mu.KotlinLogging.logger {}

    @GetMapping("/count")
    private fun count(): ResponseEntity<Long> {
        val count = kwormStore.count()
        log.info { "GET: count, result=$count" }
        return ResponseEntity.ok(count)
    }

    @GetMapping("/list")
    private fun list(): ResponseEntity<List<KwormFile>> {
        val list = kwormStore.list()
        log.info { "GET: list, result.size=${list.size}" }
        return ResponseEntity.ok(list)
    }


    @GetMapping("/{*path}", params = ["method=metadata"])
    private fun getMetadata(
        @PathVariable path: String
    ): ResponseEntity<KwormFile> {
        return if (kwormStore.contains(path)) {
            val metadata = kwormStore.getMetadata(path)
            log.info { "GET: get metadata, path=$path, result=$metadata" }
            ResponseEntity.ok()
                .contentType(APPLICATION_JSON)
                .body(metadata)
        } else {
            log.info { "GET: get metadata, path=$path, result=not_found" }
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{*path}", params = ["method=content"])
    private fun getContent(
        @PathVariable path: String
    ): ResponseEntity<ByteArray> {
        return if (kwormStore.contains(path)) {
            val content = kwormStore.getContent(path)
            log.info { "GET: get content, path=$path, result.size=${content.size}" }
            return ResponseEntity.ok()
                .contentType(APPLICATION_OCTET_STREAM)
                .contentLength(content.size.toLong())
                .body(content)
        } else {
            log.info { "GET: get content, path=$path, result=not_found" }
            ResponseEntity.notFound().build()
        }

    }


    @PutMapping("/{*path}", params = ["method=metadata"])
    private fun putMetadata(
        @PathVariable path: String,
        @RequestBody metadata: KwormFile
    ): ResponseEntity<Unit> {
        log.info { "PUT: put metadata, path=$path, metadata=$metadata" }
        kwormStore.putMetadata(path, metadata)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{*path}", params = ["method=content"])
    private fun putContent(
        @PathVariable path: String,
        @RequestBody content: ByteArray
    ): ResponseEntity<Unit> {
        log.info { "PUT: put content, path=$path, content.size=${content.size}" }
        kwormStore.putContent(path, content)
        return ResponseEntity.noContent().build()
    }


    @DeleteMapping("/{*path}")
    private fun delete(
        @PathVariable path: String
    ): ResponseEntity<Unit> {
        return if (kwormStore.contains(path)) {
            kwormStore.delete(path)
            log.info { "DELETE: delete content, path=$path, deleted" }
            ResponseEntity.noContent().build()
        } else {
            log.info { "DELETE: delete content, path=$path, not found" }
            ResponseEntity.notFound().build()
        }
    }

}