package at.asitplus.testballoon

internal actual fun compactProgressPrint(message: String) {
    @OptIn(ExperimentalWasmJsInterop::class)
    js("console.log(message)")
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun compactSummaryPrint(message: String) {
    // stdout is the kotlin-js TeamCity service-message channel; console.error → stderr stays off the protocol
    // while remaining observable and captured as testStdErr.
    js("console.error(message)")
}