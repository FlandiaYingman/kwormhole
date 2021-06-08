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
        val srcChange = srcModel.changes.take()
        val path = srcChange.path

        val srcChangeRecord = srcModel.getRecord(path)
        if (srcChangeRecord == null) {
            return
        }
        if (srcChange != srcChangeRecord) return


        val srcChangeContentPath = TempFiles.allocTempFile(tempDir)
        val srcChangeContent = srcModel.getContent(path, srcChangeContentPath)
        if (srcChangeContent == null) {
            return
        }
        if (srcChangeRecord != srcChangeContent) return


        dstModel.put(srcChangeRecord, srcChangeContentPath)
        logger.info { "Sync change from $srcModel to $dstModel: '$srcChange'" }

        TempFiles.freeTempFile(srcChangeContentPath)
    }


    override fun close() {
        runner.interrupt()
    }

}