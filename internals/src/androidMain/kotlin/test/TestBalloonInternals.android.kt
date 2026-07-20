package at.asitplus.testballoon

import android.util.Log


actual var totalMaxLen: Int = -1

internal actual fun compactProgressPrint(message: String) {
   Log.i("TestRunner", message)
}

internal actual fun compactSummaryPrint(message: String) {
   Log.i("TestRunner", message)        // logcat, always observable on device
   System.err.println(message)         // captured stream for local/Robolectric unit tests
}
