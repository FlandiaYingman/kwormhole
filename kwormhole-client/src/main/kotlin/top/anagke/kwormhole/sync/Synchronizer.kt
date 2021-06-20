package top.anagke.kwormhole.sync

import mu.KotlinLogging
import top.anagke.kwormhole.model.Model
import top.anagke.kwormhole.util.TempFiles
import java.io.Closeable
import java.io.File
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class Synchronizer(
    private val srcModel: Model,
    private val dstModel: Model,
) : Closeable {

    private val tempDir = File("TEMP")

    private val logger = KotlinLogging.logger { }

    private val runner: Thread = thread(start = false, block = this::run)

    private val barrier = CyclicBarrier(2)

    init {
        TempFiles.register(tempDir)

        runner.start()
        barrier.await()
    }


    private fun run() {
        try {
            barrier.await()
            while (!Thread.interrupted()) {
                loop()
            }
        } catch (ignore: InterruptedException) {
        } finally {
        }
    }

    private fun loop() {
        val change = srcModel.changes.take()
        if (dstModel.acceptable(change)) {
            logger.info { "Sync change from $srcModel to $dstModel: '$change'" }
            srcModel.get(change.path)!!.use { fat ->
                dstModel.put(fat)
            }
        }
    }


    override fun close() {
        runner.interrupt()
    }

}