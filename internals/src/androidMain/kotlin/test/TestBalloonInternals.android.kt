package at.asitplus.testballoon

import android.util.Log


actual var totalMaxLen: Int = -1

internal actual fun compactProgressConsolePrint(message: String) {
   Log.i("TestRunner", message)
   System.err.println(message)         // captured stream for local/Robolectric unit tests
}

internal actual fun compactSummaryConsolePrint(message: String) {
   Log.i("TestRunner", message)        // logcat, always observable on device
   System.err.println(message)         // captured stream for local/Robolectric unit tests
}
