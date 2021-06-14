package top.anagke.kwormhole.model.remote

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.model.AbstractModel
import top.anagke.kwormhole.util.TempFiles
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class RemoteModel(
    private val host: String,
    private val port: Int
) : AbstractModel() {

    companion object {

        val KFR_TEMP: Path = Path.of("KFR_TEMP")

        init {
            TempFiles.register(KFR_TEMP)
        }

    }


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
        uploadKfrs += fatKfr.kfr
        TempFiles.useTempFile(KFR_TEMP) { temp ->
            fatKfr.actualize(temp)
            kfrClient.put(fatKfr.kfr, temp.toFile())
        }
    }

    override fun getRecord(path: String): Kfr? {
        return kfrClient.head(path)
    }

    override fun getContent(path: String): FatKfr? {
        val tempFile = TempFiles.allocTempFile(KFR_TEMP)
        val kfr = kfrClient.get(path, tempFile.toFile())
        if (kfr == null) {
            TempFiles.freeTempFile(tempFile)
            return null
        }
        val fat = FatKfr(kfr, tempFile, cleanup = { TempFiles.freeTempFile(tempFile) })
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


fun toHttpHeaders(record: Kfr): Headers {
    return mapOf(
        Kfr.SIZE_HEADER_NAME to record.size.toString(),
        Kfr.TIME_HEADER_NAME to record.time.toString(),
        Kfr.HASH_HEADER_NAME to record.hash.toString()
    ).toHeaders()
}

fun fromHttpHeaders(path: String, headers: Headers): Kfr {
    val size = headers[Kfr.SIZE_HEADER_NAME]!!
    val time = headers[Kfr.TIME_HEADER_NAME]!!
    val hash = headers[Kfr.HASH_HEADER_NAME]!!
    return Kfr(path, time.toLong(), size.toLong(), hash.toLong())
}
