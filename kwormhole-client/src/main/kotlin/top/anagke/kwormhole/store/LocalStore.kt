package top.anagke.kwormhole.store

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.DiskFileContent
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.store.FileRecordEntity.Companion.fromObj
import top.anagke.kwormhole.store.FileRecordEntity.Companion.toObj
import top.anagke.kwormhole.util.matches
import java.io.Closeable
import java.io.File

class LocalStore(private val storeRoot: File) : Store, Closeable {

    private val databaseFile: File = storeRoot.resolve(".store.db")

    private val databaseConnectionPool = run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${databaseFile}"
        config.driverClassName = "org.sqlite.JDBC"
        HikariDataSource(config)
    }

    private val database get() = Database.connect(databaseConnectionPool)


    init {
        transaction(database) {
            SchemaUtils.create(FileRecordTable)
        }
    }


    @Synchronized
    fun index() {
        val newRecords = storeRoot.walk()
            .filter { it.isFile }
            .filterNot { it.matches(databaseFile) }
            .map { FileRecord.byFile(storeRoot, it) }
        newRecords.forEach { newRecord ->
            if (this.contains(newRecord.path)) {
                val oldRecord = this.getRecord(newRecord.path)
                if (newRecord.hash != oldRecord.hash) {
                    this.patch(newRecord)
                }
            } else {
                this.store(newRecord, DiskFileContent(storeRoot.resolve(newRecord.path)))
            }
        }
    }


    @Synchronized
    override fun listAll(): List<FileRecord> {
        return transaction(database) {
            FileRecordEntity.all().map { it.toObj() }.toList()
        }
    }

    @Synchronized
    override fun getRecord(path: String): FileRecord {
        return transaction(database) {
            FileRecordEntity[path].toObj()
        }
    }

    @Synchronized
    override fun getContent(path: String): FileContent {
        return DiskFileContent(resolve(path))
    }

    @Synchronized
    override fun contains(path: String): Boolean {
        return transaction(database) {
            FileRecordEntity.findById(path) != null
        }
    }

    @Synchronized
    override fun store(record: FileRecord, content: FileContent) {
        transaction(database) {
            if (FileRecordEntity.findById(record.path) == null) {
                FileRecordEntity.new(record.path) { fromObj(record) }
            } else {
                FileRecordEntity[record.path].fromObj(record)
            }
        }
        if (content is DiskFileContent && content.file == resolve(record.path)) {
            return
        }
        resolve(record.path).parentFile.mkdirs()
        content.openStream().use { input ->
            resolve(record.path).outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    @Synchronized
    fun patch(record: FileRecord) {
        transaction(database) {
            FileRecordEntity[record.path].fromObj(record)
        }
    }

    @Synchronized
    override fun delete(path: String) {
        transaction(database) {
            FileRecordEntity[path].delete()
        }
        resolve(path).delete()
    }


    private fun resolve(kwormPath: String): File {
        return storeRoot.resolve(kwormPath.trimStart('/'))
    }

    private fun relative(file: File): String {
        return "/${file.toRelativeString(storeRoot).replace('\\', '/')}"
    }


    @Synchronized
    override fun close() {
        databaseConnectionPool.close()
    }

}