@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package top.anagke.kwormhole.local

import mu.KotlinLogging
import top.anagke.kwormhole.FileRecord
import java.io.Closeable
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class LocalModel(
    val localDir: File,
) : Closeable {

    private val logger = KotlinLogging.logger { }

    private val runner: Thread = thread {
        try {
            run()
        } catch (ignore: InterruptedException) {
        }
    }

    private val lock = CountDownLatch(1)

    companion object {
        fun newLocalModel(file: File): LocalModel {
            val localModel = LocalModel(file)
            localModel.lock.await()
            return localModel
        }
    }

    private lateinit var monitor: FileSystemMonitor

    private fun run() {
        monitor = FileSystemMonitor(listOf(localDir))
        updateAll()

        lock.countDown()
        while (!Thread.interrupted()) {
            loop()
        }
    }

    private fun loop() {
        monitor.take().forEach { watchEvent ->
            val event = FileChangeEvent.from(localDir, watchEvent)
            updateFile(event.file)
        }
    }

    private fun updateAll() {
        val allFiles = localDir.walk().filter { it != localDir }.toList()
        allFiles.forEach(this::updateFile)
    }

    private fun updateFile(file: File) {
        val newRecord = FileRecord.byFile(localDir, file)
        val oldRecord = records[newRecord.path]
        if (newRecord.differTo(oldRecord)) {
            recordsMutable[newRecord.path] = newRecord
            changes.offer(newRecord)
            logger.info("Update '${newRecord.path}': $newRecord")
        }
    }

    val records: Map<String, FileRecord> by lazy { recordsMutable }
    private val recordsMutable: MutableMap<String, FileRecord> by lazy { HashMap() }

    val changes: BlockingQueue<FileRecord> = LinkedBlockingQueue()


    override fun close() {
        runner.interrupt()
    }

}