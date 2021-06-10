package top.anagke.kwormhole.model.local

import top.anagke.kio.bytes
import top.anagke.kio.deleteFile
import top.anagke.kwormhole.Kfr
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class KfrService(
    val root: File,
    val database: KfrDatabase
) : Closeable {

    private val lock: ReadWriteLock = ReentrantReadWriteLock(true)


    /**
     * Synchronizes all of the specified paths with file system.
     */
    fun sync(paths: List<String>) {
        val validKfrs = lock.readLock().withLock {
            val oldKfrsTask = ForkJoinTask.adapt(Callable { database.get(paths) }).fork()
            val newKfrTasks = paths.map { path ->
                ForkJoinTask.adapt(Callable { Kfr(root, path) }).fork()
            }
            val oldKfrs = oldKfrsTask.join()
            oldKfrs.mapIndexedNotNull { i, oldKfr ->
                val newKfr = newKfrTasks[i].join()
                if (newKfr.canReplace(oldKfr)) newKfr else null
            }
        }
        lock.writeLock().withLock {
            database.put(validKfrs)
        }
        validKfrs.forEach { callListener(it) }
    }

    fun all(): List<Kfr> {
        lock.readLock().withLock {
            return database.all()
        }
    }

    fun get(path: String): Pair<Kfr?, File?> {
        lock.readLock().withLock {
            val kfr = database.get(path)
            return if (kfr?.exists() == true) {
                (kfr to kfr.file)
            } else {
                (kfr to null)
            }
        }
    }

    fun getKfr(path: String): Kfr? {
        lock.readLock().withLock {
            return database.get(path)
        }
    }

    fun put(kfr: Kfr, bytes: ByteArray?) {
        if (kfr.exists()) checkNotNull(bytes)
        var alt = false
        lock.writeLock().withLock {
            if (kfr.canReplace(this.getKfr(kfr.path))) {
                database.put(listOf(kfr))
                if (kfr.exists()) {
                    kfr.file.bytes = bytes!!
                } else {
                    kfr.file.deleteFile()
                }
                alt = true
            }
        }
        if (alt) callListener(kfr)
    }

    fun put(kfr: Kfr, file: File?) {
        if (kfr.exists()) checkNotNull(file)
        var alt = false
        lock.writeLock().withLock {
            if (kfr.canReplace(this.getKfr(kfr.path))) {
                database.put(listOf(kfr))
                if (kfr.exists()) {
                    Files.move(file!!.toPath(), kfr.file.toPath(), REPLACE_EXISTING)
                } else {
                    Files.delete(kfr.file.toPath())
                }
                alt = true
            }
        }
        if (alt) callListener(kfr)
    }


    private val listeners: MutableList<(Kfr) -> Unit> = CopyOnWriteArrayList()

    fun registerListener(listener: (Kfr) -> Unit) {
        listeners += listener
    }

    fun callListener(kfr: Kfr) {
        listeners.forEach { listener -> listener.invoke(kfr) }
    }


    private val Kfr.file: File
        get() = toFile(root)


    override fun close() {
        database.close()
    }

}