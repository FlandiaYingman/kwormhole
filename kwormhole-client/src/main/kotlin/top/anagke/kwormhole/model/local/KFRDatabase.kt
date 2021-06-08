package top.anagke.kwormhole.model.local

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.KFR
import top.anagke.kwormhole.model.RecordEntity
import top.anagke.kwormhole.model.RecordEntity.Companion.fromObj
import top.anagke.kwormhole.model.RecordEntity.Companion.toObj
import top.anagke.kwormhole.model.RecordTable
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

    private val database = Database.connect(dbPool)

    init {
        transaction(database) {
            SchemaUtils.create(RecordTable)
        }
    }

    fun all(): List<KFR> {
        return transaction(database) {
            RecordEntity.all().map { it.toObj() }.toList()
        }
    }

    fun put(records: Collection<KFR>) {
        transaction(database) {
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
        return transaction(database) {
            RecordEntity.findById(path)?.toObj()
        }
    }

    override fun close() {
        dbPool.close()
    }

}