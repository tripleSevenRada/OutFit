package radim.outfit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import radim.outfit.core.share.server.LocalHostServer
import java.io.File
import fi.iki.elonen.NanoHTTPD
import org.junit.Test

private val log: Logger = LoggerFactory.getLogger(NanoHTTPD::class.java)

class TestRunServer {

    @Test
    fun testRun() {
        val from = "/home/radim/nano-httpd-serve-from"
        try {
            val server = LocalHostServer(NANOHTTPD_PORT,
                    File(from),
                    listOf("course2.fit",
                            "course1.fit",
                            "course3.fit",
                            "course4.fit",
                            "course5.fit",
                            "course6.fit",
                            "course7.fit"),
                    mapOf("course1.fit" to "course1",
                            "course2.fit" to "course2",
                            "course3.fit" to "course3",
                            "course4.fit" to "course4",
                            "course5.fit" to "course5",
                            "course6.fit" to "course6",
                            "course7.fit" to "course7"
                    )
            )
            log.info("JSONArray: ${server.coursenamesAsJSON()}")
            server.start()
            log.info("serving from: $from")
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }

        try {
            while (true) {
                Thread.sleep(10000)
                log.debug("still serving")
            }
        } catch (e: Exception) {
            log.error(e.localizedMessage)
        }
    }
}