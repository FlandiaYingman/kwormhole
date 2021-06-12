package top.anagke.kwormhole.model.local

import top.anagke.kio.notExists
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class FileAltMonitor(
    private val root: File,
) : Closeable {

    private val runner: Thread
    private val service: WatchService = FileSystems.getDefault().newWatchService()
    private val alternations: BlockingQueue<File> = LinkedBlockingQueue()


    init {
        require(root.isDirectory) { "require $root to be directory" }
        runner = thread {
            try {
                run()
            } catch (e: InterruptedException) {
            }
        }
    }


    private fun run() {
        register(root)
        while (!Thread.interrupted()) {
            val key = service.take()
            key.pollEvents().forEach(this::submit)
            key.reset()
        }
    }

    private fun register(dir: File) {
        check(dir.isDirectory)
        dir.walk()
            .filter { it.isDirectory }
            .forEach { it.toPath().register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) }
    }

    private fun submit(event: WatchEvent<*>) {
        val file = root.resolve((event.context() as Path).toFile())
        if (file.isDirectory) register(file)
        alternations.put(file)
    }

    fun take(): List<File> {
        val events = mutableListOf<File>()
        while (alternations.isNotEmpty()) {
            val take = alternations.take()
            if (take.notExists() || take.isFile) events += take
        }
        return events
    }

    override fun close() {
        runner.interrupt()
        runner.join()
        service.close()
    }

}