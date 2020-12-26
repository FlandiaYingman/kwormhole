package top.anagke.kwormhole.store

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.store.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.store.KwormFileTable.hash
import top.anagke.kwormhole.store.KwormFileTable.path
import top.anagke.kwormhole.store.KwormFileTable.updateTime
import top.anagke.kwormhole.util.hash
import top.anagke.kwormhole.util.requireHashEquals
import top.anagke.kwormhole.util.use
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max
import kotlin.sequences.Sequence

class FileStore(val storePath: File) {

    private val databasePath = storePath.resolve(".kwormhole.db")

    private val contentPath = storePath.resolve(".")


    private val database = Database.connect("jdbc:sqlite:${databasePath}")

    init {
        storePath.mkdirs()
        transaction(database) {
            SchemaUtils.create(KwormFileTable)
        }
    }


    fun find(kwormPath: String): KwormFile? {
        return transaction(database) {
            val single = KwormFileTable.select { path eq kwormPath }.singleOrNull() ?: return@transaction null
            KwormFile(single[path], FileMetadata(single[hash], single[updateTime]))
        }
    }

    fun findAll(kwormPaths: Sequence<String>): Sequence<KwormFile?> {
        return transaction(database) {
            kwormPaths.toList().map {
                KwormFileTable.select { path eq it }.singleOrNull()
            }
        }.asSequence().map {
            val single = it ?: return@map null
            KwormFile(single[path], FileMetadata(single[hash], single[updateTime]))
        }
    }


    fun store(bytes: ByteArray, kwormFile: KwormFile) {
        resolveTemp(kwormFile.path).use { temp ->
            val actualFile = resolve(kwormFile.path)
            temp.parentFile.mkdirs()
            temp.writeBytes(bytes)
            temp.requireHashEquals(kwormFile.hash)
            temp.renameTo(actualFile)
            transaction(database) {
                KwormFileTable.insert {
                    it[path] = kwormFile.path
                    it[hash] = kwormFile.hash
                    it[updateTime] = kwormFile.updateTime
                }
            }
        }
    }

    fun storePart(bytes: ByteArray, range: LongRange, kwormPath: String) {
        val temp = resolveTemp(kwormPath)
        temp.parentFile.mkdirs()
        temp.createNewFile()
        RandomAccessFile(temp, "rw").use {
            it.setLength(max(it.length(), range.last))
            it.seek(range.first)
            it.write(bytes)
        }
    }

    fun storePart(kwormFile: KwormFile) {
        resolveTemp(kwormFile.path).use { temp ->
            val actualPath = resolve(kwormFile.path)
            temp.requireHashEquals(kwormFile.hash)
            temp.renameTo(actualPath)
            transaction(database) {
                KwormFileTable.insert {
                    it[path] = kwormFile.path
                    it[hash] = kwormFile.hash
                    it[updateTime] = kwormFile.updateTime
                }
            }
        }
    }

    fun storeExisting(kwormPath: String) {
        val actualPath = resolve(kwormPath)
        val actualHash = actualPath.hash()
        val actualUpdateTime = utcTimeMillis
        transaction(database) {
            KwormFileTable.insert {
                it[path] = kwormPath
                it[hash] = actualHash
                it[updateTime] = actualUpdateTime
            }
        }
    }

    fun storeAllExisting(kwormPaths: Sequence<String>) {
        transaction(database) {
            for (kwormPath in kwormPaths) {
                val actualPath = resolve(kwormPath)
                val actualHash = actualPath.hash()
                val actualUpdateTime = utcTimeMillis
                KwormFileTable.insert {
                    it[path] = kwormPath
                    it[hash] = actualHash
                    it[updateTime] = actualUpdateTime
                }
            }
        }
    }


    fun update(kwormPath: KwormFile) {
        val actualHash = resolve(kwormPath.path).hash()
        if (kwormPath.hash != actualHash) {
            transaction(database) {
                KwormFileTable.update({ path eq kwormPath.path }) {
                    it[hash] = actualHash
                    it[updateTime] = utcTimeMillis
                }
            }
        }
    }

    fun updateAll(kwormPaths: Sequence<KwormFile>) {
        transaction(database) {
            kwormPaths.forEach { kwormFile ->
                val actualHash = resolve(kwormFile.path).hash()
                if (kwormFile.hash != actualHash) {
                    KwormFileTable.update({ path eq kwormFile.path }) {
                        it[hash] = actualHash
                        it[updateTime] = utcTimeMillis
                    }
                }
            }
        }
    }


    fun resolve(kwormPath: String): File {
        return contentPath.resolve(kwormPath.trimStart('/'))
    }

    private fun resolveTemp(kwormPath: String): File {
        return contentPath.resolve(kwormPath.trimStart('/').trimEnd('/') + ".temp")
    }

}