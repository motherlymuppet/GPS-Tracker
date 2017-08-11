package org.stevenlowes.tools.gpsdatabase.server

import java.math.BigDecimal
import java.time.LocalDateTime

data class Datapoint(val time: LocalDateTime, val lat: BigDecimal, val lon: BigDecimal, val speed: BigDecimal)