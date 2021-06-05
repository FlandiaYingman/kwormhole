package top.anagke.kwormhole.sync

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.asBytes

internal class SynchronizerTest {

    @Test
    fun test() {
        val testRCs = List(10) { MockKfr.mockBoth() }.associateBy { it.first.path }
        val expected = testRCs.keys.toMutableList()

        val srcModel = SimpleModel("src", testRCs.values.toList())
        val dstModel = SimpleModel("dst")

        Synchronizer(srcModel, dstModel).use { _ ->
            repeat(expected.size) { _ ->
                val dstChange = dstModel.changes.take()
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