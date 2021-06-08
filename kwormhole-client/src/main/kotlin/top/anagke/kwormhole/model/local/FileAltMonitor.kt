package top.anagke.kwormhole.model.local

import top.anagke.kio.notExists
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class FileAltMonitor(
    private val directory: File,
) : Closeable {

    private val runner: Thread

    private val buffer: BlockingQueue<File> = LinkedBlockingQueue()

    init {
        require(directory.isDirectory) { "require $directory to be directory" }
        runner = thread {
            run(directory)
        }
    }

    private fun run(directory: File) {
        FileSystems.getDefault().newWatchService().use { service ->
            try {
                directory.toPath().register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                while (!Thread.interrupted()) {
                    val key = service.take()
                    key.pollEvents().forEach(this::submit)
                    key.reset()
                }
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun submit(event: WatchEvent<*>) {
        val file = directory.resolve((event.context() as Path).toFile())
        buffer.put(file)
    }

    fun take(): List<File> {
        val events = mutableListOf<File>()
        while (buffer.isNotEmpty()) {
            val take = buffer.take()
            if (take.notExists() || take.isFile) events += take
        }
        return events
    }

    override fun close() {
        runner.interrupt()
        runner.join()
    }

}