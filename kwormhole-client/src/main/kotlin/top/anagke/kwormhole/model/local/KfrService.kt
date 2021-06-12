package top.anagke.kwormhole.model.local

import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.FatKfr
import java.io.Closeable
import java.io.File
import java.nio.file.Path
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
        val pathsDistinct = paths.distinct()
        val validKfrs = lock.readLock().withLock {
            val oldKfrsTask = ForkJoinTask.adapt(Callable { database.get(pathsDistinct) }).fork()
            val newKfrTasks = pathsDistinct.map { path ->
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

    fun get(path: String): FatKfr? {
        lock.readLock().withLock {
            val kfr = database.get(path) ?: return null
            val kfrContent = FatKfr(kfr, kfr.file)
            return kfrContent
        }
    }

    fun getKfr(path: String): Kfr? {
        lock.readLock().withLock {
            return database.get(path)
        }
    }

    fun put(fatKfr: FatKfr) {
        var alt = false
        val kfr = fatKfr.kfr
        val path = fatKfr.kfr.path
        lock.writeLock().withLock {
            if (kfr.canReplace(this.getKfr(path))) {
                database.put(listOf(kfr))
                fatKfr.actualize(kfr.file)
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


    private val Kfr.file: Path
        get() = toFile(root).toPath()


    override fun close() {
        database.close()
    }

}