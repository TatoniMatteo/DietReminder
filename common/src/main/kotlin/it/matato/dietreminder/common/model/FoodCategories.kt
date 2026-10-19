package it.matato.dietreminder.common.model

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class FoodCategory(val name: String, val keywords: List<String>)

/** One rule catalogue for Android and the web; manual categories always take priority. */
object FoodCategories {
    val all = listOf(
        FoodCategory("Frutta e verdura", listOf("mela", "mele", "pera", "pere", "banana", "banane", "arancia", "arance", "limone", "limoni", "fragole", "kiwi", "uva", "pesca", "pesche", "pomodoro", "pomodori", "insalata", "lattuga", "zucchine", "zucchina", "carote", "carota", "patate", "patata", "cipolla", "cipolle", "aglio", "broccoli", "spinaci", "melanzane", "peperoni", "verdure", "frutta", "avocado", "funghi", "sedano", "finocchi", "cavolfiore", "cetrioli")),
        FoodCategory("Carne e salumi", listOf("pollo", "tacchino", "manzo", "vitello", "maiale", "carne", "bresaola", "prosciutto", "salame", "salsiccia", "hamburger")),
        FoodCategory("Pesce", listOf("pesce", "salmone", "tonno", "merluzzo", "orata", "branzino", "gamberi", "sgombro", "sardine", "vongole", "cozze", "calamari")),
        FoodCategory("Latte, uova e formaggi", listOf("latte", "uova", "uovo", "yogurt", "formaggio", "mozzarella", "ricotta", "parmigiano", "grana", "burro", "panna", "kefir", "pecorino", "feta")),
        FoodCategory("Pane e forno", listOf("pane", "panino", "panini", "piadina", "piadine", "focaccia", "grissini", "cracker", "crackers", "fette biscottate", "brioche")),
        FoodCategory("Pasta, cereali e legumi", listOf("pasta", "riso", "spaghetti", "penne", "fusilli", "farro", "orzo", "avena", "quinoa", "cous cous", "couscous", "lenticchie", "ceci", "fagioli", "piselli", "cereali", "farina")),
        FoodCategory("Surgelati", listOf("surgelato", "surgelati", "surgelate", "surgelata", "gelato", "gelati")),
        FoodCategory("Bevande", listOf("acqua", "succo", "birra", "vino", "bevanda", "latte di soia", "latte di avena", "latte di mandorla", "caffe", "te")),
        FoodCategory("Dispensa", listOf("olio", "sale", "zucchero", "pepe", "aceto", "miele", "marmellata", "passata", "pelati", "sugo", "pesto", "biscotti", "cioccolato", "mandorle", "noci", "nocciole", "semi", "spezie")),
        FoodCategory("Altro", emptyList()),
    )
    fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
    fun classify(name: String, manual: String? = null): String {
        manual?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val normalized = normalize(name)
        // Prefer a specific phrase, then the first matching ingredient in a description.
        if (Regex("(^|[^a-z0-9])surgelat[a-z]*(?:$|[^a-z0-9])").containsMatchIn(normalized)) return "Surgelati"
        val matches = all.flatMap { category -> category.keywords.mapNotNull { keyword ->
            Regex("(^|[^a-z0-9])${Regex.escape(keyword)}($|[^a-z0-9])").find(normalized)?.let { Triple(category.name, keyword.length + if (' ' in keyword) 1000 else 0, it.range.first) }
        } }
        return matches.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second >= 1000 }.thenBy { it.third }.thenByDescending { it.second }).firstOrNull()?.first ?: "Altro"
    }
}
