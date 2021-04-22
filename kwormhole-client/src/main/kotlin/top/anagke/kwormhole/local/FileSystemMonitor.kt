package top.anagke.kwormhole.local

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.nio.file.WatchService

class FileSystemMonitor(
    directories: List<File>,
) {

    private val watchService: WatchService

    private val watchDirectories: List<Path> = directories.map(File::toPath)

    init {
        watchDirectories.forEach {
            require(Files.isDirectory(it)) { "The directory $it is required to be a directory." }
        }

        watchService = FileSystems.getDefault().newWatchService()
        watchDirectories.forEach {
            it.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        }
    }

    fun take(): List<WatchEvent<*>> {
        val watchKey = watchService.take()
        val events = watchKey.pollEvents()
        watchKey.reset()

        return events
    }

}