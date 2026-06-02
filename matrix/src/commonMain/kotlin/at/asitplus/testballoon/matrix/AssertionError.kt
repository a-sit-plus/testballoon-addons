package at.asitplus

import kotlin.AssertionError as AssertionErr

internal class AssertionError(
    message: String,
    cause: AssertionErr,
) : AssertionErr(message, cause)