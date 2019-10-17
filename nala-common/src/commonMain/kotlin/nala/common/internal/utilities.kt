package nala.common.internal

inline operator fun Unit.invoke(block: () -> Any?) {
    block()
}

/** Returns the current system time in Unix epoch milliseconds. */
expect fun currentTimeMillis(): Long
