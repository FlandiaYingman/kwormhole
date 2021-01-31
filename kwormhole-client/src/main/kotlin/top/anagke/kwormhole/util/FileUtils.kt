package top.anagke.kwormhole.util

import java.io.File

infix fun File.sameTo(file: File): Boolean {
    return this.canonicalPath == file.canonicalPath
}