package nala.rlq.internal

import kotlin.js.Date

internal actual fun currentTimeMillis(): Long = Date().getTime().toLong()
