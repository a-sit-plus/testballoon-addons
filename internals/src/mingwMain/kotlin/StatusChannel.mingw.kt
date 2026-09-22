package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.posix.AF_INET
import platform.posix.INVALID_SOCKET
import platform.posix.IPPROTO_TCP
import platform.posix.SOCKET_ERROR
import platform.posix.SOCK_STREAM
import platform.posix.WSAData
import platform.posix.WSAStartup
import platform.posix.closesocket
import platform.posix.connect
import platform.posix.memset
import platform.posix.send
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket

/**
 * Winsock rather than POSIX: mingw's `platform.posix` hands out `SOCKET` (ULong) handles, sends through
 * `send`, closes with `closesocket`, addresses live behind the `in_addr` union, and nothing works at all until
 * `WSAStartup` has run. Hence a separate actual from the shared POSIX one.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun statusPost(host: String, port: Int, body: String): Boolean =
    catchingUnwrapped {
        if (!winsockStarted()) return@catchingUnwrapped false

        memScoped {
            val descriptor = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
            if (descriptor == INVALID_SOCKET) return@catchingUnwrapped false

            val address = alloc<sockaddr_in>()
            memset(address.ptr, 0, sizeOf<sockaddr_in>().convert())
            address.sin_family = AF_INET.toShort()
            address.sin_port = port.toNetworkOrderPort()
            address.sin_addr.S_un.S_addr = host.toLoopbackAddress() ?: run {
                closesocket(descriptor)
                return@catchingUnwrapped false
            }

            val size = sizeOf<sockaddr_in>().convert<Int>()
            if (connect(descriptor, address.ptr.reinterpret<sockaddr>(), size) == SOCKET_ERROR) {
                closesocket(descriptor)
                return@catchingUnwrapped false
            }

            val request = statusRequest(host, port, body).encodeToByteArray()
            var written = 0
            request.usePinned { pinned ->
                while (written < request.size) {
                    val count = send(descriptor, pinned.addressOf(written), request.size - written, 0)
                    if (count <= 0) break
                    written += count
                }
            }
            closesocket(descriptor)
            written == request.size
        }
    }.getOrElse { false }

private var winsockAvailable: Boolean? = null

/** `WSAStartup` is reference counted, so a benign race here costs nothing. */
@OptIn(ExperimentalForeignApi::class)
private fun winsockStarted(): Boolean = winsockAvailable ?: memScoped {
    val data = alloc<WSAData>()
    val started = WSAStartup(MAKEWORD_2_2, data.ptr) == 0
    winsockAvailable = started
    started
}

/** MAKEWORD(2, 2) -- Winsock 2.2. */
private const val MAKEWORD_2_2: UShort = 0x0202u
