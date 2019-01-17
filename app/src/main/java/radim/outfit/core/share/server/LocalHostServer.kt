package radim.outfit.core.share.server

import fi.iki.elonen.NanoHTTPD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

const val MIME_JSON = "application/json"
const val MIME_FIT = "application/fit"
const val MIME_HTML = "text/html"

class LocalHostServer(port: Int, dir: File): NanoHTTPD(port) {

    private val log: Logger = LoggerFactory.getLogger(NanoHTTPD::class.java)

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        log.info("session.remoteHostName: " + session.remoteHostName)
        log.info("session.remoteIpAddress: " + session.remoteIpAddress)
        log.info("session.uri: " + session.uri)
        log.info("session.headers.toString(): " + session.headers.toString())
        log.info("session.method.toString(): " + session.method.toString())
        log.info("session.parameters.toString(): " + session.parameters.toString())
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_JSON, "{ \"error\" : \"No permission or no files\" } ")
    }


    private fun errorResponse(e: Exception): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_JSON,
                "{ \"error\" : \"" + e.toString() + "\" } ")
    }

}