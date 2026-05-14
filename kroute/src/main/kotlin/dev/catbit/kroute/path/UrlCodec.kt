package dev.catbit.kroute.path

internal object UrlCodec {
    private const val HEX_CHARS = "0123456789ABCDEF"

    fun encode(text: String): String = buildString(text.length * 2) {
        text.encodeToByteArray().forEach { b ->
            val i = b.toInt() and 0xFF
            val c = i.toChar()
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "-_.~") {
                append(c)
            } else {
                append('%')
                append(HEX_CHARS[i shr 4])
                append(HEX_CHARS[i and 0x0F])
            }
        }
    }

    fun decode(text: String, plusAsSpace: Boolean = false): String {
        val bytes = ByteArray(text.length)
        var byteCount = 0
        var i = 0

        while (i < text.length) {
            val c = text[i]
            if (c == '%' && i + 2 < text.length) {
                val d1 = text[i + 1].digitToIntOrNull(16) ?: -1
                val d2 = text[i + 2].digitToIntOrNull(16) ?: -1
                if (d1 != -1 && d2 != -1) {
                    bytes[byteCount++] = ((d1 shl 4) or d2).toByte()
                    i += 3
                    continue
                }
            }

            val byteVal = if (plusAsSpace && c == '+') {
                ' '.code.toByte()
            } else {
                c.code.toByte()
            }

            bytes[byteCount++] = byteVal
            i++
        }

        return bytes.decodeToString(endIndex = byteCount)
    }
}