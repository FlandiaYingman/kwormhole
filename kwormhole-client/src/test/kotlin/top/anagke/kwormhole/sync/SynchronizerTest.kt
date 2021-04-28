package top.anagke.kwormhole.sync

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.asBytes
import top.anagke.kwormhole.test.TEST_ENTRY_COUNT
import top.anagke.kwormhole.test.nextFileRC
import top.anagke.kwormhole.test.tryPoll
import kotlin.random.Random

internal class SynchronizerTest {

    @Test
    fun test() {
        val testRCs = List(TEST_ENTRY_COUNT) { Random.nextFileRC() }.associateBy { it.first.path }
        val expected = testRCs.keys.toMutableList()

        val srcModel = SimpleModel("src", testRCs.values.toList())
        val dstModel = SimpleModel("dst")

        Synchronizer(srcModel, dstModel).use { _ ->
            repeat(expected.size) { _ ->
                val dstChange = dstModel.changes.tryPoll()
                Assertions.assertNotNull(dstChange)
                val dstChangeRecord = dstModel.records[dstChange]
                val dstChangeContent = dstModel.contents[dstChange]
                Assertions.assertNotNull(dstChangeRecord); dstChangeRecord!!
                Assertions.assertNotNull(dstChangeContent); dstChangeContent!!

                Assertions.assertTrue(expected.remove(dstChange))

                val (testRecord, testContent) = testRCs[dstChange]!!
                Assertions.assertEquals(testRecord, dstChangeRecord)
                Assertions.assertArrayEquals(testContent.asBytes(), dstChangeContent().asBytes())
            }
        }
    }

}