import java.io.ByteArrayInputStream

@OptIn(ExperimentalStdlibApi::class)
fun main() {

    // https://apps.timwhitlock.info/unicode/inspect?s=%F0%9F%A4%A6%F0%9F%8F%BF%E2%80%8D%E2%99%82%EF%B8%8F
    // https://www.youtube.com/watch?v=SZFe3m1DV1A
    // https://py2jdbc.readthedocs.io/en/latest/mutf8.html
    // https://source.android.com/docs/core/runtime/dex-format#mutf-8
    // https://tonsky.me/blog/unicode/

    // val wrongSymbol: Char = '😉' // To many characters

    with("🤦🏿‍♂️") {
        check(length == 7)

        check(String(toByteArray(Charsets.UTF_8).take(4).toByteArray()) == "🤦")
        check(String(toByteArray(Charsets.UTF_8).take(8).toByteArray()) == "🤦🏿")
        check(String(toByteArray(Charsets.UTF_8).take(14).toByteArray()) == "🤦🏿‍♂")
        check(
            toByteArray(Charsets.UTF_8).drop(0).take(4) == listOf(
                0xf0.toByte(),
                0x9f.toByte(),
                0xa4.toByte(),
                0xa6.toByte()
            )
        )
        check(
            toByteArray(Charsets.UTF_8).drop(4).take(4) == listOf(
                0xf0.toByte(),
                0x9f.toByte(),
                0x8f.toByte(),
                0xbf.toByte()
            )
        )
        check(
            toByteArray(Charsets.UTF_8).drop(8).take(3) == listOf(
                0xe2.toByte(),
                0x80.toByte(),
                0x8d.toByte(),
            )
        )
        check(
            toByteArray(Charsets.UTF_8).drop(11).take(3) == listOf(
                0xe2.toByte(),
                0x99.toByte(),
                0x82.toByte(),
            )
        )
        check(
            toByteArray(Charsets.UTF_8).drop(14).take(3) == listOf(
                0xef.toByte(),
                0xb8.toByte(),
                0x8f.toByte(),
            )
        )
    }

    with(Character.toChars(665)) {// ʙ
        // https://apps.timwhitlock.info/unicode/inspect/hex/0299
        check(size == 1)
        // UTF-16
        check(this[0].code == 0x0299)
    }

    with(Character.toChars(128517)) {// 😅
        // https://apps.timwhitlock.info/unicode/inspect/hex/1F605
        check(size == 2)
        // UTF-16
        check(this[0].code == 0xd83d)
        check(this[1].code == 0xde05)

        with(String(this)) {
            check(length == 2)

            check(toByteArray(Charsets.UTF_8)[0].toUByte() == 0xf0.toUByte())
            check(toByteArray(Charsets.UTF_8)[1].toUByte() == 0x9f.toUByte())
            check(toByteArray(Charsets.UTF_8)[2].toUByte() == 0x98.toUByte())
            check(toByteArray(Charsets.UTF_8)[3].toUByte() == 0x85.toUByte())

            // Первые два байта это BOM (Byte Order Mark, BOM)
            check(toByteArray(Charsets.UTF_16)[0].toUByte() == 0xfe.toUByte())
            check(toByteArray(Charsets.UTF_16)[1].toUByte() == 0xff.toUByte())
            check(toByteArray(Charsets.UTF_16)[2].toUByte() == 0xd8.toUByte())
            check(toByteArray(Charsets.UTF_16)[3].toUByte() == 0x3d.toUByte())
            check(toByteArray(Charsets.UTF_16)[4].toUByte() == 0xde.toUByte())
            check(toByteArray(Charsets.UTF_16)[5].toUByte() == 0x05.toUByte())

            check(toByteArray(Charsets.UTF_16BE)[0].toUByte() == 0xd8.toUByte())
            check(toByteArray(Charsets.UTF_16BE)[1].toUByte() == 0x3d.toUByte())
            check(toByteArray(Charsets.UTF_16BE)[2].toUByte() == 0xde.toUByte())
            check(toByteArray(Charsets.UTF_16BE)[3].toUByte() == 0x05.toUByte())
        }
    }

    // char работает со старой версией Unicode, которая fixed-width 16-bit entities
    // Когда считали что 65к символов хватит
    // Значения от U+0000 до U+FFFF - Basic Multilingual Plane (BMP)
    // Значения от U+FFFF - supplementary characters.
    // Значения от U+0000 до U+10FFFF - Unicode scalar value.
    with(75000.toChar()) { // ⓸
        check(code == 9464) // 9464 = 75000 - 65536
    }

    // Mutf8
    with(0.toChar().toString()) {
        val bytes = Mutf8.encode(this)
        check(bytes.size == 2)
        check(bytes[0].toUByte() == 0xc0.toUByte())
        check(bytes[1].toUByte() == 0x80.toUByte())
        check(!this.toByteArray(Charsets.UTF_8).contentEquals(bytes))
        val byteArrayInputStream = ByteArrayInputStream(bytes + 0x00)
        check(Mutf8.decode(byteArrayInputStream, CharArray(byteArrayInputStream.available())) == this)
    }
    with("a") {
        val bytes = Mutf8.encode(this)
        check(bytes.size == 1)
        check(bytes[0].toUByte() == 0x61.toUByte())
        check(this.toByteArray(Charsets.UTF_8).contentEquals(bytes))
        val byteArrayInputStream = ByteArrayInputStream(bytes + 0x00)
        check(Mutf8.decode(byteArrayInputStream, CharArray(byteArrayInputStream.available())) == this)
    }
    with("Ĭ") {
        val bytes = Mutf8.encode(this)
        check(bytes.size == 2)
        check(bytes[0].toUByte() == 0xc4.toUByte())
        check(bytes[1].toUByte() == 0xac.toUByte())
        check(this.toByteArray(Charsets.UTF_8).contentEquals(bytes))
        val byteArrayInputStream = ByteArrayInputStream(bytes + 0x00)
        check(Mutf8.decode(byteArrayInputStream, CharArray(byteArrayInputStream.available())) == this)
    }
    with("€") {
        val bytes = Mutf8.encode(this)
        check(bytes.size == 3)
        check(bytes[0].toUByte() == 0xe2.toUByte())
        check(bytes[1].toUByte() == 0x82.toUByte())
        check(bytes[2].toUByte() == 0xac.toUByte())
        check(this.toByteArray(Charsets.UTF_8).contentEquals(bytes))
        val byteArrayInputStream = ByteArrayInputStream(bytes + 0x00)
        check(Mutf8.decode(byteArrayInputStream, CharArray(byteArrayInputStream.available())) == this)
    }
    with("𐍈") {
        val bytes = Mutf8.encode(this)
        check(bytes.size == 6)
        check(bytes[0].toUByte() == 0xed.toUByte())
        check(bytes[1].toUByte() == 0xa0.toUByte())
        check(bytes[2].toUByte() == 0x80.toUByte())
        check(bytes[3].toUByte() == 0xed.toUByte())
        check(bytes[4].toUByte() == 0xbd.toUByte())
        check(bytes[5].toUByte() == 0x88.toUByte())
        check(!this.toByteArray(Charsets.UTF_8).contentEquals(bytes))
        val byteArrayInputStream = ByteArrayInputStream(bytes + 0x00)
        check(Mutf8.decode(byteArrayInputStream, CharArray(byteArrayInputStream.available())) == this)
    }
}