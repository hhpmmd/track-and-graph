/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.util

import java.lang.NumberFormatException

/**
 * Return a number given a string by attempting to parse it as a double
 *
 * This function should account for different langauge punctuation formats. e.g.
 * French: 1.345.6,04
 * English: 1,345,6.04
 * Both of these inputs will return 13456.04
 *
 * This function will always return a number rather than throw an exception. If the number
 * couldn't be parsed then the returned value will default to 0
 */
fun getDoubleFromText(text: String): Double {
    try {
        //first account for values like 1,345,100 or French: 1.345.100
        val commaCount = text.count { c -> c == ',' }
        val dotCount = text.count { c -> c == '.' }
        if ((commaCount == 0 && dotCount > 1) || (dotCount == 0 && commaCount > 1))
            return text.replace(",", "").replace(".", "").toDouble()

        //If there is a mixture of commas and dots convert all commas to dots
        val dotsOnly = text.replace(",", ".")
        val lastDot = dotsOnly.indexOfLast { c -> c == '.' }
        //If there are no commas or dots then there is no decimal place
        if (lastDot < 0) return dotsOnly.toDouble()

        //If there is at least one comma or dot then the last one represents the decimal place
        val before = dotsOnly.substring(0, lastDot).replace(".", "")
        val after = dotsOnly.substring(lastDot)
        return "$before$after".toDouble()
    } catch (e: NumberFormatException) { return 0.0 }
}
