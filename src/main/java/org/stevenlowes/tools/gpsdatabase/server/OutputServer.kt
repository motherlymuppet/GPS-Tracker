package org.stevenlowes.tools.gpsdatabase.server

import fi.iki.elonen.NanoHTTPD
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.json.Json

class OutputServer(ip: String, port: Int, val password: String) : NanoHTTPD(ip, port) {

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        println("Output Server Running")
    }

    override fun serve(session: IHTTPSession): Response {
        val parms = session.parms

        if (parms["password"] != password) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.UNAUTHORIZED,
                                                    "text/plain",
                                                    "Incorrect Password")
        }
        else {
            val minutes: Int = parms["mins"]?.toInt() ?: 30

            Database.prepareScript("locations").use {
                it.setInt(1, minutes)
                it.executeQuery().use {
                    val results: MutableMap<String, MutableList<Triple<LocalDateTime, BigDecimal, BigDecimal>>> = mutableMapOf()

                    while (it.next()) {
                        val vanName = it.getString("vans.name")
                        val list: MutableList<Triple<LocalDateTime, BigDecimal, BigDecimal>> = results.getOrPut(vanName,
                                                                                                                { mutableListOf() })
                        list.add(Triple(it.getTimestamp("data.time").toLocalDateTime(),
                                        it.getBigDecimal("data.latitude"),
                                        it.getBigDecimal("data.longitude")))
                    }

                    val jsonBuilder = Json.createObjectBuilder()

                    results.forEach { vanName, list ->
                        val arrayBuilder = Json.createArrayBuilder()
                        list.sortedBy { it.first }.forEach { (time, latitude, longitude) ->
                            val innerBuilder = Json.createObjectBuilder()
                            innerBuilder.add("timestamp", time.toString())
                            innerBuilder.add("latitude", latitude.toString())
                            innerBuilder.add("longitude", longitude.toString())
                            arrayBuilder.add(innerBuilder)
                        }
                        jsonBuilder.add(vanName, arrayBuilder)
                    }

                    return NanoHTTPD.newFixedLengthResponse(Response.Status.ACCEPTED,
                                                            "application/json",
                                                            jsonBuilder.build().toString())
                }
            }
        }

    }
}