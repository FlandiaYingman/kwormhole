package top.anagke.kwormhole.model.remote

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import top.anagke.kwormhole.Kfr
import top.anagke.kwormhole.FatKfr
import top.anagke.kwormhole.model.AbstractModel
import top.anagke.kwormhole.util.TempFiles
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList

class RemoteModel(
    private val host: String,
    private val port: Int
) : AbstractModel() {

    companion object {

        val KFR_TEMP = Path.of("KFR_TEMP")

        init {
            TempFiles.register(KFR_TEMP)
        }

    }


    private val kwormClient: KfrClient = KfrClient(host, port)

    private var _kwormConnection: KfrConnection? = null
    private val kwormConnection: KfrConnection
        get() {
            var kwormConn = _kwormConnection
            if (kwormConn == null || !kwormConn.open) {
                kwormConn?.close()
                kwormConn = kwormClient.openConnection(after = lastPollTime)
            }
            _kwormConnection = kwormConn
            return kwormConn
        }

    private val putKfrList = CopyOnWriteArrayList<Kfr>()


    override fun init() {
    }


    private var lastPollTime: Long = Long.MIN_VALUE

    override fun poll() {
        try {
            val poll = kwormConnection.take()
            poll.forEach {
                submit(it)
            }

            lastPollTime = poll.maxOf { it.time }
        } catch (e: Exception) {
            //TODO: Log warning message
        }
    }

    @Synchronized
    private fun submit(it: Kfr) {
        if (it in putKfrList) {
            putKfrList -= it
        } else {
            changes.put(it)
        }
    }

    @Synchronized
    override fun put(fatKfr: FatKfr) {
        putKfrList += fatKfr.kfr

        TempFiles.useTempFile(KFR_TEMP) { temp ->
            fatKfr.actualize(temp)
            kwormClient.upload(fatKfr.kfr, temp.toFile())
        }
    }

    @Synchronized
    override fun getRecord(path: String): Kfr? {
        return kwormClient.downloadRecord(path)
    }

    @Synchronized
    override fun getContent(path: String): FatKfr? {
        val tempFile = TempFiles.allocTempFile(KFR_TEMP)
        val kfr = kwormClient.downloadContent(path, tempFile.toFile())
        if (kfr == null) {
            TempFiles.freeTempFile(tempFile)
            return null
        }
        val kfrContent = FatKfr(kfr, tempFile, cleanup = { TempFiles.freeTempFile(tempFile) })
        return kfrContent
    }


    override fun close() {
        kwormConnection.close()
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
