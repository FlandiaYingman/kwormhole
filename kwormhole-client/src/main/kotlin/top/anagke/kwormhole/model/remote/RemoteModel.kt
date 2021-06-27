package top.anagke.kwormhole.model.remote

import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.model.AbstractModel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class RemoteModel(
    private val host: String,
    private val port: Int
) : AbstractModel() {

    private val kfrClient: KfrClient = KfrClient(host, port)

    private val uploadKfrs = CopyOnWriteArrayList<Kfr>()

    private val connection: AtomicReference<KfrConnection> = AtomicReference()

    private val lastConnectionTime: AtomicLong = AtomicLong(Long.MIN_VALUE)


    override fun init() {}

    override fun poll() {
        try {
            val conn = connection.updateAndGet {
                if (it == null || !it.open) {
                    kfrClient.openConnection(after = lastConnectionTime.get())
                } else {
                    it
                }
            }
            val poll = conn.take()
            poll.forEach {
                submit(it)
            }

            lastConnectionTime.set(poll.maxOf { it.time })
        } catch (e: Exception) {
            //TODO: Log warning message
        }
    }


    private fun submit(it: Kfr) {
        if (it in uploadKfrs) {
            uploadKfrs -= it
        } else {
            changes.put(it)
        }
    }

    override fun put(fatKfr: FatKfr) {
        uploadKfrs += Kfr(fatKfr)
        kfrClient.put(fatKfr)
    }

    override fun head(path: String): Kfr? {
        return kfrClient.head(path)
    }

    override fun get(path: String): FatKfr? {
        val fat = kfrClient.get(path)
        return fat
    }


    override fun close() {
        connection.get()?.close()
        super.close()
    }

    override fun toString(): String {
        return "RemoteModel(host=$host, port=$port)"
    }

}
