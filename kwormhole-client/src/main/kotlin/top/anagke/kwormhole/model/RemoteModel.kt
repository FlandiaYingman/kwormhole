package top.anagke.kwormhole.model

import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import top.anagke.kwormhole.KFR
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

    private val putKfrList = CopyOnWriteArrayList<KFR>()


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
    private fun submit(it: KFR) {
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
    override fun put(record: KFR, content: File?) {
        putKfrList += record
        kwormClient.upload(record, content)
    }

    @Synchronized
    override fun getRecord(path: String): KFR? {
        return kwormClient.downloadRecord(path)
    }

    @Synchronized
    override fun getContent(path: String, file: File): KFR? {
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


fun toHttpHeaders(record: KFR): Headers {
    return mapOf(
        KFR.SIZE_HEADER_NAME to record.size.toString(),
        KFR.TIME_HEADER_NAME to record.time.toString(),
        KFR.HASH_HEADER_NAME to record.hash.toString()
    ).toHeaders()
}

fun fromHttpHeaders(path: String, headers: Headers): KFR {
    val size = headers[KFR.SIZE_HEADER_NAME]!!
    val time = headers[KFR.TIME_HEADER_NAME]!!
    val hash = headers[KFR.HASH_HEADER_NAME]!!
    return KFR(path, time.toLong(), size.toLong(), hash.toLong())
}
