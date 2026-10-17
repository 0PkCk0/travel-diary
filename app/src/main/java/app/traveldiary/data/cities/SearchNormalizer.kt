package app.traveldiary.data.cities

import java.text.Normalizer
import java.util.Locale

/**
 * Brings names and queries to the same lower-case ASCII form, so that "zürich", "Zurich" and
 * "ZURICH" all match, and "Aix-en-Provence" matches "aix en provence".
 */
object SearchNormalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val separators = Regex("[^a-z0-9]+")

    /** Letters that Unicode does not decompose into a base letter plus an accent. */
    private val specialLetters = mapOf(
        'ß' to "ss", 'æ' to "ae", 'œ' to "oe", 'ø' to "o", 'đ' to "d",
        'ð' to "d", 'ł' to "l", 'þ' to "th", 'ı' to "i", 'ħ' to "h", 'ŧ' to "t",
    )

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val replaced = buildString(lower.length) {
            lower.forEach { char -> append(specialLetters[char] ?: char) }
        }
        val withoutAccents = Normalizer.normalize(replaced, Normalizer.Form.NFD).replace(combiningMarks, "")
        return withoutAccents.replace(separators, " ").trim()
    }
}
