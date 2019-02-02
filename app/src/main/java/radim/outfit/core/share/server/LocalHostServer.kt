package radim.outfit.core.share.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import radim.outfit.DEBUG_MODE
import java.io.File
import java.io.FileInputStream
import java.lang.UnsupportedOperationException

const val MIME_JSON = "application/json"
const val MIME_FIT = "application/fit"
//const val MIME_HTML = "text/html"

class LocalHostServer(port: Int,
                      private val dir: File,
                      private val filenamesInOrder: List<String>,
                      private val filenamesToCoursenames: Map<String, String>): NanoHTTPD(port) {

    private val log: Logger = LoggerFactory.getLogger(NanoHTTPD::class.java)

    init{
        if(DEBUG_MODE) {
            log.info("filenamesInOrder")
            log.info(filenamesInOrder.toString())
            log.info("filenamesToCoursenames")
            log.info(filenamesToCoursenames.toString())
        }
    }

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        if(DEBUG_MODE) {
            log.info("session.remoteHostName: " + session.remoteHostName)
            log.info("session.remoteIpAddress: " + session.remoteIpAddress)
            log.info("session.uri: " + session.uri)
            log.info("session.headers.toString(): " + session.headers.toString())
            log.info("session.method.toString(): " + session.method.toString())
            log.info("session.parameters.toString(): " + session.parameters.toString())
            log.info("current state of dir: " + coursenamesAsJSON())
        }

        if(session.method.toString().equals("GET", ignoreCase = true)) {
            if (session.uri == "/outfit-dir.json") {
                return NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        MIME_JSON,
                        coursenamesAsJSON())
            } else if (session.uri.startsWith("/outfit-data")) {
                val uri = session.uri.substring(12, session.uri.length)
                if(DEBUG_MODE) log.info("asked to serve: $uri")
                try{
                    val fileToServe = File("${dir.absolutePath}$uri")
                    if(DEBUG_MODE) log.info("serving: ${fileToServe.absolutePath}")
                    val stream = FileInputStream(fileToServe)
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_FIT,
                            stream, fileToServe.length())
                }catch(e: Exception){
                    return errorResponse(e)
                }
            }
        }
        return errorResponse(UnsupportedOperationException())
    }

    // TODO org.json
    fun coursenamesAsJSON(): String {
        //val files = getListOfFitFilesRecursively(dir)
        val files = mutableListOf<File>()
        try{
            filenamesInOrder.forEach {
                val file = File("$dir${File.separator}$it")
                files.add(file)
            }
        }catch (e: Exception){
            Log.e("localhost", e.localizedMessage)
        }
        val sb = StringBuilder()
        sb.append("{\"courses\":[")
        files.forEach {
            val coursename = filenamesToCoursenames[it.name]
            if(coursename == null)Log.e("localhost", "coursename == null")
            val url = "${File.separator}${it.name}"
            sb.append(String.format("{\"name\":\"%s\",\"url\":\"%s\"},", coursename, url))
        }
        sb.replace(sb.length - 1, sb.length, "]}")
        return sb.toString()
    }

    private fun errorResponse(e: Exception): NanoHTTPD.Response {
        if(DEBUG_MODE)log.info("returning error response")
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, MIME_JSON,
                "{ \"error\" : \"${e.localizedMessage}\" }")
    }
}