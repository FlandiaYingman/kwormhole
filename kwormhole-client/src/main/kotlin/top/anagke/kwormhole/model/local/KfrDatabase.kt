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
import top.anagke.kwormhole.Kfr
import java.io.Closeable
import java.io.File

class KfrDatabase(
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
            SchemaUtils.create(KfrTable)
        }
    }

    fun all(): List<Kfr> {
        return transaction(database) {
            KfrEntity.all()
                .map { it.toKfr() }
        }
    }

    fun get(path: String): Kfr? {
        return transaction(database) {
            KfrEntity.findById(path)
                ?.toKfr()
        }
    }

    fun put(records: Collection<Kfr>) {
        transaction(database) {
            for (record in records) {
                val entity = KfrEntity.findById(record.path)
                if (entity != null) {
                    entity.byKfr(record)
                } else {
                    KfrEntity.new(record.path) { byKfr(record) }
                }
            }
        }
    }

    override fun close() {
        dbPool.close()
    }

}

object KfrTable : IdTable<String>("kfr") {

    override val id: Column<EntityID<String>> = varchar("path", 1024).uniqueIndex().entityId()

    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }

    val size = long("size")

    val time = long("time")

    val hash = long("hash")

}

class KfrEntity(id: EntityID<String>) : Entity<String>(id) {

    companion object : EntityClass<String, KfrEntity>(KfrTable)

    val path = this.id.value

    var size by KfrTable.size

    var time by KfrTable.time

    var hash by KfrTable.hash

    fun byKfr(record: Kfr) {
        require(this.path == record.path) { "The path ${this.path} and ${record.path} are required to be same." }
        size = record.size
        time = record.time
        hash = record.hash
    }

    fun toKfr(): Kfr {
        return Kfr(path, time, size, hash)
    }

}
