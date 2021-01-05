package top.anagke.kwormhole.store

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import top.anagke.kwormhole.store.KwormFile.Companion.utcTimeMillis
import top.anagke.kwormhole.store.KwormFileTable.hash
import top.anagke.kwormhole.store.KwormFileTable.path
import top.anagke.kwormhole.store.KwormFileTable.time
import top.anagke.kwormhole.util.hash
import top.anagke.kwormhole.util.requireHashEquals
import top.anagke.kwormhole.util.use
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max

class FileStore(val storePath: File) : Closeable {

    private val databasePath = storePath.resolve(".kwormhole.db")

    private val contentPath = storePath.resolve(".")

    private val databaseCP = kotlin.run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${databasePath}"
        config.driverClassName = "org.sqlite.JDBC"
        HikariDataSource(config)
    }

    init {
        storePath.mkdirs()
        transaction(Database.connect(databaseCP)) {
            SchemaUtils.create(KwormFileTable)
        }
    }


    fun list(): Sequence<KwormFile> {
        return transaction(Database.connect(databaseCP)) {
            KwormFileTable.selectAll().map {
                KwormFile(it[path], it[hash], it[time])
            }.asSequence()
        }
    }


    fun find(kwormPath: String): KwormFile? {
        return transaction(Database.connect(databaseCP)) {
            val single = KwormFileTable.select { path eq kwormPath }.singleOrNull() ?: return@transaction null
            KwormFile(single[path], single[hash], single[time])
        }
    }

    fun findAll(kwormPaths: Sequence<String>): Sequence<KwormFile?> {
        return transaction(Database.connect(databaseCP)) {
            kwormPaths.toList().map {
                KwormFileTable.select { path eq it }.singleOrNull()
            }
        }.asSequence().map {
            val single = it ?: return@map null
            KwormFile(single[path], single[hash], single[time])
        }
    }


    fun store(bytes: ByteArray, kwormFile: KwormFile) {
        resolveTemp(kwormFile.path).use { temp ->
            val actualFile = resolve(kwormFile.path)
            temp.parentFile.mkdirs()
            temp.writeBytes(bytes)
            temp.requireHashEquals(kwormFile.hash)
            temp.renameTo(actualFile)
            transaction(Database.connect(databaseCP)) {
                KwormFileTable.insert {
                    it[path] = kwormFile.path
                    it[hash] = kwormFile.hash
                    it[time] = kwormFile.time
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
            transaction(Database.connect(databaseCP)) {
                KwormFileTable.insert {
                    it[path] = kwormFile.path
                    it[hash] = kwormFile.hash
                    it[time] = kwormFile.time
                }
            }
        }
    }

    fun storeExisting(kwormFile: KwormFile) {
        transaction(Database.connect(databaseCP)) {
            KwormFileTable.insert {
                it[path] = kwormFile.path
                it[hash] = kwormFile.hash
                it[time] = kwormFile.time
            }
        }
    }

    fun storeAllExisting(kwormFiles: Sequence<KwormFile>) {
        transaction(Database.connect(databaseCP)) {
            for (kwormFile in kwormFiles) {
                KwormFileTable.insert {
                    it[path] = kwormFile.path
                    it[hash] = kwormFile.hash
                    it[time] = kwormFile.time
                }
            }
        }
    }


    fun update(kwormPath: KwormFile) {
        val actualHash = resolve(kwormPath.path).hash()
        if (kwormPath.hash != actualHash) {
            transaction(Database.connect(databaseCP)) {
                KwormFileTable.update({ path eq kwormPath.path }) {
                    it[hash] = actualHash
                    it[time] = utcTimeMillis
                }
            }
        }
    }

    fun updateAll(kwormPaths: Sequence<KwormFile>) {
        transaction(Database.connect(databaseCP)) {
            kwormPaths.forEach { kwormFile ->
                val actualHash = resolve(kwormFile.path).hash()
                if (kwormFile.hash != actualHash) {
                    KwormFileTable.update({ path eq kwormFile.path }) {
                        it[hash] = actualHash
                        it[time] = utcTimeMillis
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


    override fun close() {
        databaseCP.close()
    }

}