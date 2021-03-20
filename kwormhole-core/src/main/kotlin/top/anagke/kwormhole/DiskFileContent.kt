package top.anagke.kwormhole

import top.anagke.kwormhole.util.Hasher
import java.io.File
import java.io.InputStream

class DiskFileContent(val file: File) : FileContent {

    init {
        require(file.exists()) { "The file $file is required to exist." }
        require(file.isFile) { "The file $file is required to be file." }
    }

    override fun hash(): Long {
        return Hasher.hash(file)
    }

    override fun length(): Long {
        return file.length()
    }

    override fun openStream(): InputStream {
        return file.inputStream()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiskFileContent

        if (file != other.file) return false

        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

}