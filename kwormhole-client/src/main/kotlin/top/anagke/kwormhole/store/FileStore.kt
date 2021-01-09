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
import java.io.Closeable
import java.io.File

class FileStore(private val storePath: File) : Closeable {

    private val databaseFile: File = storePath.resolve(".kwormhole.db")

    private val databaseCP = run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${databaseFile}"
        config.driverClassName = "org.sqlite.JDBC"
        HikariDataSource(config)
    }

    private val database get() = Database.connect(databaseCP)


    init {
        transaction(database) {
            SchemaUtils.create(KwormFileTable)
        }
    }


    fun list(): List<KwormFile> {
        return transaction(database) {
            KwormFileTable.selectAll()
                .map { KwormFile(it[path], it[hash], it[time]) }
                .toList()
        }
    }

    fun find(kwormPath: String): KwormFile? {
        return transaction(database) {
            val single = KwormFileTable.select { path eq kwormPath }.singleOrNull()
            if (single != null) KwormFile(single[path], single[hash], single[time]) else null
        }
    }

    fun store(kwormFile: KwormFile) {
        transaction(database) {
            KwormFileTable.insert {
                it[path] = kwormFile.path
                it[hash] = kwormFile.hash
                it[time] = kwormFile.time
            }
        }
    }

    fun storeNew(kwormPath: String) {
        store(KwormFile(kwormPath, hash(kwormPath)))
    }

    fun storeBytes(bytes: ByteArray, kwormFile: KwormFile) {
        val file = resolve(kwormFile.path)
        file.parentFile.mkdirs()
        file.writeBytes(bytes)
        file.requireHashEquals(kwormFile.hash)
        store(kwormFile)
    }

    fun update(kwormPath: KwormFile) {
        val actualHash = hash(kwormPath.path)
        if (kwormPath.hash != actualHash) {
            transaction(database) {
                KwormFileTable.update({ path eq kwormPath.path }) {
                    it[hash] = actualHash
                    it[time] = utcTimeMillis
                }
            }
        }
    }

    fun scan() {
        val (presentFiles, absentFiles) = storePath.walk()
            .filter(File::isFile)
            .filterNot { it == databaseFile }
            .map { relative(it) }
            .map { it to find(it) }
            .partition { it.second != null }
        presentFiles.forEach { (_, kwormFile) -> update(kwormFile!!) }
        absentFiles.forEach { (kwormPath, _) -> storeNew(kwormPath) }
    }


    fun resolve(kwormPath: String): File {
        return storePath.resolve(kwormPath.trimStart('/'))
    }

    fun relative(file: File): String {
        return "/${file.toRelativeString(storePath).replace('\\', '/')}"
    }

    fun hash(kwormPath: String): Long {
        return resolve(kwormPath).hash()
    }


    override fun close() {
        databaseCP.close()
    }

}