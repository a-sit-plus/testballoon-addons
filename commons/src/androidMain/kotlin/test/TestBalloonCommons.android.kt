package at.asitplus.testballoon

actual val String.escaped: String get() = replace('/', '⧸')