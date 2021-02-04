package top.anagke.kwormhole.model.store

import top.anagke.kwormhole.model.Content
import top.anagke.kwormhole.util.createParents
import top.anagke.kwormhole.util.deleteParents
import java.io.File

internal class ContentStore(
    val location: File
) {

    fun getContent(path: String): Content {
        require(exists(path)) { "The path $path is required to exist." }
        return Content.ofFile(path.toActualPath())
    }

    fun putContent(path: String, content: Content) {
        val actualPath = path.toActualPath()
        actualPath.createParents()
        actualPath.outputStream().use { output ->
            content.openStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    fun deleteContent(path: String) {
        val actualPath = path.toActualPath()
        actualPath.delete()
        actualPath.deleteParents(location)
    }

    fun exists(path: String): Boolean {
        return path.toActualPath().exists()
    }


    private fun String.toActualPath(): File {
        return location.resolve(this.trimStart('/')).canonicalFile
    }

}