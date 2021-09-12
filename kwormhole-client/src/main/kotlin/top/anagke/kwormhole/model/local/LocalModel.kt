package top.anagke.kwormhole.model.local

import mu.KotlinLogging
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.model.AbstractModel
import java.io.File


class LocalModel(val kfrService: KfrService) : AbstractModel() {

    private val root = kfrService.root

    private val monitor = FileAltMonitor(root)


    private val log = KotlinLogging.logger { }


    override fun init() {
        kfrService.registerListener { changes.put(it) }
        sync()
    }

    override fun poll() {
        val poll = monitor.poll()
        sync(poll)
    }


    private fun sync(files: List<File>? = null) {
        do {
            val err = try {
                if (files != null) {
                    kfrService.sync(files)
                } else {
                    kfrService.sync()
                }
                null
            } catch (e: Exception) {
                log.warn(e) { "Error on local service syncing" }
                e
            }
        } while (err != null)
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
