package com.example.data.model

data class Language(
  val code: String,
  val name: String,
  val nativeName: String,
  val flagEmoji: String,
  val localeCode: String,
  val isRtl: Boolean = false,
  val speechRecognitionSupported: Boolean = true,
  val ttsSupported: Boolean = true
) {
  val writingSystem: String
    get() = when (code.lowercase().substringBefore("-")) {
      "ar", "fa", "ps", "sd" -> "Arabic Script (RTL)"
      "ur" -> "Urdu Nastaliq / Arabic Script (RTL)"
      "he", "yi" -> "Hebrew Script (RTL)"
      "zh" -> "Simplified Hanzi + Pinyin"
      "ja" -> "Hiragana, Katakana & Kanji"
      "ko" -> "Hangul"
      "hi", "mr", "ne" -> "Devanagari"
      "bn" -> "Bengali Script"
      "ta" -> "Tamil Script"
      "te" -> "Telugu Script"
      "gu" -> "Gujarati Script"
      "kn" -> "Kannada Script"
      "ml" -> "Malayalam Script"
      "pa" -> "Gurmukhi Script"
      "si" -> "Sinhala Script"
      "th" -> "Thai Script"
      "lo" -> "Lao Script"
      "km" -> "Khmer Script"
      "my" -> "Burmese Script"
      "am" -> "Ge'ez (Ethiopic) Script"
      "el" -> "Greek Alphabet"
      "ka" -> "Georgian Mkhedruli"
      "hy" -> "Armenian Alphabet"
      "ru", "uk", "bg", "sr", "mk", "kk", "ky", "tg", "mn" -> "Cyrillic Alphabet"
      else -> "Latin Alphabet"
    }

  val sampleGreeting: String
    get() = when (code.lowercase().substringBefore("-")) {
      "en" -> "Hello! How are you doing today?"
      "es" -> "¡Hola! ¿Cómo estás hoy?"
      "fr" -> "Bonjour ! Comment allez-vous aujourd'hui ?"
      "de" -> "Guten Tag! Wie geht es Ihnen heute?"
      "ar" -> "مرحباً! كيف حالك اليوم؟"
      "ur" -> "السلام علیکم! آپ آج کیسے ہیں؟"
      "hi" -> "नमस्ते! आज आप कैसे हैं?"
      "zh" -> "你好！你今天过得怎么样？"
      "ja" -> "こんにちは！今日の調子はいかがですか？"
      "ko" -> "안녕하세요! 오늘 하루 어떠세요?"
      "pt" -> "Olá! Tudo bem com você hoje?"
      "it" -> "Ciao! Come stai oggi?"
      "tr" -> "Merhaba! Bugün nasılsınız?"
      "ru" -> "Здравствуйте! Как ваши дела сегодня?"
      "nl" -> "Hallo! Hoe gaat het vandaag met je?"
      "fa" -> "سلام! حال شما امروز چطور است؟"
      "ps" -> "سلام! ستاسو حال نن څنګه دی؟"
      "bn" -> "নমস্কার! আপনি আজ কেমন আছেন?"
      "id" -> "Halo! Apa kabar hari ini?"
      "ms" -> "Helo! Apa khabar hari ini?"
      "vi" -> "Xin chào! Hôm nay bạn thế nào?"
      "th" -> "สวัสดีครับ/ค่ะ! วันนี้สบายดีไหม?"
      "pl" -> "Cześć! Jak się dzisiaj masz?"
      "uk" -> "Привіт! Як твої справи сьогодні?"
      "el" -> "Γεια σας! Πώς είστε σήμερα;"
      "he" -> "שלום! מה שלומך היום?"
      "sw" -> "Habari! Hali yakoกวี leo?"
      else -> "$nativeName • $name"
    }

  companion object {
    val SUPPORTED_LANGUAGES = listOf(
      Language("en", "English", "English", "🇺🇸", "en-US", false),
      Language("es", "Spanish", "Español", "🇪🇸", "es-ES", false),
      Language("fr", "French", "Français", "🇫🇷", "fr-FR", false),
      Language("de", "German", "Deutsch", "🇩🇪", "de-DE", false),
      Language("ar", "Arabic", "العربية", "🇸🇦", "ar-SA", true),
      Language("zh", "Chinese (Mandarin)", "中文", "🇨🇳", "zh-CN", false),
      Language("ja", "Japanese", "日本語", "🇯🇵", "ja-JP", false),
      Language("ko", "Korean", "한국어", "🇰🇷", "ko-KR", false),
      Language("hi", "Hindi", "हिन्दी", "🇮🇳", "hi-IN", false),
      Language("ur", "Urdu", "اردو", "🇵🇰", "ur-PK", true),
      Language("pt", "Portuguese", "Português", "🇧🇷", "pt-BR", false),
      Language("it", "Italian", "Italiano", "🇮🇹", "it-IT", false),
      Language("tr", "Turkish", "Türkçe", "🇹🇷", "tr-TR", false),
      Language("ru", "Russian", "Русский", "🇷🇺", "ru-RU", false),
      Language("ps", "Pashto", "پښتو", "🇦🇫", "ps-AF", true),
      Language("fa", "Persian", "فارسی", "🇮🇷", "fa-IR", true),
      Language("bn", "Bengali", "বাংলা", "🇧🇩", "bn-BD", false),
      Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", "id-ID", false),
      Language("ms", "Malay", "Bahasa Melayu", "🇲🇾", "ms-MY", false),
      Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", "vi-VN", false),
      Language("th", "Thai", "ไทย", "🇹🇭", "th-TH", false),
      Language("nl", "Dutch", "Nederlands", "🇳🇱", "nl-NL", false),
      Language("pl", "Polish", "Polski", "🇵🇱", "pl-PL", false),
      Language("uk", "Ukrainian", "Українська", "🇺🇦", "uk-UA", false),
      Language("ro", "Romanian", "Română", "🇷🇴", "ro-RO", false),
      Language("el", "Greek", "Ελληνικά", "🇬🇷", "el-GR", false),
      Language("hu", "Hungarian", "Magyar", "🇭🇺", "hu-HU", false),
      Language("cs", "Czech", "Čeština", "🇨🇿", "cs-CZ", false),
      Language("sv", "Swedish", "Svenska", "🇸🇪", "sv-SE", false),
      Language("da", "Danish", "Dansk", "🇩🇰", "da-DK", false),
      Language("fi", "Finnish", "Suomi", "🇫🇮", "fi-FI", false),
      Language("no", "Norwegian", "Norsk", "🇳🇴", "no-NO", false),
      Language("he", "Hebrew", "עברית", "🇮🇱", "he-IL", true),
      Language("fil", "Filipino", "Filipino", "🇵🇭", "fil-PH", false),
      Language("sw", "Swahili", "Kiswahili", "🇰🇪", "sw-KE", false),
      Language("am", "Amharic", "አማርኛ", "🇪🇹", "am-ET", false),
      Language("yo", "Yoruba", "Yorùbá", "🇳🇬", "yo-NG", false),
      Language("ig", "Igbo", "Igbo", "🇳🇬", "ig-NG", false),
      Language("ha", "Hausa", "Hausa", "🇳🇬", "ha-NG", false),
      Language("zu", "Zulu", "isiZulu", "🇿🇦", "zu-ZA", false),
      Language("af", "Afrikaans", "Afrikaans", "🇿🇦", "af-ZA", false),
      Language("ta", "Tamil", "தமிழ்", "🇮🇳", "ta-IN", false),
      Language("te", "Telugu", "తెలుగు", "🇮🇳", "te-IN", false),
      Language("mr", "Marathi", "मराठी", "🇮🇳", "mr-IN", false),
      Language("gu", "Gujarati", "ગુજરાતી", "🇮🇳", "gu-IN", false),
      Language("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳", "kn-IN", false),
      Language("ml", "Malayalam", "മലയാളം", "🇮🇳", "ml-IN", false),
      Language("pa", "Punjabi", "ਪੰਜਾਬી", "🇮🇳", "pa-IN", false),
      Language("si", "Sinhala", "සිංහල", "🇱🇰", "si-LK", false),
      Language("my", "Burmese", "မြန်မာစာ", "🇲🇲", "my-MM", false),
      Language("km", "Khmer", "ខ្មែរ", "🇰🇭", "km-KH", false),
      Language("lo", "Lao", "ລາວ", "🇱🇦", "lo-LA", false),
      Language("ne", "Nepali", "नेपाली", "🇳🇵", "ne-NP", false),
      Language("ka", "Georgian", "ქართული", "🇬🇪", "ka-GE", false),
      Language("hy", "Armenian", "Հայերեն", "🇦🇲", "hy-AM", false),
      Language("az", "Azerbaijani", "Azərbaycan", "🇦🇿", "az-AZ", false),
      Language("kk", "Kazakh", "Қазақ тілі", "🇰🇿", "kk-KZ", false),
      Language("uz", "Uzbek", "Oʻzbekcha", "🇺🇿", "uz-UZ", false),
      Language("ky", "Kyrgyz", "Кыргызча", "🇰🇬", "ky-KG", false),
      Language("tg", "Tajik", "Тоҷикӣ", "🇹🇯", "tg-TJ", false),
      Language("mn", "Mongolian", "Монгол", "🇲🇳", "mn-MN", false),
      Language("sq", "Albanian", "Shqip", "🇦🇱", "sq-AL", false),
      Language("bs", "Bosnian", "Bosanski", "🇧🇦", "bs-BA", false),
      Language("hr", "Croatian", "Hrvatski", "🇭🇷", "hr-HR", false),
      Language("sr", "Serbian", "Српски", "🇷🇸", "sr-RS", false),
      Language("sk", "Slovak", "Slovenčina", "🇸🇰", "sk-SK", false),
      Language("sl", "Slovenian", "Slovenščina", "🇸🇮", "sl-SI", false),
      Language("bg", "Bulgarian", "Български", "🇧🇬", "bg-BG", false),
      Language("mk", "Macedonian", "Македонски", "🇲🇰", "mk-MK", false),
      Language("et", "Estonian", "Eesti", "🇪🇪", "et-EE", false),
      Language("lv", "Latvian", "Latviešu", "🇱🇻", "lv-LV", false),
      Language("lt", "Lithuanian", "Lietuvių", "🇱🇹", "lt-LT", false),
      Language("is", "Icelandic", "Íslenska", "🇮🇸", "is-IS", false),
      Language("ga", "Irish", "Gaeilge", "🇮🇪", "ga-IE", false),
      Language("cy", "Welsh", "Cymraeg", "🏴󠁧󠁢󠁷󠁬󠁳󠁿", "cy-GB", false),
      Language("eu", "Basque", "Euskara", "🇪🇸", "eu-ES", false),
      Language("gl", "Galician", "Galego", "🇪🇸", "gl-ES", false),
      Language("ca", "Catalan", "Català", "🇪🇸", "ca-ES", false),
      Language("eo", "Esperanto", "Esperanto", "🌍", "eo", false),
      Language("la", "Latin", "Latina", "🇻🇦", "la", false),
      Language("yi", "Yiddish", "ייִדיש", "🇮🇱", "yi", true),
      Language("ku", "Kurdish", "Kurdî", "🇮🇶", "ku-TR", false),
      Language("sd", "Sindhi", "سنڌي", "🇵🇰", "sd-PK", true),
      Language("so", "Somali", "Soomaaliga", "🇸🇴", "so-SO", false),
      Language("mg", "Malagasy", "Malagasy", "🇲🇬", "mg-MG", false),
      Language("ny", "Chichewa", "Chichewa", "🇲🇼", "ny-MW", false),
      Language("sn", "Shona", "chiShona", "🇿🇼", "sn-ZW", false),
      Language("st", "Sesotho", "Sesotho", "🇱🇸", "st-LS", false),
      Language("ht", "Haitian Creole", "Kreyòl ayisyen", "🇭🇹", "ht-HT", false),
      Language("sm", "Samoan", "Gagana Samoa", "🇼🇸", "sm-WS", false),
      Language("mi", "Maori", "Te Reo Māori", "🇳🇿", "mi-NZ", false),
      Language("haw", "Hawaiian", "ʻŌlelo Hawaiʻi", "🇺🇸", "haw-US", false)
    )

    fun findByCode(code: String): Language {
      val clean = code.trim().lowercase().substringBefore("-")
      return SUPPORTED_LANGUAGES.firstOrNull {
        it.code.equals(clean, ignoreCase = true) || it.localeCode.equals(code.trim(), ignoreCase = true)
      } ?: SUPPORTED_LANGUAGES.first()
    }

    fun isRtlLanguage(code: String): Boolean {
      return findByCode(code).isRtl
    }
  }
}
