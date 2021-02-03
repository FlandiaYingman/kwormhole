package top.anagke.kwormhole.model.store

import top.anagke.kwormhole.util.createParents
import top.anagke.kwormhole.util.deleteParents
import java.io.File
import java.util.*

internal class ContentStore(
    val location: File
) {

    fun getContent(path: String): ByteArray {
        requireExists(path)
        return path.toActualPath().readBytes()
    }

    fun putContent(path: String, content: ByteArray) {
        path.toActualPath().createParents()
        return path.toActualPath().writeBytes(content)
    }

    fun deleteContent(path: String) {
        requireExists(path)
        path.toActualPath().delete()
        path.toActualPath().deleteParents(location)
    }

    fun exists(path: String): Boolean {
        return path.toActualPath().exists()
    }

    fun notExists(path: String): Boolean {
        return !exists(path)
    }


    private fun requireExists(path: String) {
        if (this.notExists(path)) {
            throw NoSuchElementException("The content with path '$path' doesn't exist.")
        }
    }


    private fun String.toActualPath(): File {
        return location.resolve(this.trimStart('/')).canonicalFile
    }

}