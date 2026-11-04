package it.matato.dietreminder.common.model

/** Groups are scoped to one meal. Alternatives are possibilities, never a saved choice. */
fun requireValidAlternativeGroups(values: List<String?>) {
    require(values.all { it == null || it.isNotBlank() && it.length <= 128 }) { "Invalid alternative group" }
}
