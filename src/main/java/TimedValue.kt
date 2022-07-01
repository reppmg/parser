import java.util.*

interface Timestamped {
    val timestamp: Long
}

data class TimedValue(
    override val timestamp: Long,
    val value: Int
) : Timestamped {
    constructor(value: Int) : this(Date().time, value) {
//        Timber.d("Create timed $timestamp: $value")
    }
}
