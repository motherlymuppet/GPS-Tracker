package org.stevenlowes.tools.gpsdatabase.server

import fi.iki.elonen.NanoHTTPD
import org.slf4j.LoggerFactory
import java.io.FileInputStream

class AppServer(localIp: String,
                port: Int
               ) : NanoHTTPD(localIp, port) {

    companion object {
    }

    val LOGGER = LoggerFactory.getLogger(OutputServer::class.java)

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        LOGGER.info("App Server Running")
    }

    override fun serve(session: IHTTPSession): Response {
        try {
            val filename = "gps_tracker.apk"
            val inputStream = FileInputStream(filename)
            val response = NanoHTTPD.newChunkedResponse(Response.Status.OK,
                                                        "application/vnd.android.package-archive",
                                                        inputStream)
            response.addHeader("Content-Disposition", "attachment; filename=\"$filename\"")
            return response
        }
        catch(e: Exception) {
            LOGGER.error("Exception thrown on output server", e)
            throw e
        }
    }
}