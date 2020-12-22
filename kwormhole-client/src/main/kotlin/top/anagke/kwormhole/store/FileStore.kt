package top.anagke.kwormhole.store

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.store.KwormFileTable.hash
import top.anagke.kwormhole.store.KwormFileTable.path
import top.anagke.kwormhole.store.KwormFileTable.updateTime
import top.anagke.kwormhole.util.hash
import top.anagke.kwormhole.util.requireHashEquals
import top.anagke.kwormhole.util.use
import java.io.File
import java.io.RandomAccessFile
import java.time.Instant
import kotlin.math.max

class FileStore(storePath: File) {

    private val databasePath = storePath.resolve(".kwormhole.db")

    private val contentPath = storePath.resolve(".")


    private val database = Database.connect("jdbc:sqlite:${databasePath}")

    init {
        storePath.mkdirs()
        transaction(database) {
            SchemaUtils.create(KwormFileTable)
        }
    }


    fun exists(findPath: String): Boolean {
        return transaction(database) {
            KwormFileTable.select { path eq findPath }.any()
        }
    }

    fun find(findPath: String): KwormFile {
        return transaction(database) {
            val single = KwormFileTable.select { path eq findPath }.single()
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

    fun storePart(bytes: ByteArray, range: LongRange, path: String) {
        val temp = resolveTemp(path)
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

    fun updateExisting(filePath: String) {
        val actualPath = resolve(filePath)
        val kwormFile = KwormFile(filePath, FileMetadata(actualPath.hash(), Instant.now().toEpochMilli()))
        transaction(database) {
            KwormFileTable.replace {
                it[path] = kwormFile.path
                it[hash] = kwormFile.hash
                it[updateTime] = kwormFile.updateTime
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