package studio.bakery.arduinocontroller

import DataParserImpl
import TimedValue
import org.junit.jupiter.api.Test


class DataParserTests {
    private val microphoneData = ByteArray(5);
    private val accel1Data = arrayListOf<TimedValue>()
    private val accel2Data = arrayListOf<TimedValue>()
    private val ppg1Data = arrayListOf<TimedValue>()
    private val ppg2Data = arrayListOf<TimedValue>()
    private val ecg1Data = arrayListOf<TimedValue>()
    private val ecg2Data = arrayListOf<TimedValue>()

    private val dataParserImpl = DataParserImpl(
        {it.copyInto(microphoneData)},
        {accel1Data.add(it)},
        {accel2Data.add(it)},
        {ppg1Data.add(it)},
        {ppg2Data.add(it)},
        {ecg1Data.add(it)},
        {ecg2Data.add(it)}
    )

    private val data1 = byteArrayOf(85, -86, 0, 5, 1, 23, 112, 1)
    private val data2 = byteArrayOf(44, -1, -1, 85, -86, 0, 7, 2, 39, 16, 1, -12, 58, -104, -1, -1, 85, -86, 0)
    private val data3 = byteArrayOf(3, 1, 19, -120, -1)
    private val data4 = byteArrayOf(-1, 85, -86, 0, 5, 0, 44, 33, 22, 11, -1, -1, 85)
    private val data5 = byteArrayOf(-86, 0, 3, 3, 23, 102, -1)
    private val data6 = byteArrayOf(-1)

    @Test
    fun testAccel1SimpleData() {
        dataParserImpl.onNewData(data1)
        dataParserImpl.onNewData(data2)
        dataParserImpl.onNewData(data3)
        dataParserImpl.onNewData(data4)
        dataParserImpl.onNewData(data5)
        dataParserImpl.onNewData(data6)
        assert(accel1Data[0].value == 6000 && accel1Data[1].value == 300 && ecg1Data[0].value == 10000
                && ecg1Data[1].value == 500 && ecg2Data[2].value == 15000 && accel1Data[2].value == 5000
                && microphoneData[0].toInt() == 44 && ppg1Data[0].value == 5990)
    }
}