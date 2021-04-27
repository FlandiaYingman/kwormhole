package top.anagke.kwormhole.test

import java.io.File
import kotlin.random.Random

const val TEST_ENTRY_COUNT = 8
const val TEST_ENTRY_LENGTH = 64

const val TEST_FILE_NAME_LENGTH = 8
const val TEST_FILE_PATH_DEPTH = 8

val TEST_DIR = File("./test")
val TEST_SERVER_PORT = Random.nextInt(60000, 65536)