package org.stevenlowes.tools.gpsdatabase.server

import java.awt.Color
import kotlin.coroutines.experimental.buildSequence

fun createRainbow(colors: Int): List<Color> {
    val inc = 1.0 / colors
    return buildSequence {
        for (i in 1..colors) {
            val hue = i * inc
            yield(Color.getHSBColor(hue.toFloat(), 1f, 1f))
        }
    }.toList()
}