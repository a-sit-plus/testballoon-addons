package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr
import platform.posix.stdout

@OptIn(ExperimentalForeignApi::class)
internal actual fun compactProgressPrint(message: String) {
    catchingUnwrapped {
        fprintf(stdout, "%s\n", message)
        fflush(stdout)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun compactSummaryPrint(message: String) {
    // stdout is the native TeamCity service-message channel; writing there corrupts the report parser.
    // stderr is always observable and stays off the protocol.
    catchingUnwrapped {
        fprintf(stderr, "%s\n", message)
        fflush(stderr)
    }
}
