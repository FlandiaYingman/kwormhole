package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.MockKwormholeServer
import top.anagke.kwormhole.asBytes

internal class RemoteModelTest {

    @Test
    fun testRemoteModel() {
        MockKwormholeServer().use { server ->
            val (expectedRecord1, expectedContent1) = server.mockBoth()
            RemoteModel(server.host, server.port).use { model ->
                val path1 = model.changes.take()
                val actualRecord1 = model.records[path1]!!
                val actualContent1 = model.contents[path1]!!.invoke()
                Assertions.assertEquals(expectedRecord1, actualRecord1)
                Assertions.assertArrayEquals(expectedContent1.asBytes(), actualContent1.asBytes())

                val (expectedRecord2, expectedContent2) = server.mockBoth()
                val path2 = model.changes.take()
                val actualRecord2 = model.records[path2]!!
                val actualContent2 = model.contents[path2]!!.invoke()
                Assertions.assertEquals(expectedRecord2, actualRecord2)
                Assertions.assertArrayEquals(expectedContent2.asBytes(), actualContent2.asBytes())

                server.changes.take() // clean last 2 requests
                server.changes.take()

                val (expectedRecord3, expectedContent3) = MockKfr.mockBoth()
                model.put(expectedRecord3, expectedContent3)
                val actualRecord3 = server.changes.take()
                val actualContent3 = server.contents[server.records.indexOf(actualRecord3)]
                Assertions.assertEquals(expectedRecord3, actualRecord3)
                Assertions.assertArrayEquals(expectedContent3.asBytes(), actualContent3.asBytes())
            }
        }
    }

}