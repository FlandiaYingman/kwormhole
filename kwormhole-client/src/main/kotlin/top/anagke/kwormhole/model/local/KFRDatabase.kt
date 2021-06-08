package top.anagke.kwormhole.model.local

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
import top.anagke.kwormhole.KFR
import java.io.Closeable
import java.io.File

class KFRDatabase(
    private val dbFile: File,
) : Closeable {

    private val dbPool = run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${dbFile.canonicalPath}"
        config.driverClassName = "org.sqlite.JDBC"
        config.maximumPoolSize = 1
        HikariDataSource(config)
    }

    private val database by lazy { Database.connect(dbPool) }

    init {
        transaction(database) {
            SchemaUtils.create(KFRTable)
        }
    }

    fun all(): List<KFR> {
        return transaction(database) {
            KFREntity.all()
                .map { it.toKfr() }
        }
    }

    fun get(path: String): KFR? {
        return transaction(database) {
            KFREntity.findById(path)
                ?.toKfr()
        }
    }

    fun put(records: Collection<KFR>) {
        transaction(database) {
            for (record in records) {
                val entity = KFREntity.findById(record.path)
                if (entity != null) {
                    entity.byKfr(record)
                } else {
                    KFREntity.new(record.path) { byKfr(record) }
                }
            }
        }
    }

    override fun close() {
        dbPool.close()
    }

}

object KFRTable : IdTable<String>("kfr") {

    override val id: Column<EntityID<String>> = varchar("path", 1024).uniqueIndex().entityId()

    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }

    val size = long("size")

    val time = long("time")

    val hash = long("hash")

}

class KFREntity(id: EntityID<String>) : Entity<String>(id) {

    companion object : EntityClass<String, KFREntity>(KFRTable)

    val path = this.id.value

    var size by KFRTable.size

    var time by KFRTable.time

    var hash by KFRTable.hash

    fun byKfr(record: KFR) {
        require(this.path == record.path) { "The path ${this.path} and ${record.path} are required to be same." }
        size = record.size
        time = record.time
        hash = record.hash
    }

    fun toKfr(): KFR {
        return KFR(path, time, size, hash)
    }

}
