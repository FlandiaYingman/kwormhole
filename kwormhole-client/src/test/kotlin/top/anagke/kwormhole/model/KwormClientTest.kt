package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.MockKwormholeServer
import top.anagke.kwormhole.asBytes

internal class KwormClientTest {

    @Test
    fun allWs() {
        MockKwormholeServer().use { server ->
            val client = KwormClient(server.host, server.port)
            val expected1 = server.mockRecord()
            client.wsAll().use { session ->
                val actual1 = session.buf.take()
                Assertions.assertEquals(expected1, actual1)

                val expected2 = server.mockRecord()
                val actual2 = session.buf.take()
                Assertions.assertEquals(expected2, actual2)

                val expected3 = server.mockRecord()
                val actual3 = session.buf.take()
                Assertions.assertEquals(expected3, actual3)
            }
        }
    }

    @Test
    fun head() {
        MockKwormholeServer().use { server ->
            val client = KwormClient(server.host, server.port)

            val expected1 = server.mockRecord()
            val actual1 = client.head(expected1.path)
            Assertions.assertEquals(expected1, actual1)

            val expected2 = server.mockRecord()
            val actual2 = client.head(expected2.path)
            Assertions.assertEquals(expected2, actual2)
        }
    }

    @Test
    fun get() {
        MockKwormholeServer().use { server ->
            val client = KwormClient(server.host, server.port)

            val (expectedRecord1, expectedContent1) = server.mockBoth()
            val (actualRecord1, actualContent1) = client.get(expectedRecord1.path)
            Assertions.assertEquals(expectedRecord1, actualRecord1)
            Assertions.assertArrayEquals(expectedContent1.asBytes(), actualContent1.asBytes())

            val (expectedRecord2, expectedContent2) = server.mockBoth()
            val (actualRecord2, actualContent2) = client.get(expectedRecord2.path)
            Assertions.assertEquals(expectedRecord2, actualRecord2)
            Assertions.assertArrayEquals(expectedContent2.asBytes(), actualContent2.asBytes())
        }
    }

    @Test
    fun put() {
        MockKwormholeServer().use { server ->
            val client = KwormClient(server.host, server.port)

            val (expectedRecord1, expectedContent1) = MockKfr.mockBoth()
            client.put(expectedRecord1, expectedContent1)
            val (actualRecord1, actualContent1) = server.records.last() to server.contents.last()
            Assertions.assertEquals(expectedRecord1, actualRecord1)
            Assertions.assertArrayEquals(expectedContent1.asBytes(), actualContent1.asBytes())

            val (expectedRecord2, expectedContent2) = MockKfr.mockBoth()
            client.put(expectedRecord2, expectedContent2)
            val (actualRecord2, actualContent2) = server.records.last() to server.contents.last()
            Assertions.assertEquals(expectedRecord2, actualRecord2)
            Assertions.assertArrayEquals(expectedContent2.asBytes(), actualContent2.asBytes())
        }
    }

}