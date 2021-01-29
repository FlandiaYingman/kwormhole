package top.anagke.kwormhole.store

import java.io.File

class FileContent(val file: File) : Content {

    override fun length(): Long {
        return file.length()
    }

    override fun bytes(): ByteArray {
        return file.readBytes()
    }

}