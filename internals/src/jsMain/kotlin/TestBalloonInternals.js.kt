package at.asitplus.testballoon

internal actual fun compactProgressPrint(message: String) {
    console.error(message.teamCityEscape())
}

internal actual fun compactSummaryPrint(message: String) {
    console.error(message.teamCityEscape())
}
