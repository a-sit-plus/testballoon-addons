package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped

/**
 * One implementation covers Node and the browser, which is the whole reason the wire format is HTTP:
 * `fetch` is global in both, while a raw TCP socket exists in neither browser nor (without a module import)
 * Wasm. `no-cors` keeps this a simple request, so Karma's page can post cross-port without a preflight.
 */
internal actual fun statusPost(host: String, port: Int, body: String): Boolean =
    catchingUnwrapped { postStatus("http://$host:$port/status", body); true }.getOrElse { false }

@Suppress("unused", "UNUSED_PARAMETER")
private fun postStatus(url: String, body: String) {
    js("fetch(url, { method: 'POST', mode: 'no-cors', keepalive: true, body: body }).catch(function () {})")
}
