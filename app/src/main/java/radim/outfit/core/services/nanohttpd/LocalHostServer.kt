package radim.outfit.core.services.nanohttpd

import fi.iki.elonen.NanoHTTPD


class LocalHostServer(port: Int): NanoHTTPD(port) {

    private val MIME_JSON = "application/json"
    private val MIME_FIT = "application/fit"
    private val MIME_HTML = "text/html"

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse("<html><body><h1>Hallo world OUT FIT here</h1></body></html>\n")
    }
}