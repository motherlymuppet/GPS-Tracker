package org.stevenlowes.tools.gpsdatabase.server

import fi.iki.elonen.NanoHTTPD
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.json.Json

class InputServer(ip: String, port: Int) : NanoHTTPD(ip, port) {

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        println("Input Server Running")
    }

    override fun serve(session: IHTTPSession): Response {
        val parms = session.parms

        val vanId: Long = parms["vanId"]?.toLong() ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Van ID Supplied")
        val latitudeString: String = parms["latitude"] ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Latitude Supplied")
        val longitudeString: String = parms["longitude"] ?: return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Longitude Supplied")

        Database.prepareScript("insert").use {
            it.setLong(1, vanId)
            it.setBigDecimal(2, BigDecimal(latitudeString))
            it.setBigDecimal(3, BigDecimal(longitudeString))
            it.execute()
            return NanoHTTPD.newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain", "Successful")
        }

    }
}