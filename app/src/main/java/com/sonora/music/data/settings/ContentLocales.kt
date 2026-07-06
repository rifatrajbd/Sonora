package com.sonora.music.data.settings

/**
 * Language / country choices for the Content settings (InnerTune-style). Stored value is the
 * ISO code, or [SYSTEM_DEFAULT] to follow the device locale. Old installs that stored the literal
 * "System default" fall back to system too, since it matches no code.
 */
object ContentLocales {
    const val SYSTEM_DEFAULT = "system"

    /** ISO 639-1 code -> display name. */
    val LANGUAGES: List<Pair<String, String>> = listOf(
        "en" to "English", "hi" to "Hindi", "bn" to "Bengali", "pa" to "Punjabi", "ur" to "Urdu",
        "ta" to "Tamil", "te" to "Telugu", "ml" to "Malayalam", "kn" to "Kannada", "mr" to "Marathi",
        "gu" to "Gujarati", "ne" to "Nepali", "si" to "Sinhala", "ar" to "Arabic", "es" to "Spanish",
        "fr" to "French", "de" to "German", "it" to "Italian", "pt" to "Portuguese", "ru" to "Russian",
        "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "id" to "Indonesian", "ms" to "Malay",
        "th" to "Thai", "vi" to "Vietnamese", "tr" to "Turkish", "fa" to "Persian", "nl" to "Dutch",
        "pl" to "Polish", "sv" to "Swedish", "uk" to "Ukrainian", "el" to "Greek", "he" to "Hebrew",
    )

    /** ISO 3166-1 alpha-2 code -> display name. */
    val COUNTRIES: List<Pair<String, String>> = listOf(
        "BD" to "Bangladesh", "IN" to "India", "PK" to "Pakistan", "US" to "United States",
        "GB" to "United Kingdom", "CA" to "Canada", "AU" to "Australia", "AE" to "United Arab Emirates",
        "SA" to "Saudi Arabia", "NP" to "Nepal", "LK" to "Sri Lanka", "MY" to "Malaysia",
        "SG" to "Singapore", "ID" to "Indonesia", "TH" to "Thailand", "VN" to "Vietnam",
        "PH" to "Philippines", "JP" to "Japan", "KR" to "South Korea", "CN" to "China",
        "DE" to "Germany", "FR" to "France", "IT" to "Italy", "ES" to "Spain", "PT" to "Portugal",
        "NL" to "Netherlands", "SE" to "Sweden", "NO" to "Norway", "PL" to "Poland",
        "UA" to "Ukraine", "RU" to "Russia", "TR" to "Turkey", "EG" to "Egypt", "NG" to "Nigeria",
        "ZA" to "South Africa", "BR" to "Brazil", "MX" to "Mexico", "AR" to "Argentina",
    )

    fun languageName(code: String): String? = LANGUAGES.firstOrNull { it.first == code }?.second
    fun countryName(code: String): String? = COUNTRIES.firstOrNull { it.first == code }?.second

    /** Effective language code: the chosen one, else the device language. */
    fun effectiveLanguage(setting: String): String =
        if (LANGUAGES.any { it.first == setting }) setting
        else java.util.Locale.getDefault().language.ifBlank { "en" }

    /** Effective country code: the chosen one, else the device country. */
    fun effectiveCountry(setting: String): String =
        if (COUNTRIES.any { it.first == setting }) setting
        else java.util.Locale.getDefault().country.ifBlank { "US" }
}
