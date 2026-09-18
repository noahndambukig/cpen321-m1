package com.noah.demo.data

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * The device's local time in the same shape the back-end reports, so the two
 * clocks on screen are directly comparable: `hh:mm:ss GMT+hh:mm`.
 */
fun currentClientTime(now: ZonedDateTime = ZonedDateTime.now()): String {
    // ZoneOffset renders UTC as "Z"; M1 wants an explicit numeric offset.
    val offset = now.offset.id.let { if (it == "Z") "+00:00" else it }
    return "${now.format(CLOCK_FORMAT)} GMT$offset"
}
