package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped

internal actual fun statusPost(host: String, port: Int, body: String): Boolean =
    catchingUnwrapped { postStatus("http://$host:$port/status", body); true }.getOrElse { false }

@OptIn(ExperimentalWasmJsInterop::class)
private fun postStatus(url: String, body: String) {
    js("fetch(url, { method: 'POST', mode: 'no-cors', keepalive: true, body: body }).catch(function () {})")
}
