package at.asitplus.testballoon

internal actual fun compactProgressPrint(message: String) {
    printErrMessage(message.teamCityEscape())
}


internal actual fun compactSummaryPrint(message: String) {
    printErrMessage(message.teamCityEscape())

}

@OptIn(ExperimentalWasmJsInterop::class)
private fun printMessage(serviceMessage: String) {
    js("console.log(serviceMessage)")
}
@OptIn(ExperimentalWasmJsInterop::class)
private fun printErrMessage(serviceMessage: String) {
    js("console.error(serviceMessage)")
}