package top.anagke.kwormhole.model.local

import net.contentobjects.jnotify.JNotify
import net.contentobjects.jnotify.JNotifyListener
import top.anagke.kio.file.notExists
import java.io.Closeable
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class FileAltMonitor(
    private val root: File,
) : Closeable {

    private val alternations: BlockingQueue<File> = LinkedBlockingQueue()
    private val watchID: Int


    init {
        require(root.isDirectory) { "require $root to be directory" }
        watchID = JNotify.addWatch(root.canonicalPath, JNotify.FILE_ANY, true, Listener())
    }

    private inner class Listener : JNotifyListener {

        override fun fileCreated(wd: Int, rootPath: String, name: String) {
            submit(File(rootPath, name))
        }

        override fun fileDeleted(wd: Int, rootPath: String, name: String) {
            submit(File(rootPath, name))
        }

        override fun fileModified(wd: Int, rootPath: String, name: String) {
            submit(File(rootPath, name))
        }

        override fun fileRenamed(wd: Int, rootPath: String, oldName: String, newName: String) {
            submit(File(rootPath, newName))
        }

    }


    private fun submit(file: File) {
        alternations.put(file)
    }

    fun poll(): List<File> {
        val events = mutableListOf<File>()
        while (alternations.isNotEmpty()) {
            val take = alternations.take()
            if (take.notExists() || take.isFile) events += take
        }
        return events
    }

    fun take(): List<File> {
        val events = mutableListOf<File>()
        do {
            val take = alternations.take()
            if (take.notExists() || take.isFile) events += take
        } while (alternations.isNotEmpty() || events.isEmpty())
        return events
    }

    override fun close() {
        val succeed = JNotify.removeWatch(watchID)
        if (!succeed) {
            throw IllegalStateException("invalid watch id: $watchID")
        }
    }

}