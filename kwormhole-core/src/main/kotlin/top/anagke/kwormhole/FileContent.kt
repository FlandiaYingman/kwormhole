package top.anagke.kwormhole

import java.io.InputStream

interface FileContent {

    fun hash(): Long

    fun length(): Long

    fun openStream(): InputStream

}