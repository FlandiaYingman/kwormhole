package top.anagke.kwormhole.model.local

import mu.KotlinLogging
import top.anagke.kio.notExists
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.model.AbstractModel
import top.anagke.kwormhole.toKfrPath
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING


class LocalModel(val kfrService: KfrService) : AbstractModel() {

    private val logger = KotlinLogging.logger {}


    private val root = kfrService.root

    private val monitor = FileAltMonitor(root)


    override fun init() {
        kfrService.registerListener { changes.put(it) }

        val filePathSeq = root.walk()
            .filter { it.isFile || it.notExists() }
            .map { it.toKfrPath(root) }
        val databasePathSeq = kfrService.all()
            .map { it.path }
        kfrService.sync((filePathSeq + databasePathSeq).toList())
    }

    override fun poll() {
        val poll = monitor.take()
            .map { it.toKfrPath(root) }
        kfrService.sync(poll)
    }


    override fun getRecord(path: String): Kfr? {
        return kfrService.get(path).first
    }

    override fun getContent(path: String, dst: File): Kfr? {
        val (kfr, src) = kfrService.get(path)
        if (kfr == null) return null
        if (kfr.representsExisting()) {
            Files.copy(src!!.toPath(), dst.toPath(), REPLACE_EXISTING)
        } else {
            Files.delete(dst.toPath())
        }
        return kfr
    }


    override fun put(record: Kfr, content: File?) {
        kfrService.put(record, content)
    }


    override fun close() {
        super.close()
        monitor.close()
        kfrService.close()
    }

    override fun toString(): String {
        return "LocalModel(localDir=$root)"
    }

}
