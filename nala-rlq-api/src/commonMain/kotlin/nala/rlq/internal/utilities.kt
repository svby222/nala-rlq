package nala.rlq.internal

internal inline operator fun Unit.invoke(block: () -> Any?) {
    block()
}

internal expect fun currentTimeMillis(): Long
