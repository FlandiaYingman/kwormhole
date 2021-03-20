package top.anagke.kwormhole

import top.anagke.kwormhole.util.Hasher
import java.io.ByteArrayInputStream
import java.io.InputStream

class MemoryFileContent(val bytes: ByteArray) : FileContent {

    override fun hash(): Long {
        return Hasher.hash(bytes)
    }

    override fun length(): Long {
        return bytes.size.toLong()
    }

    override fun openStream(): InputStream {
        return ByteArrayInputStream(bytes)
    }

}