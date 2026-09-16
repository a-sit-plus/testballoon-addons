package at.asitplus.testballoon

internal actual fun compactProgressConsolePrint(message: String) {
    printMessage(message)
}


internal actual fun compactSummaryConsolePrint(message: String) {
    printMessage(message)

}

@OptIn(ExperimentalWasmJsInterop::class)
private fun printMessage(serviceMessage: String) {
    js("console.log(serviceMessage)")
}
@OptIn(ExperimentalWasmJsInterop::class)
private fun printErrMessage(serviceMessage: String) {
    js("console.error(serviceMessage)")
}