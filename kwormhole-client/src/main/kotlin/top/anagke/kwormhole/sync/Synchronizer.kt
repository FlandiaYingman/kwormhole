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
        if (dstModel.validate(change)) {
            logger.info { "Sync change from $srcModel to $dstModel: '$change'" }
            val kfrContent = srcModel.getContent(change.path)!!
            dstModel.put(kfrContent)
        }
    }


    override fun close() {
        runner.interrupt()
    }

}