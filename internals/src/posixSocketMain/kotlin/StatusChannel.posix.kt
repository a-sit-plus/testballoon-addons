package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.posix.close
import platform.posix.connect
import platform.posix.memset
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.write

/**
 * Winsock is deliberately not covered here: on mingw `platform.posix` hands out `SOCKET` (ULong) handles with
 * `send`/`closesocket` and requires a `WSAStartup` call first, so it needs its own actual.
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal actual fun statusPost(host: String, port: Int, body: String): Boolean =
    catchingUnwrapped {
        memScoped {
            val descriptor = socket(AF_INET, SOCK_STREAM, 0)
            if (descriptor < 0) return@catchingUnwrapped false

            val address = alloc<sockaddr_in>()
            memset(address.ptr, 0, sizeOf<sockaddr_in>().convert())
            address.sin_family = AF_INET.convert()
            address.sin_port = port.toNetworkOrderPort()
            address.sin_addr.s_addr = host.toLoopbackAddress() ?: run {
                close(descriptor)
                return@catchingUnwrapped false
            }

            if (connect(descriptor, address.ptr.reinterpret<sockaddr>(), sizeOf<sockaddr_in>().convert()) != 0) {
                close(descriptor)
                return@catchingUnwrapped false
            }

            val request = statusRequest(host, port, body).encodeToByteArray()
            var written = 0
            request.usePinned { pinned ->
                while (written < request.size) {
                    val count = write(descriptor, pinned.addressOf(written), (request.size - written).convert())
                    if (count <= 0) break
                    written += count.toInt()
                }
            }
            close(descriptor)
            written == request.size
        }
    }.getOrElse { false }
