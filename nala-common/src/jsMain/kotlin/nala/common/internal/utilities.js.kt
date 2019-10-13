package nala.common.internal

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date().getTime().toLong()
