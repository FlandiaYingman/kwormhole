package top.anagke.kwormhole.model.local

import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import top.anagke.kio.file.notExists
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
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
        //TODO: this implementation supports only windows, support others in future
        root.toPath().register(service, arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), FILE_TREE)
        while (!Thread.interrupted()) {
            val key = service.take()
            key.pollEvents().forEach { event ->
                val parent = (key.watchable() as Path).toFile()
                val file = parent.resolve((event.context() as Path).toFile())
                submit(file)
            }
            key.reset()
        }
    }

    private fun submit(file: File) {
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