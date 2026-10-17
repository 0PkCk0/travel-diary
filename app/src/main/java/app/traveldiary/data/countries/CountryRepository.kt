package app.traveldiary.data.countries

import android.content.Context
import java.text.Collator
import java.util.Locale

/** The fixed list of countries and territories shipped in assets/data/countries.tsv. */
class CountryRepository(private val context: Context) {

    val all: List<Country> by lazy {
        val collator = Collator.getInstance(Locale.ENGLISH)
        context.assets.open("data/countries.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull(Country::fromTsv).sortedWith(compareBy(collator) { it.name }).toList()
        }
    }

    private val byCode: Map<String, Country> by lazy { all.associateBy { it.code } }

    operator fun get(code: String): Country? = byCode[code]

    fun name(code: String): String = byCode[code]?.name ?: code
}
