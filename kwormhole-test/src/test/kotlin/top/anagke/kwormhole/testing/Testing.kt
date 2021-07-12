package top.anagke.kwormhole.testing

import top.anagke.kwormhole.util.Hasher
import java.io.File

fun rootsEqual(root1: File, root2: File): Boolean {
    return correspondsFileTree(root1, root2) && correspondsFileTree(root2, root1)
}

fun correspondsFileTree(src: File, dst: File): Boolean {
    val fileTreeMap = src.walk()
        .filter { it.isFile }
        .map { it to dst.resolve(it.relativeTo(src)) }
        .toMap()
    return fileTreeMap.all { (srcFile, dstFile) ->
        (srcFile.exists() && dstFile.exists()) && (Hasher.hash(srcFile) == Hasher.hash(dstFile))
    }
}