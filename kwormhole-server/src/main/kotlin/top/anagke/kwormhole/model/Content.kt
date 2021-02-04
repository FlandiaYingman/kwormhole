package top.anagke.kwormhole.model

import java.io.File
import java.io.InputStream

interface Content {

    companion object {

        fun ofFile(file: File): Content {
            return FileContent(file)
        }

        fun ofStream(stream: InputStream): Content {
            return TempFileContent(stream)
        }

    }


    fun length(): Long

    fun openStream(): InputStream

    fun asFileContent(): FileContent


    open class FileContent(val file: File) : Content {

        init {
            require(file.exists()) { "$file is required to exist" }
            require(file.isFile()) { "$file is required to be file" }
        }

        override fun length(): Long {
            return file.length()
        }

        override fun openStream(): InputStream {
            return file.inputStream()
        }

        override fun asFileContent(): FileContent {
            return this
        }

    }

    private class TempFileContent(stream: InputStream) : FileContent(File.createTempFile("temp_content_", null)) {

        init {
            file.deleteOnExit()
            file.outputStream().use { stream.copyTo(it) }
        }

        protected fun finalize() {
            if (file.exists()) file.delete()
        }

    }

}