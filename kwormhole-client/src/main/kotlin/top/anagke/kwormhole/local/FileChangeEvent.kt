package top.anagke.kwormhole.local

import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

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
