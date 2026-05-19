package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import java.io.FileDescriptor
import java.io.FileOutputStream

private val compactProgressStderr = FileOutputStream(FileDescriptor.out)

internal actual fun compactProgressPrint(message: String) {
    catchingUnwrapped {
        compactProgressStderr.write((message + System.lineSeparator()).encodeToByteArray())
        compactProgressStderr.flush()
    }
}
