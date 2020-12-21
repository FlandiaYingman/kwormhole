package top.anagke.kwormhole.util

fun String.splitPair(delimiter: String): Pair<String, String> {
    return this.substringBefore(delimiter) to this.substringAfter(delimiter)
}