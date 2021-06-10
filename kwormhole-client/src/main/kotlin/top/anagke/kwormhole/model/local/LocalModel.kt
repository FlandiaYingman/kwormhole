package top.anagke.kwormhole.model.local

import mu.KotlinLogging
import top.anagke.kio.notExists
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.model.AbstractModel
import top.anagke.kwormhole.toKfrPath


class LocalModel(val kfrService: KfrService) : AbstractModel() {

    private val logger = KotlinLogging.logger {}


    private val root = kfrService.root

    private val monitor = FileAltMonitor(root)


    override fun init() {
        kfrService.registerListener { changes.put(it) }

        val filePathSeq = root.walk()
            .filter { it.isFile || it.notExists() }
            .map { toKfrPath(root, it) }
        val databasePathSeq = kfrService.all()
            .map { it.path }
        kfrService.sync((filePathSeq + databasePathSeq).toList())
    }

    override fun poll() {
        val poll = monitor.take()
            .map { toKfrPath(root, it) }
        kfrService.sync(poll)
    }


    override fun getRecord(path: String): Kfr? {
        return kfrService.getKfr(path)
    }

    override fun getContent(path: String): FatKfr? {
        return kfrService.get(path)
    }


    override fun put(fatKfr: FatKfr) {
        kfrService.put(fatKfr)
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
