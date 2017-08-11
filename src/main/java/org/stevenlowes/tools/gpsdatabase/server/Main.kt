package org.stevenlowes.tools.gpsdatabase.server

import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.stevenlowes.tools.randomspicegenerator.database.utils.fireVanEditor
import java.util.logging.Level
import java.util.logging.LogManager

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")

    val log = LogManager.getLogManager().getLogger("")
    for (h in log.handlers) {
        h.level = Level.ALL
    }

    val LOGGER = LoggerFactory.getLogger("Main")

    Database.connect()

    var ip: String? = null
    var inPort: Int? = null
    var outPort: Int? = null
    var name: String? = null
    var mins: Int? = null
    var pass: String? = null
    var apiKey: String? = null
    var externalIp: String? = null

    val iterator = args.iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        when (next) {
            "-h" -> {
                printHelp()
                return
            }
            "-v" -> fireVanEditor()
            "-i" -> {
                if (iterator.hasNext()) {
                    ip = iterator.next()
                    LOGGER.info("IP: $ip")
                }
                else {
                    throw IllegalArgumentException("-i must be followed by the local localIp for the server")
                }
            }
            "-p" -> {
                if (iterator.hasNext()) {
                    inPort = iterator.next().toInt()
                    LOGGER.info("INPUT PORT: $inPort")
                    if (iterator.hasNext()) {
                        outPort = iterator.next().toInt()
                        LOGGER.info("OUTPUT PORT: $outPort")
                    }
                    else {
                        throw IllegalArgumentException("-p must be followed by two parameters, the port for the input server, then the port for the output server")
                    }
                }
                else {
                    throw IllegalArgumentException("-p must be followed by two parameters, the port for the input server, then the port for the output server")
                }
            }
            "-n" -> {
                if (iterator.hasNext()) {
                    name = iterator.next()
                    LOGGER.info("NAME: $name")
                }
                else {
                    throw IllegalArgumentException("-n must be followed by the name for the gps display")
                }
            }
            "-m" -> {
                if (iterator.hasNext()) {
                    mins = iterator.next().toInt()
                    LOGGER.info("MINS: $mins")
                }
                else {
                    throw IllegalArgumentException("-m must be followed by the default number of minutes of data to show on the output")
                }
            }
            "-pw" -> {
                if (iterator.hasNext()) {
                    pass = iterator.next()
                    LOGGER.info("PASS: $pass")
                }
                else {
                    throw IllegalArgumentException("-pw must be followed by the password for the output server")
                }
            }
            "-a" -> {
                if (iterator.hasNext()) {
                    apiKey = iterator.next()
                    LOGGER.info("API KEY: $apiKey")
                }
                else {
                    throw IllegalArgumentException("-a must be followed by the google maps javascript api key")
                }
            }
            "-e" -> {
                if (iterator.hasNext()) {
                    externalIp = iterator.next()
                    LOGGER.info("EXTERNAL IP: $externalIp")
                }
                else {
                    throw IllegalArgumentException("-e must be followed by the external ip address")
                }
            }
        }
    }

    if (ip == null || inPort == null || outPort == null || pass == null || apiKey == null) {
        throw IllegalArgumentException("-i, -p, -a, and -pw are mandatory parameters. Add -h to view help")
    }

    val inputServer = InputServer(ip, inPort)
    val outputServer = OutputServer(ip, outPort, pass, apiKey, mins, name, externalIp)

    LOGGER.info("Output Server Link: http://${outputServer.externalIp}:$outPort")
    LOGGER.info("Output Server Link (pass-free): http://${outputServer.externalIp}:$outPort/?password=$pass")

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            inputServer.closeAllConnections()
            inputServer.stop()
            outputServer.closeAllConnections()
            outputServer.stop()
        }
    })
}

fun printHelp() {
    println("Help:")
    println("-i <localIp> : The Local IP address of the server")
    println("-p <port> <port> The ports for the input and output servers respectively")
    println("-pw <password> : The password for the output server")
    println("-a <apiKey> : The google maps javascript api key")
    println("-h : Show this message")
    println("-v : Add names to vans")
    println("-n <name> : The name to show on the output server")
    println("-m <mins> : The number of minutes of history to show")
}
