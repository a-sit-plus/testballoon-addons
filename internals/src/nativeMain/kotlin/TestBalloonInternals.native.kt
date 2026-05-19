package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stdout

@OptIn(ExperimentalForeignApi::class)
internal actual fun compactProgressPrint(message: String) {
    catchingUnwrapped {
        fprintf(stdout, "%s\n", message)
        fflush(stdout)
    }
}
