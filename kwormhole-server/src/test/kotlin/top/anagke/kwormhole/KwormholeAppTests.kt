package top.anagke.kwormhole

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForObject
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpMethod.GET
import top.anagke.kwormhole.model.KwormFile
import top.anagke.kwormhole.model.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.model.store.KwormStore
import top.anagke.kwormhole.util.KiB
import top.anagke.kwormhole.util.hash
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class KwormholeAppTests {

    companion object {

        private const val TEST_COUNT = 8

        private const val TEST_DEPTH = 8

        private val TEST_SIZE = 4.KiB

    }


    @LocalServerPort
    private var port: Int? = null

    private fun path(path: String, vararg params: Pair<String, String>): String {
        return if (params.isEmpty()) {
            "http://localhost:$port$path"
        } else {
            val paramString = params.joinToString("&") { (name, value) ->
                "$name=$value"
            }
            "http://localhost:$port$path?$paramString"
        }
    }


    @Autowired private lateinit var restTemplate: TestRestTemplate

    private lateinit var testContentList: List<ByteArray>

    private lateinit var testMetadataList: List<KwormFile>

    private lateinit var testData: List<Pair<KwormFile, ByteArray>>


    @BeforeEach
    fun doPut() {
        testContentList = Random.nextBytesList(TEST_COUNT, TEST_SIZE)
        testMetadataList = List(TEST_COUNT) { index ->
            KwormFile(Random.nextPath(TEST_DEPTH), testContentList[index].hash(), Random.nextLong(utcTimeMillis))
        }
        testData = testMetadataList.zip(testContentList)
        testData.forEach { (metadata, content) ->
            restTemplate.put(path(metadata.path, "method" to "metadata"), metadata)
            restTemplate.put(path(metadata.path, "method" to "content"), content)
        }
    }

    @AfterEach
    fun doDelete() {
        testData.forEach { (metadata, _) ->
            restTemplate.delete(path(metadata.path))
        }
    }


    @Test
    fun testCount() {
        val count = restTemplate.getForObject<Int>(path("count"))
        assertThat(count)
            .isEqualTo(testData.size)
    }

    @Test
    fun testList() {
        val list = restTemplate.exchange<List<KwormFile>>(path("list"), GET).body
        assertThat(list)
            .containsAll(testMetadataList)
    }

    @Test
    fun testGet() {
        testData.forEach { (metadata, content) ->
            val actualMetadata = restTemplate.getForObject<KwormFile>(path(metadata.path, "method" to "metadata"))
            val actualContent = restTemplate.getForObject<ByteArray>(path(metadata.path, "method" to "content"))
            assertThat(actualMetadata)
                .isEqualTo(metadata)
            assertThat(actualContent)
                .isEqualTo(content)
        }
    }


    @AfterAll
    fun clean() {
        KwormStore.DEFAULT_CONTENT_LOCATION.delete()
    }

}
