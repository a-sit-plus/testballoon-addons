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

internal actual fun compactSummaryPrint(message: String) {
    // Raw FD: always visible on the real console even when the runner captures System.out/err.
    catchingUnwrapped {
        compactProgressStderr.write((message + System.lineSeparator()).encodeToByteArray())
        compactProgressStderr.flush()
    }
    // Captured stream: Gradle/IDE attach System.err to the test as <system-err>, so the count shows in reports.
    catchingUnwrapped { System.err.println(message) }
}
