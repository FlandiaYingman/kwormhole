package top.anagke.kwormhole.model

import org.apache.tomcat.util.http.parser.ContentRange
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import top.anagke.kwormhole.util.urlPathToStore
import java.io.File
import java.io.RandomAccessFile

@Component
class FileContentStore {

    companion object {

        private const val STORE_LOCATION = "./store/"

        private fun String.toLocalFile(): File {
            if (File(this).isAbsolute) throw IllegalArgumentException("$this should not be absolute")
            return File(STORE_LOCATION).resolve(this.urlPathToStore()).normalize().absoluteFile
        }

    }

    // TODO: NOT TESTED 2020/12/16
    fun storeFile(path: String, fileParts: ByteArray) {
        val localFile = path.toLocalFile()
        localFile.parentFile.mkdirs()
        localFile.writeBytes(fileParts)
    }

    // TODO: NOT TESTED 2020/12/16
    fun storePart(path: String, bytes: ByteArray, range: ContentRange) {
        val localFile = path.toLocalFile()
        localFile.parentFile.mkdirs()

        val raFile = RandomAccessFile(localFile, "rws")
        raFile.seek(range.start)
        raFile.write(bytes)
    }


    // TODO: NOT TESTED 2020/12/16
    fun existsFile(path: String): Boolean {
        val localFile = path.toLocalFile()
        return localFile.exists()
    }

    // TODO: NOT TESTED 2020/12/16
    fun getFile(path: String): Resource? {
        val localFile = path.toLocalFile()
        return if (localFile.exists()) {
            ByteArrayResource(localFile.readBytes())
        } else {
            null
        }
    }

}