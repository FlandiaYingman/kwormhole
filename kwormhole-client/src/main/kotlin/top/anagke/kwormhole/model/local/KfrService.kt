@file:Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")

package top.anagke.kwormhole.model.local

import mu.KotlinLogging
import top.anagke.kio.file.notExists
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.parseKfrPath
import top.anagke.kwormhole.toKfrPath
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

    private val logger = KotlinLogging.logger {}

    private val lock: ReadWriteLock = ReentrantReadWriteLock(true)


    /**
     * Synchronizes all files with file system and database.
     */
    fun sync() {
        val diskFiles = root
            .walk()
            .filter { it.isFile }
            .toList()
        val databaseFiles = database
            .all()
            .map { parseKfrPath(root, it.path) }
        sync(diskFiles + databaseFiles)
    }

    /**
     * Synchronizes given files with file system and database.
     */
    fun sync(files: List<File> = emptyList()) {
        val sequence = files
            .asSequence()
            .distinct()
        val fileList = sequence
            .onEach { check(it.isFile || it.notExists()) }
        val pathList = sequence
            .map { toKfrPath(root, it) }
        val result = lock.writeLock().withLock {
            val oldKfrTask = pathList.let { ForkJoinTask.adapt(Callable { database.get(it.toList()) }).fork() }
            val newKfrTasks = fileList.map { ForkJoinTask.adapt(Callable { Kfr(root, it) }).fork() }.toList()
            val result = oldKfrTask.join()
                .asSequence()
                .withIndex()
                .map { it.value to newKfrTasks[it.index].join() }
                .filter { (oldKfr, newKfr) -> newKfr.canReplace(oldKfr) }
                .map { (oldKfr, newKfr) -> newKfr }
                .toList()
            database.put(result)
            result
        }
        result.forEach { callListener(it) }
    }


    fun all(): List<Kfr> {
        lock.readLock().withLock {
            return database.all()
        }
    }

    fun head(path: String): Kfr? {
        lock.readLock().withLock {
            return database.get(path)
        }
    }

    fun get(path: String): FatKfr? {
        lock.readLock().withLock {
            val kfr = database.get(path) ?: return null
            val fat = FatKfr(kfr, kfr.file)
            return fat
        }
    }

    fun put(fatKfr: FatKfr) {
        var alt = false
        val kfr = fatKfr.kfr
        val path = fatKfr.kfr.path
        lock.writeLock().withLock {
            if (kfr.canReplace(this.head(path))) {
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

    private fun callListener(kfr: Kfr) {
        listeners.forEach { listener -> listener.invoke(kfr) }
    }

    init {
        registerListener { logger.info { "KfrService: $it " } }
    }


    private val Kfr.file: Path get() = toFile(root).toPath()

    override fun close() {
        database.close()
    }

}