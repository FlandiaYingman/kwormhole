package top.anagke.kwormhole.sync

import mu.KotlinLogging
import top.anagke.kwormhole.model.Model
import java.io.Closeable
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class Synchronizer(
    private val srcModel: Model,
    private val dstModel: Model,
) : Closeable {

    private val logger = KotlinLogging.logger { }

    private val runner: Thread = thread(start = false, block = this::run)

    private val barrier = CyclicBarrier(2)

    init {
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
        val srcChange = srcModel.changes.take()
        val srcChangeRecord = srcModel.records[srcChange]!!
        val srcChangeContent = srcModel.contents[srcChange]!!
        dstModel.put(srcChangeRecord, srcChangeContent())
        logger.info { "Sync change from $srcModel to $dstModel: '$srcChange'" }
    }


    override fun close() {
        runner.interrupt()
    }

}