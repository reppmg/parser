
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.experimental.or

class CRCCalculator {
    private var crc: Int = 0xffff
    private val polynomial = 0x1021

    fun getCRC(): Int {
        return crc
    }

    fun resetCRC() {
        crc = 0xffff
    }

    fun addByteToCRC(b: UByte) {
        for (i in 0..7) {
            val bit = (b.toInt() shr (7 - i)) and 1 == 1
            val c15 = (crc shr 15) and 1 == 1
            crc = crc shl 1
            if (c15 xor bit) crc = crc xor polynomial
        }
        crc = crc and 0xffff
    }
}

fun ConcurrentLinkedQueue<Byte>.readShort(calc: CRCCalculator, isCRC: Boolean = false): Short {
    val byte1 = (this.poll() ?: 0).toUByte()
    if (!isCRC) {
        calc.addByteToCRC(byte1)
    }
    val byte2 = (this.poll() ?: 0).toUByte()
    if (!isCRC) {
        calc.addByteToCRC(byte2)
    }
    return if (isCRC) {
        (byte1 * 256u).toShort() or byte2.toShort()
    } else {
        (byte2 * 256u).toShort() or byte1.toShort()
    }
}

fun ConcurrentLinkedQueue<Byte>.readInt(calc: CRCCalculator, numBytes: Int): Int {
    var value = 0
    var shift = 0
    for (i in 0 until numBytes) {
        val byte1 = (this.poll() ?: 0).toUByte()
        calc.addByteToCRC(byte1)
        value = value or (byte1.toInt() shl shift)
        shift += 8
    }
    return value
}

fun ConcurrentLinkedQueue<Byte>.sendBytes(payloadLength: Int, calc: CRCCalculator, callback: (ByteArray) -> Unit) {
    val soundData = ByteArray(payloadLength)
    for (i in 0 until payloadLength) {
        soundData[i] = this.poll() ?: 0
        calc.addByteToCRC(soundData[i].toUByte())
    }
    callback.invoke(soundData)
}