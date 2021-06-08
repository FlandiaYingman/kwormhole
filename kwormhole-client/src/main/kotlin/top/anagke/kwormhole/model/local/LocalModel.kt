package top.anagke.kwormhole.model.local

import mu.KotlinLogging
import top.anagke.kio.deleteFile
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.Kfr.Companion.asKfr
import top.anagke.kwormhole.model.AbstractModel
import top.anagke.kwormhole.resolveBy
import java.io.File


class LocalModel(
    private val root: File,
    private val database: KfrDatabase,
) : AbstractModel() {

    private val logger = KotlinLogging.logger {}

    private val monitor = FileAltMonitor(root)


    override fun init() {
        val databasePathList = database.all()
            .map { it.path.resolveBy(root) }
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
        submitFile(changes)
    }

    @Synchronized
    private fun submitFile(files: List<File>) {
        val kfrs = files.map { it.asKfr(root) }
        val validKfrs = kfrs.asSequence()
            .filter { it.isValid() }
            .toList()
        validKfrs.forEach { changes.put(it) }
        database.put(validKfrs)
    }


    @Synchronized
    override fun getRecord(path: String): Kfr? {
        return database.get(path)
    }

    @Synchronized
    override fun getContent(path: String, file: File): Kfr? {
        val kfr = getRecord(path) ?: return null
        if (kfr.representsExisting()) {
            val diskPath = kfr.path.resolveBy(root)
            diskPath.copyTo(file, overwrite = true)
        } else {
            file.deleteFile()
        }
        return kfr
    }


    @Synchronized
    override fun where(path: String): File {
        val diskPath = path.resolveBy(root)
        val diskTempPath = diskPath.resolveSibling("${diskPath.name}.__temp")
        return diskTempPath
    }

    @Synchronized
    override fun put(record: Kfr, content: File?) {
        if (record.isValid()) {
            logger.info { "Putting KFR $record" }
            val diskPath = record.path.resolveBy(root)
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
