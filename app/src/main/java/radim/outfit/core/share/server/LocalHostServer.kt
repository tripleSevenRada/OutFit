package radim.outfit.core.share.server

import fi.iki.elonen.NanoHTTPD

const val MIME_JSON = "application/json"
const val MIME_FIT = "application/fit"
const val MIME_HTML = "text/html"

class LocalHostServer(port: Int): NanoHTTPD(port) {

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse("<html><body><h1>Hallo world OUT FIT here</h1></body></html>")
    }
}