package top.anagke.kwormhole.util

import java.io.File

infix fun File.matches(file: File): Boolean {
    return this.canonicalPath == file.canonicalPath
}