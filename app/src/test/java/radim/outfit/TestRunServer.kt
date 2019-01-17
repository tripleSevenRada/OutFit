package radim.outfit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import radim.outfit.core.share.server.LocalHostServer
import java.io.File
import fi.iki.elonen.NanoHTTPD


    private val log: Logger = LoggerFactory.getLogger(NanoHTTPD::class.java)

    fun main(args: Array<String>) {
        val from = "/home/radim/nano-httpd-serve-from"
        try {
            val server = LocalHostServer(NANOHTTPD_PORT,
                    File(from)
            )
            server.start()
            log.info("serving " + from)
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }

        try {
            while (true) {
                Thread.sleep(10000)
            }
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }

    }
