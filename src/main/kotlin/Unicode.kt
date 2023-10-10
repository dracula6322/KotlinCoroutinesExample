fun main() {

    // val wrongSymbol: Char = '😉' // To many characters

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

        with(String(this)){
            check(length == 2)
            println()

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

}