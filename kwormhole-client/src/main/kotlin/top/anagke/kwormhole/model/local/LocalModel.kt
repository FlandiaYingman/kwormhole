package top.anagke.kwormhole.model.local

import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.model.AbstractModel


class LocalModel(val kfrService: KfrService) : AbstractModel() {

    private val root = kfrService.root

    private val monitor = FileAltMonitor(root)


    override fun init() {
        kfrService.registerListener { changes.put(it) }
        kfrService.sync()
    }

    override fun poll() {
        val poll = monitor.take()
        kfrService.sync(poll)
    }


    override fun head(path: String): Kfr? {
        return kfrService.head(path)
    }

    override fun get(path: String): FatKfr? {
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
