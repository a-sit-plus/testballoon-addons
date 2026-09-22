package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.testPlatform
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val StatusChannelTest by testSuite {

    test("status channel stays best-effort whether or not the build opened a listener") {
        repeat(3) { round ->
            StatusChannel.send("${testPlatform.displayName} ↘ Σheartbeat: ${round + 1}/3 source cases registered")
        }

        // With the `at.asitplus.testballoon.addons` Gradle plugin applied these lines surface in the build
        // output. Without it -- as in this project, which cannot apply a plugin it also builds -- the channel
        // never comes up live. Neither case may throw or fail a run.
        if (!StatusChannel.isLive) StatusChannel.send("unreachable") shouldBe false
    }
}
