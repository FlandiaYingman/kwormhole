package top.anagke.kwormhole.store

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import top.anagke.kwormhole.store.Metadata.Companion.utcEpochMillis
import top.anagke.kwormhole.store.MetadataEntity.Companion.fromObj
import top.anagke.kwormhole.store.MetadataEntity.Companion.toObj
import top.anagke.kwormhole.util.sameTo
import java.io.Closeable
import java.io.File

class LocalStore(private val storeRoot: File) : Store, Closeable {

    private val databaseFile: File = storeRoot.resolve(".store.db")

    private val databaseCP = run {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${databaseFile}"
        config.driverClassName = "org.sqlite.JDBC"
        HikariDataSource(config)
    }

    private val database get() = Database.connect(databaseCP)


    init {
        transaction(database) {
            SchemaUtils.create(MetadataTable)
        }
    }


    fun index() {
        val indexTime = utcEpochMillis
        val thisHash = storeRoot.walk()
            .filterNot { it sameTo databaseFile }
            .filter { it.isFile }
            .map { relative(it) to FileContent(it).hash() }
            .toMap()
        val prevHash = list().asSequence()
            .map { it.path to it.hash }
            .toMap()
        (thisHash.keys + prevHash.keys).asSequence()
            .filter { thisHash[it] != prevHash[it] }
            .forEach {
                transaction(database) {
                    val entity = MetadataEntity.findById(it) ?: MetadataEntity.new(it) {}
                    //However the file is changed, update time field.
                    entity.time = indexTime
                    if (it in thisHash) {
                        //It still exists, set its length.
                        entity.length = resolve(it).length()
                        entity.hash = thisHash[it]!!
                    } else {
                        //It doesn't exists anymore, set its length to -1.
                        entity.length = -1
                    }
                }
            }
    }


    override fun list(): List<Metadata> {
        return transaction(database) {
            MetadataEntity.all().map { it.toObj() }.toList()
        }
    }

    override fun getMetadata(path: String): Metadata {
        return transaction(database) {
            MetadataEntity[path].toObj()
        }
    }

    override fun getContent(path: String): Content {
        return FileContent(resolve(path))
    }

    override fun exists(path: String): Boolean {
        return transaction(database) {
            MetadataEntity.findById(path) != null
        }
    }

    override fun store(metadata: Metadata, content: Content) {
        val path = metadata.path
        transaction(database) {
            if (MetadataEntity.findById(path) == null) {
                MetadataEntity.new(path) { fromObj(metadata) }
            } else {
                MetadataEntity[path].fromObj(metadata)
            }
        }
        resolve(path).parentFile.mkdirs()
        resolve(path).writeBytes(content.bytes())
    }

    override fun delete(path: String) {
        transaction(database) {
            MetadataEntity[path].delete()
        }
        resolve(path).delete()
    }


    fun resolve(kwormPath: String): File {
        return storeRoot.resolve(kwormPath.trimStart('/'))
    }

    fun relative(file: File): String {
        return "/${file.toRelativeString(storeRoot).replace('\\', '/')}"
    }


    override fun close() {
        databaseCP.close()
    }

}