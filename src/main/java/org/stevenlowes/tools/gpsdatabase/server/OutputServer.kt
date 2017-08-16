package org.stevenlowes.tools.gpsdatabase.server

import fi.iki.elonen.NanoHTTPD
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OutputServer(val localIp: String,
                   val port: Int,
                   val password: String,
                   val apiKey: String,
                   val mins: Int = 30,
                   val name: String = "GPS Tracker",
                   val externalIp: String = localIp) : NanoHTTPD(localIp, port) {

    constructor(localIp: String,
                port: Int,
                password: String,
                apiKey: String,
                mins: Int?,
                name: String?,
                externalIp: String?) : this(
            localIp,
            port,
            password,
            apiKey,
            mins ?: 30,
            name ?: "GPS Tracker",
            externalIp ?: localIp)

    companion object {
    }

    val LOGGER = LoggerFactory.getLogger(OutputServer::class.java)

    init {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        LOGGER.info("Output Server Running")
    }

    override fun serve(session: IHTTPSession): Response {
        try {
            val parms = session.parms

            if (parms["password"] == null || parms["password"] != password) {
                val htmlBuilder = StringJoiner(System.lineSeparator())
                htmlBuilder.add("<html><body><h1>$name</h1>")
                htmlBuilder.add("<form action='?' method='get'>").add("<p>Password: <input type='text' name='password'></p>").add(
                        "</form>")
                return newFixedLengthResponse(htmlBuilder.toString())
            }
            else {
                val script = StringJoiner(System.lineSeparator(),
                                          Database.readScript("locations") + System.lineSeparator(),
                                          "")

                if (parms["day"] != null) {
                    script.add("WHERE time >= ? AND time < ?")
                }
                else {
                    script.add("WHERE time > DATEADD('MINUTE', -?, NOW())")
                }

                if (parms["van"] != null) {
                    script.add("AND vans.name = ?")
                }


                val minutes: Int = parms["minutes"]?.toInt() ?: mins
                val dayString: String? = parms["day"]
                val vanParameter: String? = parms["van"]
                val noLinks: Boolean = if (parms["nolinks"] == "y") true else false
                val height = if (noLinks) 100 else 80

                Database.connection.prepareStatement(script.toString()).use {
                    var i = 1
                    if (parms["day"] != null) {
                        val date = LocalDate.parse(dayString)
                        it.setDate(i++, Date.valueOf(date))
                        it.setDate(i++, Date.valueOf(date.plusDays(1)))
                    }
                    else {
                        it.setInt(i++, minutes)
                    }
                    if (parms["van"] != null) {
                        it.setString(i++, vanParameter)
                    }
                    it.executeQuery().use {
                        val results: MutableMap<String, MutableList<Datapoint>> = mutableMapOf()

                        while (it.next()) {
                            val vanName = it.getString("van_name")
                            val list: MutableList<Datapoint> = results.getOrPut(vanName, { mutableListOf() })
                            list.add(Datapoint(it.getTimestamp("data.time").toLocalDateTime(),
                                               it.getBigDecimal("data.latitude"),
                                               it.getBigDecimal("data.longitude"),
                                               it.getBigDecimal("data.speed")))
                        }

                        val allValues = results.flatMap { it.value }

                        val count = allValues.size

                        val lats = allValues.map { it.lat }
                        val minLat = lats.min() ?: BigDecimal.ZERO
                        val maxLat = lats.max() ?: BigDecimal.ZERO
                        val latChange = maxLat - minLat
                        val latZoom = Math.round(Math.log(300 * 360 / latChange.toDouble() / 256) / Math.log(2.0))

                        val lons = allValues.map { it.lon }
                        val minLon = lons.min() ?: BigDecimal.ZERO
                        val maxLon = lons.max() ?: BigDecimal.ZERO
                        val lonChange = maxLon - minLon
                        val lonZoom = Math.round(Math.log(200 * 360 / lonChange.toDouble() / 256) / Math.log(2.0))

                        val latCent = (minLat + maxLat) / BigDecimal(2)
                        val lonCent = (minLon + maxLon) / BigDecimal(2)

                        val zoom = if (count == 0) 10 else minOf(maxOf(minOf(latZoom, lonZoom), 1), 18)

                        val htmlBuilder = StringJoiner(System.lineSeparator())
                        htmlBuilder.add("<!DOCTYPE html>").add("<html>").add("<head>").add("<style>").add("html { height: 100% }").add(
                                "body { height: 100%; margin: 0px; padding: 0px }").add("#map {").add("height: $height%;").add(
                                "width: 100%;").add("}").add("</style>").add("</head>").add("<body>")
                        if (!noLinks) {
                            htmlBuilder.add("<h1>$name</h1>")
                        }
                        htmlBuilder.add(
                                "<div id=\"map\"></div>").add("<script>").add("setTimeout(function () {").add("location.reload();").add(
                                "}, 60 * 1000);").add("function initMap() {").add("var center = {lat: $latCent, lng: $lonCent};").add(
                                "var map = new google.maps.Map(document.getElementById('map'), {").add("zoom: $zoom,").add(
                                "center: center").add(
                                "});")

                        val colors = createRainbow(results.size)
                        val vanColors = results.keys.zip(colors).toMap()

                        results.forEach { van, list ->
                            val color = vanColors[van]!!
                            val hexColor = String.format("#%02x%02x%02x", color.red, color.green, color.blue)
                            val lastLoc = list.maxBy { it.time }
                            val minsAgo = Duration.between(LocalDateTime.now(), lastLoc?.time)
                            if (lastLoc != null) {
                                htmlBuilder.add("var marker$van = new google.maps.Marker({").add("position: {lat: ${lastLoc.lat}, lng: ${lastLoc.lon}},").add(
                                        "label: '$van (${(lastLoc.speed * BigDecimal("2.2")).setScale(0,
                                                                                                      RoundingMode.HALF_UP)} mph) ($minsAgo ago)',").add(
                                        "map: map").add("});")
                            }

                            val iterator = list.sortedBy { it.time }.iterator()
                            htmlBuilder.add("var coordinates$van = [")
                            while (iterator.hasNext()) {
                                val next = iterator.next()
                                val string = "{lat: ${next.lat}, lng: ${next.lon}}"
                                if (iterator.hasNext()) {
                                    htmlBuilder.add("$string,")
                                }
                                else {
                                    htmlBuilder.add(string)
                                }
                            }
                            htmlBuilder.add("];").add("var path$van = new google.maps.Polyline({").add("path: coordinates$van,").add(
                                    "geodesic: true,").add("strokeColor: '$hexColor',").add("strokeOpacity: 1.0,").add("strokeWeight: 2").add(
                                    "});").add("path$van.setMap(map);")

                        }

                        htmlBuilder.add("}").add("</script>").add("<script async defer").add("src=\"https://maps.googleapis.com/maps/api/js?key=$apiKey&callback=initMap\">").add(
                                "</script>")

                        if (!noLinks) {
                            htmlBuilder
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&nolinks=y\">Hide Links</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&minutes=30\">Past 30 Mins</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&minutes=60\">Past Hour</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&minutes=1440\">Past 24 Hrs</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&minutes=10080\">Past Week</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&day=${LocalDate.now()}\">Today</a><br>")
                                    .add("<a href=\"http://$externalIp:$port/?password=$password&day=${LocalDate.now().minusDays(
                                            1)}\">Yesterday</a><br>")
                                    .add("<h2>Single Van View</h2><br>")

                            results.keys.sorted().forEach { htmlBuilder.add("<a href=\"http://$externalIp:$port/?password=$password&day=${LocalDate.now()}&van=$it\">Today: $it only</a><br>") }
                        }
                        htmlBuilder.add("</body>").add("</html>")
                        return newFixedLengthResponse(htmlBuilder.toString())
                    }
                }
            }
        }
        catch(e: Exception) {
            LOGGER.error("Exception thrown on output server", e)
            throw e
        }
    }
}