package top.anagke.kwormhole.model

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import top.anagke.kwormhole.Kfr
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class RemoteModel(
    private val host: String,
    private val port: Int
) : AbstractModel() {

    private val kwormClient: KWormClient = KWormClient(host, port)

    private var _kwormConnection: KWormConnection? = null
    private val kwormConnection: KWormConnection
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

    override fun where(path: String): File? {
        return null
    }

    @Synchronized
    override fun put(record: Kfr, content: File?) {
        putKfrList += record
        kwormClient.upload(record, content)
    }

    @Synchronized
    override fun getRecord(path: String): Kfr? {
        return kwormClient.downloadRecord(path)
    }

    @Synchronized
    override fun getContent(path: String, file: File): Kfr? {
        return kwormClient.downloadContent(path, file)
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
