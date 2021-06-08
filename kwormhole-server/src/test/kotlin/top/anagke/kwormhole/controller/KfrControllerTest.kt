package top.anagke.kwormhole.controller

import com.google.gson.Gson
import org.hibernate.engine.jdbc.BlobProxy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import top.anagke.kwormhole.dao.ContentEntity
import top.anagke.kwormhole.dao.ContentRepository
import top.anagke.kwormhole.dao.RecordEntity
import top.anagke.kwormhole.dao.RecordRepository
import top.anagke.kwormhole.util.Hasher
import kotlin.random.Random

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class KfrControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var recordRepo: RecordRepository

    @Autowired
    private lateinit var contentRepo: ContentRepository


    private fun createKfr(seed: Int, path: String): Pair<RecordEntity, ContentEntity> {
        val random = Random(seed)
        val content = random.nextBytes(256)
        val size = content.size.toLong()
        val time = random.nextLong(0, Long.MAX_VALUE)
        val hash = Hasher.hash(content)
        return RecordEntity(path, size, time, hash) to ContentEntity(path, BlobProxy.generateProxy(content))
    }


    @Test
    fun all_shouldReturnEmptyList() {
        mvc.perform(get("/all"))
            .andExpect(status().isOk)
            .andExpect(content().json(Gson().toJson(emptyList<Unit>())))
    }

    @Test
    fun all_shouldReturnPathList() {
        val kfrs = List(8) { createKfr(it, "/foo/bar/$it") }
        kfrs.forEach { (record, content) ->
            recordRepo.save(record)
            contentRepo.save(content)
        }
        val paths = kfrs.map { it.first.path }
        mvc.perform(get("/all"))
            .andExpect(status().isOk)
            .andExpect(content().json(Gson().toJson(paths)))
    }

    @Test
    fun head_shouldReturnHeaders() {
        val (record, content) = createKfr(0, "/foo/bar")
        recordRepo.save(record)
        contentRepo.save(content)
        mvc.perform(head("/kfr/foo/bar"))
            .andExpect(status().isOk)
            .andExpect(header().string("Record-Size", record.size.toString()))
            .andExpect(header().string("Record-Time", record.time.toString()))
            .andExpect(header().string("Record-Hash", record.hash.toString()))
    }

    @Test
    fun head_shouldReturnNotFound() {
        mvc.perform(head("/kfr/foo/bar"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun get_shouldReturnHeadersAndBody() {
        val (record, content) = createKfr(0, "/foo/bar")
        recordRepo.save(record)
        contentRepo.save(content)
        mvc.perform(get("/kfr/foo/bar"))
            .andExpect(status().isOk)
            .andExpect(header().string("Record-Size", record.size.toString()))
            .andExpect(header().string("Record-Time", record.time.toString()))
            .andExpect(header().string("Record-Hash", record.hash.toString()))
            .andExpect(content().bytes(content.content.binaryStream.use { it.readBytes() }))
    }

    @Test
    fun get_shouldReturnNotFound() {
        mvc.perform(get("/kfr/foo/bar"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun put_shouldReturnCreated() {
        val (record, content) = createKfr(0, "/foo/bar")
        mvc.perform(
            put("/kfr/foo/bar")
                .header("Record-Size", record.size.toString())
                .header("Record-Time", record.time.toString())
                .header("Record-Hash", record.hash.toString())
                .content(content.content.binaryStream.use { it.readBytes() })
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun put_shouldReturnOK() {
        val (record, content) = createKfr(0, "/foo/bar")
        recordRepo.save(record)
        contentRepo.save(content)
        mvc.perform(
            put("/kfr/foo/bar")
                .header("Record-Size", record.size.toString())
                .header("Record-Time", record.time.toString())
                .header("Record-Hash", record.hash.toString())
                .content(content.content.binaryStream.use { it.readBytes() })
        )
            .andExpect(status().isOk)
    }

}