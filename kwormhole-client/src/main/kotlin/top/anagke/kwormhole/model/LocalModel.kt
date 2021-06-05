package top.anagke.kwormhole.model

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.FileContent
import top.anagke.kwormhole.FileRecord
import top.anagke.kwormhole.model.RecordEntity.Companion.fromObj
import top.anagke.kwormhole.model.RecordEntity.Companion.toObj
import top.anagke.kwormhole.shouldReplace
import top.anagke.kwormhole.toDiskPath
import top.anagke.kwormhole.writeToFile
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService


class LocalModel(
    private val localDir: File,
    private val database: RecordDatabase? = null,
) : AbstractModel() {

    private val monitor = FileSystemMonitor(localDir)


    override fun init(): List<Pair<FileRecord, FileContent>> {
        val databaseRecords = database?.all()
            ?.map { fromDatabase(it) }
            .orEmpty()
            .toList()
        val diskRecords = localDir.walk()
            .filter { it != localDir }
            .filter { it.isFile }
            .map { fromDisk(it) }
            .toList()
        return databaseRecords + diskRecords
    }

    override fun poll(): List<Pair<FileRecord, FileContent>> {
        val changes = monitor.take()
            .filterNot { it.file.isDirectory }
            .map { fromDisk(it.file) }
            .toList()
        return changes
    }

    private fun fromDatabase(record: FileRecord): Pair<FileRecord, FileContent> {
        val path = record.path
        val diskPath = path.toDiskPath(localDir)
        val newRecord = FileRecord.record(localDir, diskPath)
        val content = FileContent.content(diskPath)
        return if (newRecord.shouldReplace(record)) {
            (newRecord to content)
        } else {
            (record to content)
        }
    }

    private fun fromDisk(file: File): Pair<FileRecord, FileContent> {
        val record = FileRecord.record(localDir, file)
        val content = FileContent.content(file)
        return record to content
    }


    override fun onPut(record: FileRecord, content: FileContent) {
        val diskPath = record.path.toDiskPath(localDir)
        if (record.isNone()) {
            diskPath.delete()
            //TODO: Delete 'diskPath's parent
        } else {
            if (diskPath.parentFile.exists().not()) {
                diskPath.parentFile.mkdirs()
            }
            content.writeToFile(diskPath)
        }
        database?.put(listOf(record))
    }


    override fun close() {
        super.close()
        monitor.close()
        database?.close()
    }

    override fun toString(): String {
        return "LocalModel(localDir=$localDir)"
    }

}


private class FileSystemMonitor(
    directory: File,
) : Closeable {

    private val watchService: WatchService

    private val watchDirectory: Path = directory.toPath()

    init {
        require(Files.isDirectory(watchDirectory)) { "The directory $watchDirectory is required to be a directory." }


        watchService = FileSystems.getDefault().newWatchService()
        watchDirectory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )
    }

    fun take(): List<FileChangeEvent> {
        val watchKey = watchService.take()
        val events = watchKey.pollEvents()
        watchKey.reset()

        return events.map { FileChangeEvent.from(watchDirectory.toFile(), it) }
    }

    override fun close() {
        watchService.close()
    }

}

private data class FileChangeEvent(
    val file: File,
    val change: Type,
) {

    enum class Type {
        MODIFIED,
        CREATED,
        DELETED,
    }

    companion object {

        fun from(root: File, event: WatchEvent<*>): FileChangeEvent {
            val file = root.resolve((event.context() as Path).toFile())
            val type = when (event.kind()) {
                StandardWatchEventKinds.ENTRY_CREATE -> Type.CREATED
                StandardWatchEventKinds.ENTRY_DELETE -> Type.DELETED
                StandardWatchEventKinds.ENTRY_MODIFY -> Type.MODIFIED
                else -> error("Unexpected kind of watch event $event.")
            }
            return FileChangeEvent(file, type)
        }

    }

}


class RecordDatabase(
    private val dbFile: File,
) : Closeable {

    private val dbPool = run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${dbFile.canonicalPath}"
        config.driverClassName = "org.sqlite.JDBC"
        config.maximumPoolSize = 1
        HikariDataSource(config)
    }

    private fun openDb() = Database.connect(dbPool)

    init {
        transaction(openDb()) {
            SchemaUtils.create(RecordTable)
        }
    }

    fun all(): List<FileRecord> {
        return transaction(openDb()) {
            RecordEntity.all().map { it.toObj() }.toList()
        }
    }

    fun put(records: Collection<FileRecord>) {
        transaction(openDb()) {
            for (record in records) {
                val recordEntity = RecordEntity.findById(record.path)
                if (recordEntity == null) {
                    RecordEntity.new(record.path) { this.fromObj(record) }
                } else {
                    recordEntity.fromObj(record)
                }
            }
        }
    }

    override fun close() {
        dbPool.close()
    }

}

object RecordTable : IdTable<String>("file_record") {
    override val id: Column<EntityID<String>> = varchar("path", 1024).uniqueIndex().entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
    val size = long("size")
    val time = long("time")
    val hash = long("hash")
}

class RecordEntity(id: EntityID<String>) : Entity<String>(id) {

    companion object : EntityClass<String, RecordEntity>(RecordTable) {
        fun RecordEntity.fromObj(record: FileRecord) {
            require(this.path == record.path) { "The path ${this.path} and ${record.path} are required to be same." }
            size = record.size
            time = record.time
            hash = record.hash
        }

        fun RecordEntity.toObj(): FileRecord {
            return FileRecord(path, size, time, hash)
        }

    }

    val path = this.id.value
    var size by RecordTable.size
    var time by RecordTable.time
    var hash by RecordTable.hash

}