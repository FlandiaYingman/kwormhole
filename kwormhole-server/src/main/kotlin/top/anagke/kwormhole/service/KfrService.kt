package top.anagke.kwormhole.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.dao.KfrEntity
import top.anagke.kwormhole.dao.KfrRepository
import java.io.File
import java.nio.file.Path
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

@Service
class KfrService(
    private val kfrRepo: KfrRepository,
    @Value("\${kwormhole.root}")
    private val root: String,
) {

    //TODO: Transaction!
    private val lock: ReadWriteLock = ReentrantReadWriteLock(true)

    fun all(): List<Kfr> {
        lock.readLock().withLock {
            return kfrRepo.findAll().map { it.asKfr() }
        }
    }

    fun head(path: String): Kfr? {
        lock.readLock().withLock {
            return kfrRepo.findById(path).orElseGet { null }?.asKfr()
        }
    }


    fun get(path: String): FatKfr? {
        lock.readLock().withLock {
            val kfr = kfrRepo.findById(path).orElse(null)?.asKfr() ?: return null
            val fat = FatKfr(kfr, kfr.file)
            return fat
        }
    }

    fun put(fatKfr: FatKfr) {
        val kfr = fatKfr.kfr
        val path = fatKfr.kfr.path
        lock.writeLock().withLock {
            if (kfr.canReplace(this.head(path))) {
                kfrRepo.save(KfrEntity(kfr))
                fatKfr.actualize(kfr.file)
            }
        }
    }

    private val Kfr.file: Path get() = toFile(File(root)).toPath()


    operator fun contains(path: String): Boolean {
        val recordExistence = kfrRepo.existsById(path)
        return recordExistence
    }

}