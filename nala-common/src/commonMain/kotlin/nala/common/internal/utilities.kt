package nala.common.internal

inline operator fun Unit.invoke(block: () -> Any?) {
    block()
}

expect fun currentTimeMillis(): Long
