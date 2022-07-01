package studio.bakery.arduinocontroller

import DataParserImpl
import TimedValue
import org.junit.jupiter.api.Test


class RealDataV01Tests {
    val data = this.javaClass.classLoader?.getResource("log01.log")?.openStream()?.readBytes()!!

    private val microphoneData = arrayListOf<Byte>();
    private val accel1Data = arrayListOf<TimedValue>()
    private val accel2Data = arrayListOf<TimedValue>()
    private val ppg1Data = arrayListOf<TimedValue>()
    private val ppg2Data = arrayListOf<TimedValue>()
    private val ecg1Data = arrayListOf<TimedValue>()
    private val ecg2Data = arrayListOf<TimedValue>()

    private val dataParserImpl = DataParserImpl(
        { microphoneData.addAll(it.asList()) },
        { accel1Data.add(it) },
        { accel2Data.add(it) },
        { ppg1Data.add(it) },
        { ppg2Data.add(it) },
        { ecg1Data.add(it) },
        { ecg2Data.add(it) }
    )

    @Test
    fun feedAllLog() {
        dataParserImpl.onNewData(data)
        verifyDataParseCorrect()
    }

    private fun verifyDataParseCorrect() {
        val takeLast = microphoneData.takeLast(8)
        println(takeLast)
        assert(takeLast[0] == 7.toByte() && takeLast[1] == 29.toByte())
    }

    @Test
    fun feedChunks() {
        val chunkSize = 50
        data.asList().chunked(chunkSize).forEach {
            dataParserImpl.onNewData(it.toByteArray())
        }
        verifyDataParseCorrect()
    }

}