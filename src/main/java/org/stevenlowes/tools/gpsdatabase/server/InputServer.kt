package org.stevenlowes.tools.gpsdatabase.server

import fi.iki.elonen.NanoHTTPD
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

class InputServer(ip: String, port: Int) : NanoHTTPD(ip, port) {

    companion object {
        val LOGGER = LoggerFactory.getLogger(InputServer::class.java)
    }

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        LOGGER.info("Input Server Running")
    }

    override fun serve(session: IHTTPSession): Response {
        try {
            val parms = session.parms

            val imei: String? = parms["imei"]

            if (imei == null) {
                LOGGER.debug("Failed - No imei")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Van ID Supplied")
            }

            val longImei = imei.toLong()

            val locationString: String? = parms["loc"]

            if (locationString == null) {
                LOGGER.debug("Failed - No Location")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Location Supplied")
            }

            val speed: String? = parms["spd"]

            if (speed == null) {
                LOGGER.debug("Failed - No Speed")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No Speed Supplied")
            }

            val location = locationString.split(",")

            Database.connection.prepareStatement("MERGE INTO vans (imei) VALUES (?)").use {
                it.setLong(1, longImei)
                it.execute()
            }

            Database.prepareScript("insert").use {
                it.setLong(1, longImei)
                it.setBigDecimal(2, BigDecimal(location[0]))
                it.setBigDecimal(3, BigDecimal(location[1]))
                it.setBigDecimal(4, BigDecimal(speed))
                it.execute()

                val name = Database.connection.prepareStatement("SELECT vans.name FROM vans WHERE vans.imei = ?").use {
                    it.setLong(1, longImei)
                    it.executeQuery().use {
                        it.next()
                        it.getNullableString("name")
                    }
                }
                LOGGER.debug("${LocalDateTime.now()} INPUT RECEIVED VAN NAME ${name ?: imei} LOCATION $locationString SPEED $speed")

                return newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain", "Successful")
            }
        }
        catch(e: Exception) {
            LOGGER.error("Exception Throw on input server", e)
            throw e
        }
    }


}