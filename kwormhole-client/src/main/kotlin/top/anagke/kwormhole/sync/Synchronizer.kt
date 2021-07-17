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
        val change = srcModel.changes.take()
        try {
            if (dstModel.acceptable(change)) {
                logger.info { "Sync change from $srcModel to $dstModel: '$change'" }
                val srcKfr = srcModel.get(change.path)
                dstModel.put(srcKfr!!)
            }
        } catch (e: Exception) {
            if (e is InterruptedException) throw e
            logger.warn(e) { "Sync error" }
            srcModel.changes.put(change)
        }
    }


    override fun close() {
        runner.interrupt()
    }

}