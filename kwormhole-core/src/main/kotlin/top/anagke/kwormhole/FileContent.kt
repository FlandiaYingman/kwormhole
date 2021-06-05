package top.anagke.kwormhole

import top.anagke.kwormhole.util.Hasher
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

interface FileContent {

    companion object {

        fun content(file: File): FileContent {
            return if (file.exists()) {
                DiskFileContent(file)
            } else {
                NoneFileContent
            }
        }

        fun content(bytes: ByteArray): FileContent {
            return MemoryFileContent(bytes)
        }

    }

    fun hash(): Long

    fun length(): Long

    fun openStream(): InputStream

    fun isNone() = length() < 0

}

object NoneFileContent : FileContent {

    override fun hash(): Long = 0

    override fun length(): Long = -1

    override fun openStream() = ByteArrayInputStream(byteArrayOf())

}

class DiskFileContent(val file: File) : FileContent {

    init {
        //require(file.exists()) { "The file $file is required to exist." }
        //require(file.isFile) { "The file $file is required to be file." }
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

fun FileContent.writeToFile(dest: File) {
    if (this is DiskFileContent) {
        this.file.copyTo(dest, overwrite = true)
    } else {
        this.openStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

fun FileContent.asBytes(): ByteArray {
    this.openStream().use {
        return it.readBytes()
    }
}