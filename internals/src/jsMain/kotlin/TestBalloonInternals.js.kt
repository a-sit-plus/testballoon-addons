package at.asitplus.testballoon

internal actual fun compactProgressPrint(message: String) {
    console.log(message)
}

internal actual fun compactSummaryPrint(message: String) {
    // stdout is the kotlin-js TeamCity service-message channel; writing there corrupts the report parser.
    // console.error → stderr: always observable AND captured as testStdErr, while staying off the protocol.
    console.error(message)
}
