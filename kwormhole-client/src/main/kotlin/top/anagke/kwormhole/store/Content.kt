package top.anagke.kwormhole.store

import top.anagke.kwormhole.util.hash

interface Content {

    fun length(): Long

    fun bytes(): ByteArray


    fun hash(): Long {
        return bytes().hash()
    }

}