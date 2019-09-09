package nala.rlq.internal

internal fun <T> infiniteSequenceOf(value: T) = object : Sequence<T> {

    private val iterator = object : Iterator<T> {
        override fun hasNext() = true
        override fun next() = value
    }

    override fun iterator() = iterator

}
