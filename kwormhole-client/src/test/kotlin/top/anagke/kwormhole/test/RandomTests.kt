package top.anagke.kwormhole.test

import java.math.BigInteger
import kotlin.random.Random

fun Random.nextHexString(length: Int): String {
    return this.nextBytes(length * 16).let {
        BigInteger(1, it).toString(16).takeLast(length)
    }
}