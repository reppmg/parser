
import TimedValue

interface DataParser {
    val onSoundDataParsed: (ByteArray) -> Unit
    val onAccel1DataParsed: (TimedValue) -> Unit
    val onAccel2DataParsed: (TimedValue) -> Unit
    val onPpg1DataParsed: (TimedValue) -> Unit
    val onPpg2DataParsed: (TimedValue) -> Unit
    val onEcg1DataParsed: (TimedValue) -> Unit
    val onEcg2DataParsed: (TimedValue) -> Unit

    fun onNewData(data: ByteArray)
}