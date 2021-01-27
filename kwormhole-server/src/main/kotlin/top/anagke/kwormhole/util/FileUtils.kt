@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package top.anagke.kwormhole.util

import java.io.File

fun File.createParents() {
    this.parentFile.mkdirs()
}

fun File.deleteParents(base: File) {
    val canonicalBase = base.canonicalFile
    var canonicalParent = this.parentFile.canonicalFile
    while (
        canonicalParent != canonicalBase &&
        canonicalParent.startsWith(canonicalBase)
    ) {
        if (canonicalParent.list()?.isEmpty() ?: false) {
            canonicalParent.delete()
        }
        canonicalParent = canonicalParent.parentFile.canonicalFile
    }
}
