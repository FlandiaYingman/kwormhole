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

class CreateTest {

    @BeforeEach
    fun setUp() = IntegrationTesting.setup()

    @AfterEach
    fun tearDown() = IntegrationTesting.teardown()


    @Test
    @Timeout(2, unit = MINUTES)
    fun test_create_basic() {
        test_create(1, 4.KiB)
    }

    @Test
    @Timeout(2, unit = MINUTES)
    fun test_create_large() {
        test_create(1, 64.MiB)
    }

    @Test
    @Timeout(2, unit = MINUTES)
    fun test_create_many() {
        test_create(256, 4.KiB)
    }

    @Test
    @Timeout(5, unit = MINUTES)
    fun test_create_many_large() {
        test_create(16, 64.MiB)
    }


    @Test
    @Timeout(2, unit = MINUTES)
    fun test_create_empty() {
        test_create(1, 0.KiB)
    }

    @Test
    @Timeout(2, unit = MINUTES)
    fun test_create_many_empty() {
        test_create(64, 0.KiB)
    }


    private fun test_create(fileNum: Int, fileSize: Int) {
        repeat(fileNum) {
            mockOnFile(client1Root, fileSize = fileSize)
        }
        while (rootsEqual(client1Root, client2Root).not()) {
            Thread.sleep(1000)
        }
    }

}

