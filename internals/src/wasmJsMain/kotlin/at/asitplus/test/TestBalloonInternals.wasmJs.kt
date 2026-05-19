package at.asitplus.testballoon

internal actual fun compactProgressPrint(message: String) {
    @OptIn(ExperimentalWasmJsInterop::class)
    js("console.log(message)")
}