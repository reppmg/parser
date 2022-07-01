
import java.util.concurrent.ConcurrentLinkedQueue

data class DataParserImpl(
    override val onSoundDataParsed: (ByteArray) -> Unit,
    override val onAccel1DataParsed: (TimedValue) -> Unit,
    override val onAccel2DataParsed: (TimedValue) -> Unit,
    override val onPpg1DataParsed: (TimedValue) -> Unit,
    override val onPpg2DataParsed: (TimedValue) -> Unit,
    override val onEcg1DataParsed: (TimedValue) -> Unit,
    override val onEcg2DataParsed: (TimedValue) -> Unit
): DataParser {
    companion object {
        const val Microphone: Byte = 0
        const val Accel1: Byte = 1
        const val Accel2: Byte = 2
        const val Ecg1: Byte = 0x0b
        const val Ecg2: Byte = 0x0c
        const val Ppg1: Byte = 0x15
        const val Ppg2: Byte = 0x16

        private const val SIG_LENGTH = 2
        private const val HEADER_LENGTH = 4
        private const val TAIL_LENGTH = 2
        private const val sig = 0xAA55.toShort()
    }

    private var mWriteBuffer = ConcurrentLinkedQueue<Byte>()
    private var signatureRead = false
    private var payloadLengthRead = false
    private var payloadLength = 0xffff
    private var sensorType: Byte = 0x7f.toByte()
    private var startNextChunkOrDataReceived = false
    private var currentSize: Int = 0
    private var currentPos: Int = 0
    private val calc = CRCCalculator()

    @Synchronized
    override fun onNewData(data: ByteArray) {
        mWriteBuffer.addAll(data.asList())
        currentSize += data.size
        startNextChunkOrDataReceived = true
        while (startNextChunkOrDataReceived) {
            parseChunk()
        }
    }

    private fun parseChunk() {
        startNextChunkOrDataReceived = false
        parseSignature()
        parsePayloadLength()
        parseData()
    }

    private fun parseSignature() {
        if (!signatureRead && currentSize >= SIG_LENGTH && mWriteBuffer.readShort(calc) == sig) {
            currentPos = SIG_LENGTH
            signatureRead = true
        }
    }

    private fun parsePayloadLength() {
        if (!payloadLengthRead && currentSize >= HEADER_LENGTH) {
            payloadLength = mWriteBuffer.readShort(calc).toUShort().toInt()
            payloadLengthRead = true
            currentPos = HEADER_LENGTH
        }
    }

    private fun parseData() {
        if (signatureRead && payloadLengthRead && currentSize > HEADER_LENGTH) {
            if (sensorType == 0x7f.toByte()) {
                currentPos = HEADER_LENGTH + 1
                sensorType = mWriteBuffer.poll() ?: 0x7f
                calc.addByteToCRC(sensorType.toUByte())
                if (sensorType == Microphone) {
                    payloadLength = 128
                }
            }
            val chunkSize = HEADER_LENGTH + payloadLength + 1 + TAIL_LENGTH
            val bytesInChunk = currentSize.coerceAtMost(HEADER_LENGTH + payloadLength + 1)
            val numWords: Int = (bytesInChunk - currentPos) / 2
            val numInts: Int = (bytesInChunk - currentPos + (if (bytesInChunk > HEADER_LENGTH + payloadLength) 3 else 0)) / 4
            when (sensorType) {
                Microphone -> sendMicrophoneData(chunkSize, bytesInChunk)
                Accel1 -> sendShorts(numWords, onAccel1DataParsed)
                Accel2 -> sendShorts(numWords, onAccel2DataParsed)
                Ecg1 -> sendIntegers(numInts, onEcg1DataParsed)
                Ecg2 -> sendIntegers(numInts, onEcg2DataParsed)
                Ppg1 -> sendIntegers(numInts, onPpg1DataParsed)
                Ppg2 -> sendIntegers(numInts, onPpg2DataParsed)
            }
            parseTail(chunkSize)
        }
    }

    private fun parseTail(chunkSize: Int) {
        if (currentPos == chunkSize - 2 && currentSize >= chunkSize) {
            val tailWord = mWriteBuffer.readShort(calc,true).toUShort().toInt()
            //if (tailWord == calc.getCRC()) {
                resetAfterBatchRead()
                currentSize -= chunkSize
            //}
        }
    }

    private fun sendMicrophoneData(chunkSize: Int, bytesInChunk: Int) {
        if (bytesInChunk >= chunkSize - TAIL_LENGTH && currentPos == HEADER_LENGTH + 1) {
            mWriteBuffer.sendBytes(payloadLength, calc, onSoundDataParsed)
            currentPos = chunkSize - 2
        }
    }

    private fun sendShorts(numWords: Int, callback: (TimedValue) -> Unit) {
        for (i in 0 until numWords) {
            callback.invoke(TimedValue(mWriteBuffer.readShort(calc).toInt()))
            currentPos += 2
        }
    }

    private fun sendIntegers(numInts: Int, callback: (TimedValue) -> Unit) {
        for (i in 0 until numInts) {
            val nBytes = if (currentPos < payloadLength + HEADER_LENGTH - 3 || payloadLength % 4 == 0) 4 else payloadLength % 4
            callback.invoke(TimedValue(mWriteBuffer.readInt(calc, nBytes)))
            currentPos += nBytes
        }
    }

    private fun resetAfterBatchRead() {
        signatureRead = false
        payloadLengthRead = false
        payloadLength = 0xffff
        sensorType = 0x7f.toByte()
        currentPos = 0
        calc.resetCRC()
        startNextChunkOrDataReceived = mWriteBuffer.size > 0
    }
}