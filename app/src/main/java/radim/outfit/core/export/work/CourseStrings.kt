package radim.outfit.core.export.work

fun replaceNonAllowedChars(input: String, replacementChar: Char): String {
    val nonAllowed = """[^\p{ASCII}]""".toRegex()
    return nonAllowed.replace(input, replacementChar.toString())
}