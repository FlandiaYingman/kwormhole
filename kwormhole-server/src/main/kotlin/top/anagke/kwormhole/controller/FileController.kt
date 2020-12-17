package top.anagke.kwormhole.controller

import org.apache.tomcat.util.http.parser.ContentRange
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.function.ServerResponse
import top.anagke.kwormhole.model.FileContentStore
import top.anagke.kwormhole.model.FileMetadata
import top.anagke.kwormhole.model.FileMetadataRepo

@Controller
@RequestMapping("/files")
class FileController(
    val metadataRepo: FileMetadataRepo,
    val contentStore: FileContentStore
) {

    private val log = mu.KotlinLogging.logger {}


    // TODO: NOT TESTED 2020/12/16
    @GetMapping("/{*path}", params = ["method=metadata"])
    private fun getFileMetadata(
        @PathVariable path: String,
        @RequestParam start: Int = 0,
        @RequestParam limit: Int = Int.MAX_VALUE
    ): ServerResponse {
        log.info { "GET file metadata $path; start=$start, limit=$limit" }
        return when {
            metadataRepo.existsFile(path) ->
                ServerResponse.ok()
                    .contentType(APPLICATION_JSON)
                    .body(metadataRepo.getFileMetadata(path)!!)
            metadataRepo.existsFolder(path) ->
                ServerResponse.ok()
                    .contentType(APPLICATION_JSON)
                    .body(metadataRepo.getFolderMetadata(path, start, limit))
            else ->
                ServerResponse.notFound().build()
        }
    }

    // TODO: NOT TESTED 2020/12/16
    @GetMapping("/{*path}", params = ["method=content"])
    private fun getFileContent(
        @PathVariable path: String
    ): ServerResponse {
        log.info { "GET file content $path" }
        return when {
            contentStore.existsFile(path) -> {
                val resource = contentStore.getFile(path)!!
                ServerResponse.ok()
                    .contentType(APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .body(resource)
            }
            else ->
                ServerResponse.notFound().build()
        }
    }

    // TODO: NOT TESTED 2020/12/16
    @PutMapping("/{*path}", params = ["method=metadata"])
    private fun putFile(
        @PathVariable path: String,
        @RequestBody metadata: FileMetadata
    ): ServerResponse {
        log.info { "PUT file metadata $path, $metadata;" }
        metadataRepo.putFile(path, metadata)
        return ServerResponse.ok().build()
    }

    // TODO: NOT TESTED 2020/12/16
    @PutMapping("/{*path}", params = ["method=content"])
    private fun putFile(
        @PathVariable path: String,
        @RequestBody bytes: ByteArray,
    ): ServerResponse {
        log.info { "PUT file content $path, length=${bytes.size};" }
        contentStore.storeFile(path, bytes)
        return ServerResponse.ok().build()
    }

    // TODO: NOT TESTED 2020/12/17
    @PutMapping("/{*path}", params = ["method=content-ranged"])
    private fun putFile(
        @PathVariable path: String,
        @RequestBody bytes: ByteArray,
        @RequestHeader contentRange: ContentRange
    ): ServerResponse {
        log.info { "PUT file content $path, range=$contentRange;" }
        if (bytes.size != contentRange.length.toInt())
            return ServerResponse.badRequest().build()

        contentStore.storePart(path, bytes, contentRange)
        return ServerResponse.ok().build()
    }

}