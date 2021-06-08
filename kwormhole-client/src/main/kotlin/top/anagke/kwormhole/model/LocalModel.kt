package top.anagke.kwormhole.model

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kio.deleteFile
import top.anagke.kwormhole.KFR
import top.anagke.kwormhole.KFR.Companion.recordAsKFR
import top.anagke.kwormhole.model.RecordEntity.Companion.fromObj
import top.anagke.kwormhole.model.RecordEntity.Companion.toObj
import top.anagke.kwormhole.toDiskPath
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


class LocalModel(
    private val root: File,
    private val database: RecordDatabase,
) : AbstractModel() {

    private val logger = KotlinLogging.logger {}

    private val monitor = FileSystemMonitor(root)


    override fun init() {
        val databasePathList = database.all()
            .map { it.path.toDiskPath(root) }
            .map { it.canonicalFile }
        val rootPathList = root.walk()
            .filter { it != root }
            .filter { it.isFile }
            .map { it.canonicalFile }
        val files = (databasePathList + rootPathList)
            .distinct()
            .toList()
        submitFile(files)
    }

    override fun poll() {
        val changes = monitor.take()
        submitFile(changes.map { it.file })
    }

    @Synchronized
    private fun submitFile(files: List<File>) {
        val kfrs = files.map { it.recordAsKFR(root) }
        val validKfrs = kfrs.asSequence()
            .filter { it.isValid() }
            .toList()
        validKfrs.forEach { changes.put(it) }
        database.put(validKfrs)
    }


    @Synchronized
    override fun getRecord(path: String): KFR? {
        return database.get(path)
    }

    @Synchronized
    override fun getContent(path: String, file: File): KFR? {
        val kfr = getRecord(path) ?: return null
        if (kfr.representsExisting()) {
            val diskPath = kfr.path.toDiskPath(root)
            diskPath.copyTo(file, overwrite = true)
        } else {
            file.deleteFile()
        }
        return kfr
    }


    @Synchronized
    override fun where(path: String): File {
        val diskPath = path.toDiskPath(root)
        val diskTempPath = diskPath.resolveSibling("${diskPath.name}.__temp")
        return diskTempPath
    }

    @Synchronized
    override fun put(record: KFR, content: File?) {
        if (record.isValid()) {
            logger.info { "Putting KFR $record" }
            val diskPath = record.path.toDiskPath(root)
            if (record.representsExisting()) {
                content!!
                if (diskPath.parentFile.canonicalFile != content.parentFile.canonicalFile && content.name.endsWith(".__temp")) {
                    content.renameTo(diskPath)
                } else {
                    content.copyTo(diskPath, overwrite = true)
                }
            } else {
                diskPath.deleteFile()
            }
            changes.put(record)
            database.put(listOf(record))
        }
    }


    override fun close() {
        super.close()
        monitor.close()
        database.close()
    }

    override fun toString(): String {
        return "LocalModel(localDir=$root)"
    }

}


private class FileSystemMonitor(
    directory: File,
) : Closeable {

    private val runner: Thread

    private val buffer: BlockingQueue<FileChangeEvent> = LinkedBlockingQueue()

    init {
        require(directory.isDirectory) { "The directory $directory is required to be a directory." }
        runner = thread {
            run(directory)
        }
    }

    private fun run(directory: File) {
        FileSystems.getDefault().newWatchService().use { service ->
            try {
                directory.toPath().register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                while (!Thread.interrupted()) {
                    val key = service.take()
                    key.pollEvents()
                        .asSequence()
                        .map { FileChangeEvent.from(directory, it) }
                        .forEach { buffer.put(it) }
                    key.reset()
                }
            } catch (e: InterruptedException) {
            }
        }
    }

    fun take(): List<FileChangeEvent> {
        val events = mutableListOf<FileChangeEvent>()
        while (buffer.isNotEmpty()) {
            val take = buffer.take()
            if (!take.file.isDirectory) events += take
        }
        return events
    }

    override fun close() {
        runner.interrupt()
        runner.join()
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
                ENTRY_CREATE -> Type.CREATED
                ENTRY_DELETE -> Type.DELETED
                ENTRY_MODIFY -> Type.MODIFIED
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

    fun all(): List<KFR> {
        return transaction(openDb()) {
            RecordEntity.all().map { it.toObj() }.toList()
        }
    }

    fun put(records: Collection<KFR>) {
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

    fun get(path: String): KFR? {
        return transaction(openDb()) {
            RecordEntity.findById(path)?.toObj()
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
        fun RecordEntity.fromObj(record: KFR) {
            require(this.path == record.path) { "The path ${this.path} and ${record.path} are required to be same." }
            size = record.size
            time = record.time
            hash = record.hash
        }

        fun RecordEntity.toObj(): KFR {
            return KFR(path, time, size, hash)
        }

    }

    val path = this.id.value
    var size by RecordTable.size
    var time by RecordTable.time
    var hash by RecordTable.hash

}