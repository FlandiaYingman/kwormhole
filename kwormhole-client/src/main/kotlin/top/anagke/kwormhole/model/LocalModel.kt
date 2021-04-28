package top.anagke.kwormhole.model

import mu.KotlinLogging
import top.anagke.kwormhole.DiskFileContent
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.shouldReplace
import top.anagke.kwormhole.toRealPath
import top.anagke.kwormhole.writeToFile
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class LocalModel(
    private val localDir: File,
) : Model {

    private val logger = KotlinLogging.logger { }

    private val runner: Thread = thread(start = false) { run() }

    private val barrier = CyclicBarrier(2)

    init {
        runner.start()
        barrier.await()
    }


    private fun run() {
        val monitor = FileSystemMonitor(listOf(localDir))
        try {
            barrier.await()
            initChanges()
            monitorChanges(monitor)
        } catch (ignore: InterruptedException) {
        } finally {
            monitor.close()
        }
    }

    private fun initChanges() {
        val allFiles = localDir.walk().filter { it != localDir }.toList()
        allFiles.forEach(this::submitFileChange)
    }

    private fun monitorChanges(monitor: FileSystemMonitor) {
        while (!Thread.interrupted()) {
            monitor.take().forEach { watchEvent ->
                val event = FileChangeEvent.from(localDir, watchEvent)
                submitFileChange(event.file)
            }
        }
    }


    override val records: Map<String, FileRecord> by lazy { recordsMutable }
    private val recordsMutable: MutableMap<String, FileRecord> = ConcurrentHashMap()

    override val contents: Map<String, () -> FileContent> by lazy { contentsMutable }
    private val contentsMutable: MutableMap<String, FileContentProvider> = ConcurrentHashMap()

    override val changes: BlockingQueue<String> = LinkedBlockingQueue()

    private fun submitFileChange(file: File) {
        val record = FileRecord.record(localDir, file)
        val content = DiskFileContent(file)
        submitChange(record, content)
    }

    private fun submitChange(record: FileRecord, content: FileContent, manual: Boolean = false) {
        val existingRecord = records[record.path]
        if (record.shouldReplace(existingRecord)) {
            recordsMutable[record.path] = record
            contentsMutable[record.path] = { content }
            if (manual) {
                logger.info { "Submit manual change: $record" }
            } else {
                changes.offer(record.path)
                logger.info { "Submit change: $record" }
            }
        }
    }


    override fun put(record: FileRecord, content: FileContent) {
        val actualFile = toRealPath(localDir, record.path)
        actualFile.parentFile.mkdirs()
        content.writeToFile(actualFile)

        submitChange(record, content, manual = true)
    }


    override fun close() {
        runner.interrupt()
    }

    override fun toString(): String {
        return "LocalModel(localDir=$localDir)"
    }

}

class FileSystemMonitor(
    directories: List<File>,
) : Closeable {

    private val watchService: WatchService

    private val watchDirectories: List<Path> = directories.map(File::toPath)

    init {
        watchDirectories.forEach {
            require(Files.isDirectory(it)) { "The directory $it is required to be a directory." }
        }

        watchService = FileSystems.getDefault().newWatchService()
        watchDirectories.forEach {
            it.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY)
        }
    }

    fun take(): List<WatchEvent<*>> {
        val watchKey = watchService.take()
        val events = watchKey.pollEvents()
        watchKey.reset()

        return events
    }

    override fun close() {
        watchService.close()
    }

}

data class FileChangeEvent(
    val file: File,
    val change: Type,
) {

    enum class Type {
        MODIFIED,
        CREATED,
        DELETED,
    }

    companion object {

        fun from(root: File, event: WatchEvent<*>): FileChangeEvent {
            val file = root.resolve((event.context() as Path).toFile())
            val type = when (event.kind()) {
                StandardWatchEventKinds.ENTRY_CREATE -> Type.CREATED
                StandardWatchEventKinds.ENTRY_DELETE -> Type.DELETED
                StandardWatchEventKinds.ENTRY_MODIFY -> Type.MODIFIED
                else -> error("Unexpected kind of watch event $event.")
            }
            return FileChangeEvent(file, type)
        }

    }

}