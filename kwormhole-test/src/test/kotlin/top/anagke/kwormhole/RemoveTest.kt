package top.anagke.kwormhole

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import top.anagke.kio.KiB
import top.anagke.kio.MiB
import top.anagke.kwormhole.MockKfr.mockOnFile
import top.anagke.kwormhole.testing.IntegrationTesting
import top.anagke.kwormhole.testing.IntegrationTesting.client1Root
import top.anagke.kwormhole.testing.IntegrationTesting.client2Root
import top.anagke.kwormhole.testing.rootsEqual
import java.util.concurrent.TimeUnit.MINUTES

class RemoveTest {

    @BeforeEach
    fun setUp() = IntegrationTesting.setup()

    @AfterEach
    fun tearDown() = IntegrationTesting.teardown()


    @Test
    @Timeout(2, unit = MINUTES)
    fun test_remove_basic() {
        test_remove(1, 4.KiB)
    }

    @Test
    @Timeout(2, unit = MINUTES)
    fun test_remove_large() {
        test_remove(1, 64.MiB)
    }

    @Test
    @Timeout(5, unit = MINUTES)
    fun test_remove_many() {
        test_remove(256, 4.KiB)
    }

    @Test
    @Timeout(5, unit = MINUTES)
    fun test_remove_many_large() {
        test_remove(16, 64.MiB)
    }


    fun test_remove(fileNum: Int, fileSize: Int) {
        val mockFiles = MutableList(fileNum) {
            mockOnFile(client1Root, fileSize = fileSize)
        }
        while (rootsEqual(client1Root, client2Root).not()) {
            Thread.sleep(1000)
        }

        mockFiles.forEach { (_, file) ->
            file.delete()
        }
        while (rootsEqual(client1Root, client2Root).not()) {
            Thread.sleep(1000)
        }
    }

}

