package at.asitplus.testballoon

actual val String.escaped: String get() = this //= replace('/', '⧸')
actual  val DEFAULT_TEST_NAME_MAX_LEN : Int = 10
