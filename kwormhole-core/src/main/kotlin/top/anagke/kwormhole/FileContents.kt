package top.anagke.kwormhole

import java.io.File

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