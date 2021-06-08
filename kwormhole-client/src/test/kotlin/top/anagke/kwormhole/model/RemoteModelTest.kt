package top.anagke.kwormhole.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kwormhole.MockKfr
import top.anagke.kwormhole.MockKWormholeServer
import top.anagke.kwormhole.test.TEST_DIR
import top.anagke.kwormhole.test.pollNonnull
import top.anagke.kwormhole.test.useDir
import java.util.concurrent.TimeUnit

@Timeout(5, unit = TimeUnit.SECONDS)
internal class RemoteModelTest {

    @Test
    fun init_get() {
        TEST_DIR.useDir {
            MockKWormholeServer().use { server ->
                val (mockKfr, mockBytes) = server.mockPair()
                RemoteModel(server.host, server.port).use { model ->
                    model.open()

                    val change = model.changes.take()
                    val kfr = model.getRecord(mockKfr.path)
                    val (_, bytes) = model.getContent(mockKfr.path)
                    assertEquals(mockKfr, change)
                    assertEquals(mockKfr, kfr)
                    assertArrayEquals(mockBytes, bytes)
                }
            }
        }
    }

    @Test
    fun poll_get() {
        TEST_DIR.useDir {
            MockKWormholeServer().use { server ->
                val (dummy, _) = server.mockPair()
                RemoteModel(server.host, server.port).use { model ->
                    model.open()
                    assertEquals(dummy, model.changes.take())

                    val (mockKfr, mockBytes) = server.mockPair()
                    val change = model.changes.take()
                    val kfr = model.getRecord(mockKfr.path)
                    val (_, bytes) = model.getContent(mockKfr.path)
                    assertEquals(mockKfr, change)
                    assertEquals(mockKfr, kfr)
                    assertArrayEquals(mockBytes, bytes)
                }
            }
        }
    }

    @Test
    fun put_get() {
        TEST_DIR.useDir {
            MockKWormholeServer().use { server ->
                RemoteModel(server.host, server.port).use { model ->
                    model.open()

                    val (mockKfr, mockBytes) = MockKfr.mockPair()
                    model.put(mockKfr, mockBytes)

                    pollNonnull { model.getRecord(mockKfr.path) }

                    val kfr = model.getRecord(mockKfr.path)
                    val (_, bytes) = model.getContent(mockKfr.path)
                    assertEquals(mockKfr, kfr)
                    assertArrayEquals(mockBytes, bytes)

                    val change = model.changes.poll()
                    assertNull(change)
                }
            }
        }
    }

}