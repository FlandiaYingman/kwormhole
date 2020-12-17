package top.anagke.kwormhole.util


fun String.urlPathToRepoFolder() = if (this.endsWith('/')) this else "$this/"
fun String.urlPathToRepoFile() = this.trimEnd('/')


fun String.urlPathToStore() = this.trimStart('/')
