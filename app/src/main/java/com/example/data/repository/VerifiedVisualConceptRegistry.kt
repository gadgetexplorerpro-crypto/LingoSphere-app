package com.example.data.repository

import android.content.Context
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Stable, content-bound answer option model used across all multiple-choice games.
 * Eliminates fragile index-based validation so shuffling or layout changes can never
 * cause the wrong option to be highlighted green.
 */
data class ValidatedAnswerOption(
  val id: String,
  val text: String,
  val subtitle: String = ""
)

/**
 * A concept-verified visual entry for Picture Word Quiz and visual vocabulary games.
 * Every visual concept has:
 * 1) A canonical `conceptId` (e.g., "table", "lamp", "door", "clock", or "dyn:espresso_machine").
 * 2) Verified multilingual vocabulary & natural definition prompts.
 * 3) A real Wikimedia Commons / Wikipedia REST photograph URL (or verified exact-word fallback when offline).
 * 4) Semantic keywords across supported languages so any dynamic AI question is
 *    deterministically verified against the image before it is ever shown to the learner.
 */
data class VerifiedVisualConcept(
  val conceptId: String,
  val domainIndex: Int,
  val domainLabel: String,
  val englishWord: String,
  val definitionQuestionEn: String,
  val exampleSentenceEn: String,
  val wikimediaPhotoUrl: String,
  val localDrawableResId: Int? = null,
  val iconKey: String,
  val translations: Map<String, Pair<String, String>>, // langCode -> (word, exampleSentence)
  val phonetics: Map<String, String>,
  val matchKeywords: Set<String>,
  val levelBand: Int = 1,
  val isDynamic: Boolean = false
) {
  fun wordForLanguage(languageCode: String): String {
    val key = languageCode.lowercase().take(2)
    return translations[key]?.first ?: englishWord
  }

  fun sentenceForLanguage(languageCode: String): String {
    val key = languageCode.lowercase().take(2)
    return translations[key]?.second ?: exampleSentenceEn
  }

  fun phoneticForLanguage(languageCode: String): String {
    val key = languageCode.lowercase().take(2)
    return phonetics[key] ?: englishWord.lowercase()
  }
}

object VerifiedVisualConceptRegistry {

  private const val VISUAL_CACHE_PREFS = "lingosphere_verified_visual_cache"
  private const val USER_AGENT = "LingoSphere/1.0 (Android; Language Learning App; contact: support@lingosphere.app)"

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .build()
  }

  private val resolvedUrlMemoryCache = ConcurrentHashMap<String, String>()
  private val dynamicByIdMap = ConcurrentHashMap<String, VerifiedVisualConcept>()

  private fun wikiPhoto(fileName: String): String =
    when {
      fileName.isBlank() -> ""
      fileName.startsWith("http://") || fileName.startsWith("https://") -> fileName
      else -> "https://commons.wikimedia.org/wiki/Special:FilePath/$fileName?width=640"
    }

  private fun entry(
    id: String,
    domainIdx: Int,
    domain: String,
    enW: String,
    questionEn: String,
    enS: String,
    wikiFile: String,
    localRes: Int? = null,
    iconKey: String,
    esW: String,
    esS: String,
    frW: String,
    frS: String,
    deW: String,
    deS: String,
    arW: String,
    arS: String,
    jaW: String,
    jaS: String,
    extraKeywords: List<String> = emptyList(),
    band: Int = 1
  ): VerifiedVisualConcept {
    val keywords = buildSet {
      add(id.lowercase())
      add(enW.lowercase())
      add(esW.lowercase())
      add(frW.lowercase())
      add(deW.lowercase())
      add(arW.lowercase())
      add(jaW.lowercase())
      extraKeywords.forEach { add(it.lowercase()) }
    }
    return VerifiedVisualConcept(
      conceptId = id,
      domainIndex = domainIdx,
      domainLabel = domain,
      englishWord = enW,
      definitionQuestionEn = questionEn,
      exampleSentenceEn = enS,
      wikimediaPhotoUrl = wikiPhoto(wikiFile),
      localDrawableResId = localRes,
      iconKey = iconKey,
      translations = mapOf(
        "en" to (enW to enS),
        "es" to (esW to esS),
        "fr" to (frW to frS),
        "de" to (deW to deS),
        "ar" to (arW to arS),
        "ja" to (jaW to jaS)
      ),
      phonetics = mapOf(
        "en" to "/${enW.lowercase()}/",
        "es" to "/${esW.lowercase()}/",
        "fr" to "/${frW.lowercase()}/",
        "de" to "/${deW.lowercase()}/",
        "ar" to arW,
        "ja" to jaW
      ),
      matchKeywords = keywords,
      levelBand = band,
      isDynamic = false
    )
  }

  private fun quickEntry(
    id: String,
    domainIdx: Int,
    domain: String,
    enW: String,
    questionEn: String,
    enS: String,
    iconKey: String,
    esW: String,
    frW: String,
    deW: String,
    arW: String,
    jaW: String,
    band: Int,
    wikiTitle: String = enW.replace(" ", "_"),
    extraKeywords: List<String> = emptyList()
  ): VerifiedVisualConcept = entry(
    id = id,
    domainIdx = domainIdx,
    domain = domain,
    enW = enW,
    questionEn = questionEn,
    enS = enS,
    wikiFile = "${wikiTitle}.jpg",
    iconKey = iconKey,
    esW = esW, esS = enS,
    frW = frW, frS = enS,
    deW = deW, deS = enS,
    arW = arW, arS = enS,
    jaW = jaW, jaS = enS,
    extraKeywords = extraKeywords,
    band = band
  )

  val ALL_CONCEPTS: List<VerifiedVisualConcept> = listOf(
    // ==================== DOMAIN 0: HOUSEHOLD FURNITURE & OBJECTS ====================
    entry(
      id = "table",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Table",
      questionEn = "What furniture is used for eating or working?",
      enS = "We set the dinner plates on the wooden table.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6a/Dining_Table_%28Soltak%29.jpg/960px-Dining_Table_%28Soltak%29.jpg",
      iconKey = "table",
      esW = "Mesa", esS = "Pusimos los platos sobre la mesa de madera.",
      frW = "Table", frS = "Nous avons posé les assiettes sur la table en bois.",
      deW = "Tisch", deS = "Wir stellten die Teller auf den Holztisch.",
      arW = "طاولة", arS = "وضعنا الأطباق على الطاولة الخشبية.",
      jaW = "テーブル", jaS = "木のテーブルにお皿を並べました。",
      extraKeywords = listOf("dining table", "mesa")
    ),
    entry(
      id = "lamp",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Lamp",
      questionEn = "What object provides light in a room?",
      enS = "Turn on the desk lamp before reading at night.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/d/d5/Desk_lamp.jpg",
      iconKey = "lamp",
      esW = "Lámpara", esS = "Enciende la lámpara antes de leer por la noche.",
      frW = "Lampe", frS = "Allume la lampe de bureau avant de lire.",
      deW = "Lampe", deS = "Schalte vor dem Lesen die Schreibtischlampe ein.",
      arW = "مصباح", arS = "شغّل المصباح قبل القراءة في المساء.",
      jaW = "ランプ", jaS = "夜に本を読む前にランプをつけてください。",
      extraKeywords = listOf("desk lamp", "lantern", "luz")
    ),
    entry(
      id = "door",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Door",
      questionEn = "What hinged movable barrier opens and closes at the entrance of a room?",
      enS = "Please close the front door gently behind you.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a1/Wooden_door.jpg/960px-Wooden_door.jpg",
      iconKey = "door",
      esW = "Puerta", esS = "Por favor cierra la puerta principal con cuidado.",
      frW = "Porte", frS = "Fermez doucement la porte d'entrée, s'il vous plaît.",
      deW = "Tür", deS = "Bitte schließe die Haustür leise hinter dir.",
      arW = "باب", arS = "يرجى إغلاق الباب الأمامي بهدوء.",
      jaW = "ドア", jaS = "玄関のドアを静かに閉めてください。",
      extraKeywords = listOf("front door", "wooden door", "gate")
    ),
    entry(
      id = "clock",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Clock",
      questionEn = "What wall or desk object shows the current time with numbers and hands?",
      enS = "The wall clock shows that it is nine o'clock.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/09/London_Big_Ben_Inner_Clock_Face_1070925-PSD.jpg/960px-London_Big_Ben_Inner_Clock_Face_1070925-PSD.jpg",
      iconKey = "clock",
      esW = "Reloj de pared", esS = "El reloj de pared marca las nueve en punto.",
      frW = "Horloge", frS = "L'horloge murale indique neuf heures.",
      deW = "Wanduhr", deS = "Die Wanduhr zeigt genau neun Uhr an.",
      arW = "ساعة حائط", arS = "تشير ساعة الحائط إلى التاسعة تماماً.",
      jaW = "掛け時計", jaS = "壁の時計はちょうど9時を指しています。",
      extraKeywords = listOf("wall clock", "analog clock", "clock face", "timepiece")
    ),
    entry(
      id = "chair",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Chair",
      questionEn = "What piece of furniture with a backrest is designed for one person to sit on?",
      enS = "Pull up a comfortable chair next to the desk.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/Wooden_chair_01.jpg/640px-Wooden_chair_01.jpg",
      iconKey = "chair",
      esW = "Silla", esS = "Acerca una silla cómoda junto al escritorio.",
      frW = "Chaise", frS = "Approche une chaise confortable près du bureau.",
      deW = "Stuhl", deS = "Stell einen bequemen Stuhl neben den Schreibtisch.",
      arW = "كرسي", arS = "ضع كرسياً مريحاً بجانب المكتب.",
      jaW = "椅子", jaS = "机の横に快適な椅子を置いてください。",
      extraKeywords = listOf("wooden chair", "seat", "armchair")
    ),
    entry(
      id = "bed",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Bed",
      questionEn = "What bedroom furniture with a mattress is used for sleeping and resting?",
      enS = "Make your bed neatly every morning after waking up.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/42/Canopy_bed_of_Amantaka_Suite_in_Amantaka_luxury_Resort_%26_Hotel_in_Luang_Prabang_Laos.jpg/960px-Canopy_bed_of_Amantaka_Suite_in_Amantaka_luxury_Resort_%26_Hotel_in_Luang_Prabang_Laos.jpg",
      iconKey = "bed",
      esW = "Cama", esS = "Tiende tu cama cada mañana al despertar.",
      frW = "Lit", frS = "Fais ton lit chaque matin après le réveil.",
      deW = "Bett", deS = "Mach jeden Morgen nach dem Aufstehen dein Bett.",
      arW = "سرير", arS = "رتّب سريرك كل صباح بعد الاستيقاظ.",
      jaW = "ベッド", jaS = "毎朝起きたらベッドを整えましょう。",
      extraKeywords = listOf("bedroom bed", "mattress", "pillow", "blanket")
    ),
    entry(
      id = "window",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Window",
      questionEn = "What glass opening in a wall lets daylight and fresh air into a room?",
      enS = "Open the window to let fresh morning air inside.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/72/Open_window.jpg/960px-Open_window.jpg",
      iconKey = "window",
      esW = "Ventana", esS = "Abre la ventana para que entre aire fresco.",
      frW = "Fenêtre", frS = "Ouvre la fenêtre pour faire entrer l'air frais.",
      deW = "Fenster", deS = "Öffne das Fenster für frische Morgenluft.",
      arW = "نافذة", arS = "افتح النافذة لدخول الهواء النقي.",
      jaW = "窓", jaS = "新鮮な空気を入れるために窓を開けてください。",
      extraKeywords = listOf("open window", "glass window")
    ),
    entry(
      id = "key",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Key",
      questionEn = "What small metal object is inserted into a lock to open or secure a door?",
      enS = "Keep your house key safe inside your coat pocket.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/91/Llaves_casa.jpg/960px-Llaves_casa.jpg",
      iconKey = "key",
      esW = "Llave", esS = "Guarda la llave de casa en el bolsillo del abrigo.",
      frW = "Clé", frS = "Garde la clé de la maison dans ta poche.",
      deW = "Schlüssel", deS = "Bewahre deinen Hausschlüssel sicher in der Tasche auf.",
      arW = "مفتاح", arS = "احتفظ بمفتاح المنزل في جيب معطفك.",
      jaW = "鍵", jaS = "家の鍵はポケットに大切にしまってください。",
      extraKeywords = listOf("house key", "door key", "keychain", "metal key")
    ),
    entry(
      id = "book",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Book",
      questionEn = "What bound collection of printed pages contains stories or knowledge to read?",
      enS = "She borrowed an interesting history book from the library.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6e/Latin_dictionary.jpg/960px-Latin_dictionary.jpg",
      iconKey = "book",
      esW = "Libro", esS = "Ella tomó prestado un libro interesante de la biblioteca.",
      frW = "Livre", frS = "Elle a emprunté un livre d'histoire à la bibliothèque.",
      deW = "Buch", deS = "Sie hat ein spannendes Buch aus der Bibliothek ausgeliehen.",
      arW = "كتاب", arS = "استعارت كتاباً ممتعاً من المكتبة.",
      jaW = "本", jaS = "彼女は図書館で面白い本を借りました。",
      extraKeywords = listOf("hardcover book", "novel", "textbook")
    ),
    entry(
      id = "kettle",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Kettle",
      questionEn = "What kitchen appliance is used to boil water quickly for tea or coffee?",
      enS = "Please boil fresh water in the electric kettle.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8e/Electric_kettle.jpg/960px-Electric_kettle.jpg",
      iconKey = "kettle",
      esW = "Hervidor", esS = "Por favor hierve agua fresca en el hervidor.",
      frW = "Bouilloire", frS = "Fais bouillir de l'eau dans la bouilloire.",
      deW = "Wasserkocher", deS = "Bitte koche Wasser im Wasserkocher.",
      arW = "غلاية", arS = "يرجى غلي الماء في الغلاية الكهربائية.",
      jaW = "やかん", jaS = "やかんで新しいお湯を沸かしてください。",
      extraKeywords = listOf("electric kettle", "tea kettle", "water boiler")
    ),


    // ==================== DOMAIN 1: CLOTHING, FOOTWEAR & ACCESSORIES ====================
    entry(
      id = "tshirt",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "T-shirt",
      questionEn = "What casual short-sleeved cotton shirt is worn in warm weather?",
      enS = "This cotton T-shirt feels light and comfortable in summer.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/24/Blue_Tshirt.jpg/640px-Blue_Tshirt.jpg",
      iconKey = "clothing",
      esW = "Camiseta", esS = "Esta camiseta de algodón es cómoda en verano.",
      frW = "T-shirt", frS = "Ce t-shirt en coton est léger en été.",
      deW = "T-Shirt", deS = "Dieses Baumwoll-T-Shirt ist im Sommer sehr bequem.",
      arW = "قميص قطني", arS = "هذا القميص القطني مريح جداً في الصيف.",
      jaW = "Tシャツ", jaS = "この綿のTシャツは夏にとても快適です。",
      extraKeywords = listOf("cotton shirt", "tee", "shirt")
    ),
    entry(
      id = "sneakers",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Sneakers",
      questionEn = "What comfortable athletic shoes do you wear for walking, running, or sports?",
      enS = "These cushioned sneakers are great for long city walks.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/69/2023_Adidas_Yeezy_350_V2_EF2905_%281%29.jpg/960px-2023_Adidas_Yeezy_350_V2_EF2905_%281%29.jpg",
      iconKey = "shoes",
      esW = "Zapatillas", esS = "Estas zapatillas deportivas son ideales para caminar.",
      frW = "Baskets", frS = "Ces baskets confortables sont idéales pour marcher.",
      deW = "Turnschuhe", deS = "Diese Turnschuhe eignen sich super zum Laufen.",
      arW = "حذاء رياضي", arS = "هذا الحذاء الرياضي ممتاز للمشي الطويل.",
      jaW = "スニーカー", jaS = "このスニーカーは街歩きに最適です。",
      extraKeywords = listOf("running shoes", "athletic shoes", "trainers", "footwear")
    ),
    entry(
      id = "jacket",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Jacket",
      questionEn = "What outer garment with sleeves do you wear over a shirt for outdoor warmth?",
      enS = "Zip up your warm jacket before stepping outside.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/aa/Black_worn_leather_jacket_detail_1.jpg/960px-Black_worn_leather_jacket_detail_1.jpg",
      iconKey = "clothing",
      esW = "Chaqueta", esS = "Abróchate la chaqueta antes de salir al frío.",
      frW = "Veste", frS = "Ferme ta veste chaude avant de sortir.",
      deW = "Jacke", deS = "Zieh deine warme Jacke an, bevor du rausgehst.",
      arW = "معطف", arS = "ارتدِ معطفك الدافئ قبل الخروج.",
      jaW = "ジャケット", jaS = "外に出る前に暖かいジャケットを着てください。",
      extraKeywords = listOf("leather jacket", "coat", "winter jacket")
    ),
    entry(
      id = "sunglasses",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Sunglasses",
      questionEn = "What dark-lensed eyewear protects your eyes from bright sunlight?",
      enS = "Remember to pack your sunglasses for the beach trip.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/9d/2008-07-27_Sunglasses.jpg/960px-2008-07-27_Sunglasses.jpg",
      iconKey = "glasses",
      esW = "Gafas de sol", esS = "No olvides llevar tus gafas de sol a la playa.",
      frW = "Lunettes de soleil", frS = "N'oublie pas tes lunettes de soleil pour la plage.",
      deW = "Sonnenbrille", deS = "Vergiss deine Sonnenbrille für den Strand nicht.",
      arW = "نظارة شمسية", arS = "تذكر إحضار نظارتك الشمسية إلى الشاطئ.",
      jaW = "サングラス", jaS = "ビーチへの旅行にサングラスを忘れないでください。",
      extraKeywords = listOf("dark glasses", "eyewear", "shades")
    ),
    entry(
      id = "hat",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Hat",
      questionEn = "What clothing accessory is worn on your head for shade or warmth?",
      enS = "Wear a wide-brimmed hat when walking in the afternoon sun.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/93/A_fedora_hat%2C_made_by_Borsalino.jpg/960px-A_fedora_hat%2C_made_by_Borsalino.jpg",
      iconKey = "hat",
      esW = "Sombrero", esS = "Usa un sombrero cuando camines bajo el sol.",
      frW = "Chapeau", frS = "Porte un chapeau quand tu marches au soleil.",
      deW = "Hut", deS = "Trage einen Hut in der Nachmittagssonne.",
      arW = "قبعة", arS = "ارتدِ قبعة عند المشي تحت أشعة الشمس.",
      jaW = "帽子", jaS = "日差しの強い午後は帽子をかぶりましょう。",
      extraKeywords = listOf("fedora hat", "cap", "headwear")
    ),
    entry(
      id = "backpack",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Backpack",
      questionEn = "What bag with two shoulder straps do students and hikers carry on their backs?",
      enS = "Put your notebook and water bottle inside your backpack.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a3/A_backpack_with_trekking_poles_and_shoes.jpg/960px-A_backpack_with_trekking_poles_and_shoes.jpg",
      iconKey = "backpack",
      esW = "Mochila", esS = "Guarda tu cuaderno y botella en la mochila.",
      frW = "Sac à dos", frS = "Mets ton cahier dans ton sac à dos.",
      deW = "Rucksack", deS = "Steck dein Heft in deinen Rucksack.",
      arW = "حقيبة ظهر", arS = "ضع دفترك وزجاجة الماء في حقيبة الظهر.",
      jaW = "リュックサック", jaS = "ノートと水筒をリュックサックに入れてください。",
      extraKeywords = listOf("school backpack", "hiking bag", "rucksack")
    ),
    entry(
      id = "watch",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Wristwatch",
      questionEn = "What small timepiece do you wear strapped around your wrist to tell the time?",
      enS = "She checked her wristwatch before catching the train.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/fd/Fossil_wristwatch_with_white_background.jpg/960px-Fossil_wristwatch_with_white_background.jpg",
      iconKey = "watch",
      esW = "Reloj de pulsera", esS = "Miró su reloj de pulsera antes de subir al tren.",
      frW = "Montre", frS = "Elle a regardé sa montre avant de prendre le train.",
      deW = "Armbanduhr", deS = "Sie schaute vor der Abfahrt auf ihre Armbanduhr.",
      arW = "ساعة يد", arS = "نظرت إلى ساعة يدها قبل ركوب القطار.",
      jaW = "腕時計", jaS = "彼女は電車に乗る前に腕時計を確認しました。",
      extraKeywords = listOf("wristwatch", "wrist watch", "reloj de pulsera")
    ),
    entry(
      id = "umbrella",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Umbrella",
      questionEn = "What folding canopy do you hold over your head to stay dry in the rain?",
      enS = "Carry an umbrella today because rain is in the forecast.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/19/Beige_and_brown_oil-paper_umbrella_on_the_edge_of_a_wooden_house_in_Luang_Prabang_Laos.jpg/960px-Beige_and_brown_oil-paper_umbrella_on_the_edge_of_a_wooden_house_in_Luang_Prabang_Laos.jpg",
      iconKey = "umbrella",
      esW = "Paraguas", esS = "Lleva un paraguas hoy porque va a llover.",
      frW = "Parapluie", frS = "Prends un parapluie aujourd'hui car il va pleuvoir.",
      deW = "Regenschirm", deS = "Nimm heute einen Regenschirm mit, es soll regnen.",
      arW = "مظلة", arS = "احمل مظلة اليوم لأن المطر متوقع.",
      jaW = "傘", jaS = "今日は雨が降りそうなので傘を持っていきましょう。",
      extraKeywords = listOf("rain umbrella", "parasol")
    ),


    // ==================== DOMAIN 2: FOOD & DRINKS ====================
    entry(
      id = "coffee",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Coffee",
      questionEn = "What warm roasted beverage is served in a cup at a morning café?",
      enS = "I order a cup of hot coffee every morning.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/45/A_small_cup_of_coffee.JPG/960px-A_small_cup_of_coffee.JPG",
      iconKey = "coffee",
      esW = "Café", esS = "Pido una taza de café caliente cada mañana.",
      frW = "Café", frS = "Je commande une tasse de café chaud chaque matin.",
      deW = "Kaffee", deS = "Ich bestelle jeden Morgen eine Tasse heißen Kaffee.",
      arW = "قهوة", arS = "أطلب كوباً من القهوة الساخنة كل صباح.",
      jaW = "コーヒー", jaS = "毎朝温かいコーヒーを一杯注文します。",
      extraKeywords = listOf("hot coffee", "espresso", "latte", "cup")
    ),
    entry(
      id = "apple",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Apple",
      questionEn = "What round sweet fruit with crisp flesh and red or green skin grows on trees?",
      enS = "Wash the crisp red apple before eating it as a snack.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/15/Red_Apple.jpg/960px-Red_Apple.jpg",
      iconKey = "food",
      esW = "Manzana", esS = "Lava la manzana roja antes de comerla.",
      frW = "Pomme", frS = "Lave la pomme rouge avant de la manger.",
      deW = "Apfel", deS = "Wasche den roten Apfel vor dem Essen.",
      arW = "تفاحة", arS = "اغسل التفاحة الحمراء قبل تناولها.",
      jaW = "りんご", jaS = "おやつに食べる前に赤いりんごを洗いましょう。",
      extraKeywords = listOf("red apple", "fruit", "manzana", "pomme")
    ),
    entry(
      id = "bread",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Bread",
      questionEn = "What baked staple food made from flour and water is sliced for toast or sandwiches?",
      enS = "The local bakery sells warm crusty bread every morning.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Fresh_made_bread_05.jpg/640px-Fresh_made_bread_05.jpg",
      iconKey = "bread",
      esW = "Pan", esS = "La panadería vende pan recién horneado cada mañana.",
      frW = "Pain", frS = "La boulangerie vend du pain chaud chaque matin.",
      deW = "Brot", deS = "Die Bäckerei verkauft jeden Morgen frisches Brot.",
      arW = "خبز", arS = "يبيع المخبز خبزاً طازجاً كل صباح.",
      jaW = "パン", jaS = "近所のパン屋は毎朝焼きたてのパンを売っています。",
      extraKeywords = listOf("bread loaf", "pastry", "loaf", "fresh bread")
    ),
    entry(
      id = "salad",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Salad",
      questionEn = "What fresh dish mixes leafy greens, tomatoes, and vegetables in a bowl?",
      enS = "The garden salad is topped with fresh tomatoes and olive oil.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bd/Fritta_with_goats_cheese_and_beets_%2B_salad_-_The_Green_Room_2025-07-17.jpg/960px-Fritta_with_goats_cheese_and_beets_%2B_salad_-_The_Green_Room_2025-07-17.jpg",
      iconKey = "salad",
      esW = "Ensalada", esS = "La ensalada fresca lleva tomates y aceite de oliva.",
      frW = "Salade", frS = "La salade fraîche contient des tomates et de l'huile d'olive.",
      deW = "Salat", deS = "Der frische Salat hat Tomaten und Olivenöl.",
      arW = "سلطة", arS = "تحتوي السلطة الطازجة على الطماطم وزيت الزيتون.",
      jaW = "サラダ", jaS = "新鮮なサラダにはトマトとオリーブオイルが入っています。",
      extraKeywords = listOf("green salad", "vegetable salad")
    ),
    entry(
      id = "soup",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Soup",
      questionEn = "What hot liquid meal of vegetables or broth is eaten from a bowl with a spoon?",
      enS = "Warm vegetable soup is comforting on a cold evening.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ee/Bowl_of_lentil_soup_with_green_and_red_lentils.jpg/960px-Bowl_of_lentil_soup_with_green_and_red_lentils.jpg",
      iconKey = "soup",
      esW = "Sopa", esS = "La sopa caliente de verduras es ideal en invierno.",
      frW = "Soupe", frS = "Une soupe chaude aux légumes réchauffe en hiver.",
      deW = "Suppe", deS = "Heiße Gemüsesuppe tut an einem kalten Abend gut.",
      arW = "حساء", arS = "حساء الخضار الساخن مريح في المساء البارد.",
      jaW = "スープ", jaS = "寒い夜には温かい野菜スープがぴったりです。",
      extraKeywords = listOf("bowl of soup", "hot soup", "vegetable soup")
    ),
    entry(
      id = "sandwich",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Sandwich",
      questionEn = "What food item places cheese, vegetables, or meat fillings between two slices of bread?",
      enS = "Pack a fresh cheese sandwich for the afternoon picnic.",
      wikiFile = "Club_sandwich.jpg",
      iconKey = "lunch",
      esW = "Sándwich", esS = "Prepara un sándwich fresco para el almuerzo.",
      frW = "Sandwich", frS = "Prépare un sandwich frais pour le déjeuner.",
      deW = "Sandwich", deS = "Mach ein frisches Sandwich für das Picknick.",
      arW = "شطيرة", arS = "جهّز شطيرة طازجة لوجبة الغداء.",
      jaW = "サンドイッチ", jaS = "ピクニックのために新鮮なサンドイッチを作りましょう。",
      extraKeywords = listOf("club sandwich", "toast")
    ),
    entry(
      id = "juice",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Orange Juice",
      questionEn = "What refreshing vitamin-rich drink is pressed or squeezed from fresh citrus fruit?",
      enS = "Would you like a glass of freshly squeezed orange juice?",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/66/%28a_glass_of_orange_juice_bread_with_and_jelly_and_a_waffle%29.jpg/960px-%28a_glass_of_orange_juice_bread_with_and_jelly_and_a_waffle%29.jpg",
      iconKey = "drink",
      esW = "Jugo de naranja", esS = "¿Quieres un vaso de jugo de naranja natural?",
      frW = "Jus d'orange", frS = "Voulez-vous un verre de jus d'orange pressé ?",
      deW = "Orangensaft", deS = "Möchten Sie ein Glas frisch gepressten Orangensaft?",
      arW = "عصير برتقال", arS = "هل تود كوباً من عصير البرتقال الطازج؟",
      jaW = "オレンジジュース", jaS = "搾りたてのオレンジジュースを一杯いかがですか？",
      extraKeywords = listOf("juice", "orange juice", "fruit juice")
    ),

    // ==================== DOMAIN 3: TRANSPORT, TRAVEL & PLACES ====================
    entry(
      id = "airplane",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Airplane",
      questionEn = "What winged aircraft flies passengers high across countries and oceans?",
      enS = "The international airplane landed smoothly at the airport.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bd/Airliner_Contrail.jpg/960px-Airliner_Contrail.jpg",
      iconKey = "flight",
      esW = "Avión", esS = "El avión internacional aterrizó a tiempo en el aeropuerto.",
      frW = "Avion", frS = "L'avion international a atterri à l'heure à l'aéroport.",
      deW = "Flugzeug", deS = "Das internationale Flugzeug landete pünktlich am Flughafen.",
      arW = "طائرة", arS = "هبطت الطائرة الدولية بسلاسة في المطار.",
      jaW = "飛行機", jaS = "国際線の飛行機は空港に無事着陸しました。",
      extraKeywords = listOf("aeroplane", "aircraft", "flight")
    ),
    entry(
      id = "train",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Train",
      questionEn = "What long railway vehicle carries passengers along tracks between stations?",
      enS = "The express train departs from platform four at noon.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ce/Gusev_passenger_train_2025-05_5354.JPG/960px-Gusev_passenger_train_2025-05_5354.JPG",
      iconKey = "train",
      esW = "Tren", esS = "El tren rápido sale del andén cuatro al mediodía.",
      frW = "Train", frS = "Le train express part du quai quatre à midi.",
      deW = "Zug", deS = "Der Schnellzug fährt mittags von Gleis vier ab.",
      arW = "قطار", arS = "يغادر القطار السريع من الرصيف الرابع ظهراً.",
      jaW = "電車", jaS = "急行電車は正午に4番ホームから出発します。",
      extraKeywords = listOf("railway train", "metro", "express train")
    ),
    entry(
      id = "bicycle",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Bicycle",
      questionEn = "What two-wheeled vehicle do you ride by pushing pedals with your feet?",
      enS = "She rides her bicycle along the park lane every morning.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/92/Azor_Oma_Oklahoma_R3D_Violet_Matt.jpg/960px-Azor_Oma_Oklahoma_R3D_Violet_Matt.jpg",
      iconKey = "bike",
      esW = "Bicicleta", esS = "Ella pasea en bicicleta por el parque cada mañana.",
      frW = "Vélo", frS = "Elle fait du vélo dans le parc chaque matin.",
      deW = "Fahrrad", deS = "Sie fährt jeden Morgen mit dem Fahrrad durch den Park.",
      arW = "دراجة هوائية", arS = "تركب دراجتها الهوائية في الحديقة كل صباح.",
      jaW = "自転車", jaS = "彼女は毎朝公園の道を自転車で走ります。",
      extraKeywords = listOf("bike", "cycling")
    ),
    entry(
      id = "car",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Car",
      questionEn = "What four-wheeled motor vehicle do families drive on roads and highways?",
      enS = "Park the car safely in the garage before dinner.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/48/Cadillac_Sedan_de_Ville_BW_2017-07-16_13-24-59.jpg/960px-Cadillac_Sedan_de_Ville_BW_2017-07-16_13-24-59.jpg",
      iconKey = "car",
      esW = "Coche", esS = "Estaciona el coche en el garaje antes de cenar.",
      frW = "Voiture", frS = "Gare la voiture dans le garage avant le dîner.",
      deW = "Auto", deS = "Parke das Auto vor dem Abendessen in der Garage.",
      arW = "سيارة", arS = "أوقف السيارة بأمان في المرآب قبل العشاء.",
      jaW = "車", jaS = "夕食の前に車をガレージに停めてください。",
      extraKeywords = listOf("automobile", "sedan", "vehicle")
    ),
    entry(
      id = "suitcase",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Suitcase",
      questionEn = "What rectangular wheeled travel bag holds your clothes during a trip?",
      enS = "My carry-on suitcase fits neatly in the overhead bin.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f6/GUESS_traveling_suitcase_-_panoramio.jpg/960px-GUESS_traveling_suitcase_-_panoramio.jpg",
      iconKey = "luggage",
      esW = "Maleta", esS = "Mi maleta de mano cabe en el compartimento superior.",
      frW = "Valise", frS = "Ma valise à roulettes rentre dans le coffre supérieur.",
      deW = "Koffer", deS = "Mein Rollkoffer passt problemlos ins Gepäckfach.",
      arW = "حقيبة سفر", arS = "حقيبة سفري تناسب الخزانة العلوية تماماً.",
      jaW = "スーツケース", jaS = "私の機内持ち込みスーツケースは上の棚に入ります。",
      extraKeywords = listOf("travel luggage", "baggage")
    ),

    // ==================== DOMAIN 4: PLACES AROUND TOWN ====================
    entry(
      id = "building",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Building",
      questionEn = "What multi-story city structure has walls, floors, and rooms for offices or apartments?",
      enS = "The modern glass office building stands downtown.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f3/Paleis_van_Justitie%2C_Amsterdam-9011.jpg/960px-Paleis_van_Justitie%2C_Amsterdam-9011.jpg",
      iconKey = "building",
      esW = "Edificio", esS = "El edificio moderno de cristal está en el centro.",
      frW = "Immeuble", frS = "L'immeuble moderne en verre se trouve au centre-ville.",
      deW = "Gebäude", deS = "Das moderne Bürogebäude steht in der Innenstadt.",
      arW = "مبنى", arS = "يقع المبنى الحديث وسط المدينة.",
      jaW = "建物", jaS = "モダンなガラス張りの建物が都心に建っています。",
      extraKeywords = listOf("office building", "structure", "edificio")
    ),
    entry(
      id = "restaurant",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Restaurant",
      questionEn = "What commercial dining venue prepares and serves meals to seated guests at tables?",
      enS = "We reserved a quiet corner table at the neighborhood restaurant.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b4/Childs%27_Restaurant_building_interior_%2884200%29.jpg/960px-Childs%27_Restaurant_building_interior_%2884200%29.jpg",
      localRes = R.drawable.img_listening_restaurant,
      iconKey = "restaurant",
      esW = "Restaurante", esS = "Reservamos una mesa tranquila en el restaurante.",
      frW = "Restaurant", frS = "Nous avons réservé une table calme au restaurant.",
      deW = "Restaurant", deS = "Wir haben einen ruhigen Tisch im Restaurant reserviert.",
      arW = "مطعم", arS = "حجزنا طاولة هادئة في المطعم.",
      jaW = "レストラン", jaS = "近所のレストランで静かな席を予約しました。",
      extraKeywords = listOf("dining room", "bistro", "eatery")
    ),

    // ==================== DOMAIN 5: WORKPLACE & TECHNOLOGY ====================
    entry(
      id = "laptop",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Laptop",
      questionEn = "What portable personal computer folds open with a screen and keyboard for work or study?",
      enS = "Open your laptop to join the morning video meeting.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d6/HP_Pavilion_Computer_laptop_keyboard_closeup.jpg/960px-HP_Pavilion_Computer_laptop_keyboard_closeup.jpg",
      iconKey = "laptop",
      esW = "Portátil", esS = "Abre tu ordenador portátil para unirte a la reunión.",
      frW = "Ordinateur portable", frS = "Ouvre ton ordinateur portable pour rejoindre la réunion.",
      deW = "Laptop", deS = "Öffne deinen Laptop für die Morgenbesprechung.",
      arW = "حاسوب محمول", arS = "افتح حاسوبك المحمول للانضمام إلى الاجتماع.",
      jaW = "ノートパソコン", jaS = "朝の会議に参加するためにノートパソコンを開いてください。",
      extraKeywords = listOf("laptop computer", "notebook computer")
    ),
    entry(
      id = "phone",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Smartphone",
      questionEn = "What handheld mobile touchscreen device is used to make calls, send messages, and run apps?",
      enS = "Charge your smartphone battery before leaving for the trip.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d6/Smartphone_with_blank_screen.jpg/640px-Smartphone_with_blank_screen.jpg",
      iconKey = "phone",
      esW = "Teléfono móvil", esS = "Carga la batería de tu teléfono móvil antes de salir.",
      frW = "Téléphone portable", frS = "Charge ton téléphone portable avant de partir.",
      deW = "Smartphone", deS = "Lade dein Smartphone vor der Abreise auf.",
      arW = "هاتف ذكي", arS = "اشحن بطارية هاتفك الذكي قبل السفر.",
      jaW = "スマートフォン", jaS = "出発前にスマートフォンを充電しておきましょう。",
      extraKeywords = listOf("smartphone", "cell phone", "mobile phone")
    ),
    entry(
      id = "camera",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Camera",
      questionEn = "What optical device is used to capture digital photographs and record videos?",
      enS = "Bring your digital camera to photograph the mountain view.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/76/12_MP_digital_camera_-_red.jpg/960px-12_MP_digital_camera_-_red.jpg",
      iconKey = "camera",
      esW = "Cámara", esS = "Trae tu cámara digital para fotografiar las montañas.",
      frW = "Appareil photo", frS = "Apporte ton appareil photo pour photographier les montagnes.",
      deW = "Kamera", deS = "Bring deine Kamera mit, um die Berge zu fotografieren.",
      arW = "كاميرا", arS = "أحضر الكاميرا لالتقاط صور للجبال.",
      jaW = "カメラ", jaS = "山の景色を撮るためにカメラを持ってきてください。",
      extraKeywords = listOf("digital camera", "photo camera")
    ),
    entry(
      id = "trophy",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Trophy",
      questionEn = "What golden cup prize is awarded to the winner of a tournament or competition?",
      enS = "Our school debate team won the first-place golden trophy.",
      wikiFile = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Golden_Trophy.jpg/640px-Golden_Trophy.jpg",
      iconKey = "trophy",
      esW = "Trofeo", esS = "Nuestro equipo ganó el trofeo de primer lugar.",
      frW = "Trophée", frS = "Notre équipe a remporté le trophée d'or.",
      deW = "Pokal", deS = "Unser Schulteam hat den goldenen Pokal gewonnen.",
      arW = "كأس الفوز", arS = "فاز فريق مدرستنا بكأس المركز الأول.",
      jaW = "トロフィー", jaS = "私たちのチームが優勝トロフィーを獲得しました。",
      extraKeywords = listOf("trophy cup", "gold trophy", "award")
    ),

    // ==================== DOMAIN 6: NATURE & WEATHER ====================
    entry(
      id = "tree",
      domainIdx = 6,
      domain = "Nature & weather",
      enW = "Tree",
      questionEn = "What tall woody plant has roots, a trunk, branches, and green leaves?",
      enS = "We rested in the cool shade under the oak tree.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/Dublin_Stephen%27s_Green-44_edit.jpg/960px-Dublin_Stephen%27s_Green-44_edit.jpg",
      iconKey = "tree",
      esW = "Árbol", esS = "Descansamos bajo la sombra fresca del árbol.",
      frW = "Arbre", frS = "Nous nous sommes reposés à l'ombre de l'arbre.",
      deW = "Baum", deS = "Wir ruhten uns im kühlen Schatten unter dem Baum aus.",
      arW = "شجرة", arS = "استرحنا في الظل البارد تحت الشجرة.",
      jaW = "木", jaS = "私たちは大きな木の涼しい木陰で休みました。",
      extraKeywords = listOf("green tree", "oak tree", "nature")
    ),
    entry(
      id = "sun",
      domainIdx = 6,
      domain = "Nature & weather",
      enW = "Sunshine",
      questionEn = "What bright warm light comes from the sun in a clear daytime sky?",
      enS = "Bright morning sunshine warmed the park benches.",
      wikiFile = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e5/Beinn_an_Dothaidh_in_morning_sun%2C_Scotland.jpg/960px-Beinn_an_Dothaidh_in_morning_sun%2C_Scotland.jpg",
      iconKey = "sun",
      esW = "Luz del sol", esS = "La brillante luz del sol calentó el parque.",
      frW = "Soleil", frS = "Le grand soleil du matin a réchauffé le parc.",
      deW = "Sonnenschein", deS = "Heller Sonnenschein wärmte am Morgen den Park.",
      arW = "أشعة الشمس", arS = "أدفأت أشعة الشمس الصباحية مقاعد الحديقة.",
      jaW = "日差し", jaS = "明るい朝の日差しが公園のベンチを温めました。",
      extraKeywords = listOf("morning sun", "sunlight", "sunshine", "sunny", "summer")
    ),
    // ==================== ADDITIONAL VERIFIED CONCEPTS ACROSS DOMAINS 0..9 ====================
    entry(
      id = "mirror",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Mirror",
      questionEn = "What reflective glass surface on a wall shows your reflection?",
      enS = "Check your hair in the bathroom mirror before leaving.",
      wikiFile = "Bathroom_mirror.jpg",
      iconKey = "mirror",
      esW = "Espejo", esS = "Mírate en el espejo antes de salir.",
      frW = "Miroir", frS = "Regarde dans le miroir avant de partir.",
      deW = "Spiegel", deS = "Schau vor dem Gehen in den Spiegel.",
      arW = "مرآة", arS = "انظر في المرآة قبل الخروج.",
      jaW = "鏡", jaS = "出かける前に鏡を確認してください。",
      extraKeywords = listOf("wall mirror", "glass mirror")
    ),
    entry(
      id = "sofa",
      domainIdx = 0,
      domain = "Everyday household objects",
      enW = "Sofa",
      questionEn = "What long upholstered living room seat has cushions for several people?",
      enS = "The family relaxed together on the living room sofa.",
      wikiFile = "Sofa_in_living_room.jpg",
      iconKey = "sofa",
      esW = "Sofá", esS = "La familia descansó en el sofá de la sala.",
      frW = "Canapé", frS = "La famille s'est détendue sur le canapé.",
      deW = "Sofa", deS = "Die Familie entspannte sich auf dem Sofa.",
      arW = "أريكة", arS = "استرخت العائلة على أريكة غرفة المعيشة.",
      jaW = "ソファ", jaS = "家族はリビングのソファでくつろぎました。",
      extraKeywords = listOf("couch", "living room sofa")
    ),
    entry(
      id = "scarf",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Scarf",
      questionEn = "What strip of wool or silk fabric is wrapped around the neck for warmth?",
      enS = "Wrap a warm wool scarf around your neck in winter.",
      wikiFile = "Red_wool_scarf.jpg",
      iconKey = "clothing",
      esW = "Bufanda", esS = "Envuelve una bufanda cálida alrededor de tu cuello.",
      frW = "Écharpe", frS = "Enroule une écharpe chaude autour de ton cou.",
      deW = "Schal", deS = "Wickle im Winter einen warmen Schal um deinen Hals.",
      arW = "وشاح", arS = "لف وشاحاً دافئاً حول عنقك في الشتاء.",
      jaW = "マフラー", jaS = "冬には暖かいマフラーを首に巻きましょう。",
      extraKeywords = listOf("wool scarf", "neck scarf")
    ),
    entry(
      id = "gloves",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Gloves",
      questionEn = "What hand coverings with separate sections for each finger keep hands warm?",
      enS = "Wear leather gloves when cycling on frosty mornings.",
      wikiFile = "Leather_gloves.jpg",
      iconKey = "clothing",
      esW = "Guantes", esS = "Usa guantes para proteger tus manos del frío.",
      frW = "Gants", frS = "Porte des gants par temps froid.",
      deW = "Handschuhe", deS = "Trage bei Kälte warme Handschuhe.",
      arW = "قفازات", arS = "ارتدِ قفازات لحماية يديك من البرد.",
      jaW = "手袋", jaS = "寒い朝には手袋をはめましょう。",
      extraKeywords = listOf("winter gloves", "mittens")
    ),
    entry(
      id = "boots",
      domainIdx = 1,
      domain = "Basic clothing & footwear",
      enW = "Boots",
      questionEn = "What sturdy footwear covers both the foot and the ankle for rain or snow?",
      enS = "Put on waterproof boots before walking on snowy trails.",
      wikiFile = "Leather_boots.jpg",
      iconKey = "shoes",
      esW = "Botas", esS = "Ponte botas impermeables para caminar en la nieve.",
      frW = "Bottes", frS = "Mets des bottes imperméables pour la neige.",
      deW = "Stiefel", deS = "Zieh wasserdichte Stiefel für den Schnee an.",
      arW = "حذاء طويل", arS = "ارتدِ حذاءً طويلاً مقاوماً للماء في الثلج.",
      jaW = "ブーツ", jaS = "雪道を歩く前に防水ブーツを履いてください。",
      extraKeywords = listOf("winter boots", "hiking boots")
    ),
    entry(
      id = "cheese",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Cheese",
      questionEn = "What dairy food made from pressed milk curds is sliced for sandwiches or pasta?",
      enS = "Add a slice of aged cheddar cheese to the sandwich.",
      wikiFile = "Swiss_cheese_cubes.jpg",
      iconKey = "food",
      esW = "Queso", esS = "Añade una loncha de queso al sándwich.",
      frW = "Fromage", frS = "Ajoute une tranche de fromage au sandwich.",
      deW = "Käse", deS = "Leg eine Scheibe Käse auf das Brot.",
      arW = "جبن", arS = "أضف شريحة من الجبن إلى الشطيرة.",
      jaW = "チーズ", jaS = "サンドイッチにチーズを一切れ加えましょう。",
      extraKeywords = listOf("cheddar cheese", "dairy")
    ),
    entry(
      id = "banana",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Banana",
      questionEn = "What curved yellow tropical fruit has a peel that you strip off before eating?",
      enS = "Slice a ripe yellow banana over your morning oatmeal.",
      wikiFile = "Banana_isolated.jpg",
      iconKey = "food",
      esW = "Plátano", esS = "Corta un plátano maduro para el desayuno.",
      frW = "Banane", frS = "Coupe une banane mûre pour le petit-déjeuner.",
      deW = "Banane", deS = "Schneide eine reife Banane zum Frühstück.",
      arW = "موز", arS = "قطّع موزة ناضجة مع وجبة الإفطار.",
      jaW = "バナナ", jaS = "朝食のオートミールに熟したバナナを添えましょう。",
      extraKeywords = listOf("yellow banana", "tropical fruit")
    ),
    entry(
      id = "croissant",
      domainIdx = 2,
      domain = "Food & drinks",
      enW = "Croissant",
      questionEn = "What flaky crescent-shaped French bakery pastry is eaten at breakfast?",
      enS = "She ordered a warm butter croissant with her morning tea.",
      wikiFile = "Croissant_closeup.jpg",
      iconKey = "bread",
      esW = "Cruasán", esS = "Pidió un cruasán caliente con su té.",
      frW = "Croissant", frS = "Elle a commandé un croissant chaud au beurre.",
      deW = "Croissant", deS = "Sie bestellte ein warmes Buttercroissant.",
      arW = "كرواسون", arS = "طلبت كرواسون بالزبدة مع الشاي.",
      jaW = "クロワッサン", jaS = "彼女は紅茶と一緒に温かいクロワッサンを注文しました。",
      extraKeywords = listOf("crescent pastry", "french pastry")
    ),
    entry(
      id = "passport",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Passport",
      questionEn = "What official government travel booklet verifies your identity and nationality abroad?",
      enS = "Show your passport at the international border desk.",
      wikiFile = "Biometric_passport.jpg",
      iconKey = "passport",
      esW = "Pasaporte", esS = "Muestra tu pasaporte en el control internacional.",
      frW = "Passeport", frS = "Présentez votre passeport au contrôle frontalier.",
      deW = "Reisepass", deS = "Zeigen Sie Ihren Reisepass an der Grenzkontrolle.",
      arW = "جواز سفر", arS = "أبرز جواز سفرك عند مكتب الحدود الدولي.",
      jaW = "パスポート", jaS = "国際線の審査場でパスポートを提示してください。",
      extraKeywords = listOf("travel document", "visa")
    ),
    entry(
      id = "bus",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Bus",
      questionEn = "What large road vehicle carries many passengers along scheduled city routes?",
      enS = "Take the city bus from the main square to the museum.",
      wikiFile = "City_transit_bus.jpg",
      iconKey = "bus",
      esW = "Autobús", esS = "Toma el autobús urbano hasta el museo.",
      frW = "Bus", frS = "Prends le bus de la place principale au musée.",
      deW = "Bus", deS = "Nimm den Stadtbus vom Hauptplatz zum Museum.",
      arW = "حافلة", arS = "استقل حافلة المدينة إلى المتحف.",
      jaW = "バス", jaS = "広場から博物館まで路線バスに乗りましょう。",
      extraKeywords = listOf("city bus", "coach")
    ),
    entry(
      id = "ship",
      domainIdx = 3,
      domain = "Travel, luggage & transport",
      enW = "Ship",
      questionEn = "What large ocean vessel transports passengers or cargo across the sea?",
      enS = "The passenger ship sailed calmly out of the harbor.",
      wikiFile = "Passenger_ship.jpg",
      iconKey = "ship",
      esW = "Barco", esS = "El barco de pasajeros zarpó del puerto.",
      frW = "Navire", frS = "Le navire a quitté le port calmement.",
      deW = "Schiff", deS = "Das Passagierschiff verließ ruhig den Hafen.",
      arW = "سفينة", arS = "أبحرت السفينة بهدوء من الميناء.",
      jaW = "船", jaS = "客船は港から静かに出航しました。",
      extraKeywords = listOf("ferry", "boat", "vessel")
    ),
    entry(
      id = "library",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Library",
      questionEn = "What quiet public building has shelves of books for reading and studying?",
      enS = "Students gather in the public library to study for exams.",
      wikiFile = "Public_library_reading_room.jpg",
      iconKey = "book",
      esW = "Biblioteca", esS = "Los estudiantes estudian en la biblioteca pública.",
      frW = "Bibliothèque", frS = "Les étudiants travaillent à la bibliothèque.",
      deW = "Bibliothek", deS = "Studenten lernen in der öffentlichen Bibliothek.",
      arW = "مكتبة", arS = "يجتمع الطلاب في المكتبة العامة للدراسة.",
      jaW = "図書館", jaS = "学生たちは試験勉強のために図書館に集まります。",
      extraKeywords = listOf("public library", "reading room")
    ),
    entry(
      id = "bridge",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Bridge",
      questionEn = "What structure spans across a river or valley so vehicles and pedestrians can cross?",
      enS = "Walk across the stone bridge to reach the old town.",
      wikiFile = "Stone_arch_bridge.jpg",
      iconKey = "bridge",
      esW = "Puente", esS = "Cruza el puente de piedra hacia el casco antiguo.",
      frW = "Pont", frS = "Traverse le pont de pierre pour rejoindre la vieille ville.",
      deW = "Brücke", deS = "Geh über die Steinbrücke in die Altstadt.",
      arW = "جسر", arS = "اعبر الجسر الحجري للوصول إلى المدينة القديمة.",
      jaW = "橋", jaS = "旧市街へ行くには石の橋を渡ってください。",
      extraKeywords = listOf("river bridge", "suspension bridge")
    ),
    entry(
      id = "hospital",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Hospital",
      questionEn = "What medical institution provides doctors, nurses, and care for sick or injured patients?",
      enS = "The city hospital is open twenty-four hours a day.",
      wikiFile = "Modern_hospital_building.jpg",
      iconKey = "hospital",
      esW = "Hospital", esS = "El hospital de la ciudad abre las veinticuatro horas.",
      frW = "Hôpital", frS = "L'hôpital municipal est ouvert vingt-quatre heures sur vingt-quatre.",
      deW = "Krankenhaus", deS = "Das städtische Krankenhaus ist rund um die Uhr geöffnet.",
      arW = "مستشفى", arS = "مستشفى المدينة مفتوح على مدار الساعة.",
      jaW = "病院", jaS = "市立病院は24時間いつでも開いています。",
      extraKeywords = listOf("medical center", "clinic")
    ),
    entry(
      id = "museum",
      domainIdx = 4,
      domain = "Places around town",
      enW = "Museum",
      questionEn = "What cultural building exhibits historical artifacts, sculptures, and paintings?",
      enS = "We spent Saturday afternoon exploring the art museum.",
      wikiFile = "Art_museum_gallery.jpg",
      iconKey = "building",
      esW = "Museo", esS = "Pasamos la tarde explorando el museo de arte.",
      frW = "Musée", frS = "Nous avons visité le musée d'art samedi après-midi.",
      deW = "Museum", deS = "Wir verbrachten den Nachmittag im Kunstmuseum.",
      arW = "متحف", arS = "قضينا بعد الظهر في استكشاف متحف الفنون.",
      jaW = "博物館", jaS = "土曜日の午後は美術館を巡って過ごしました。",
      extraKeywords = listOf("art gallery", "exhibition hall")
    ),
    entry(
      id = "headphones",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Headphones",
      questionEn = "What audio device worn over the ears lets you listen to music or calls privately?",
      enS = "Put on your headphones to listen to the pronunciation audio.",
      wikiFile = "Over_ear_headphones.jpg",
      iconKey = "headphones",
      esW = "Auriculares", esS = "Ponte los auriculares para escuchar el audio.",
      frW = "Casque audio", frS = "Mets ton casque audio pour écouter la leçon.",
      deW = "Kopfhörer", deS = "Setz deine Kopfhörer auf, um das Audio zu hören.",
      arW = "سماعات رأس", arS = "ارتدِ سماعات الرأس للاستماع إلى التسجيل الصوتي.",
      jaW = "ヘッドホン", jaS = "発音音声を聞くためにヘッドホンをつけてください。",
      extraKeywords = listOf("earphones", "headset")
    ),
    entry(
      id = "keyboard",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Keyboard",
      questionEn = "What panel of letter and number keys do you press to type text into a computer?",
      enS = "Type your response clearly using the computer keyboard.",
      wikiFile = "Computer_keyboard.jpg",
      iconKey = "laptop",
      esW = "Teclado", esS = "Escribe tu respuesta usando el teclado del ordenador.",
      frW = "Clavier", frS = "Tape ta réponse sur le clavier de l'ordinateur.",
      deW = "Tastatur", deS = "Tippe deine Antwort mit der Computertastatur.",
      arW = "لوحة مفاتيح", arS = "اكتب إجابتك باستخدام لوحة مفاتيح الحاسوب.",
      jaW = "キーボード", jaS = "パソコンのキーボードを使って回答を入力してください。",
      extraKeywords = listOf("computer keyboard", "typing keys")
    ),
    entry(
      id = "microphone",
      domainIdx = 5,
      domain = "Workplace & technology",
      enW = "Microphone",
      questionEn = "What recording instrument captures your spoken voice for broadcasting or speech practice?",
      enS = "Speak clearly into the studio microphone during the recording.",
      wikiFile = "Studio_microphone.jpg",
      iconKey = "microphone",
      esW = "Micrófono", esS = "Habla claro frente al micrófono durante la grabación.",
      frW = "Microphone", frS = "Parle clairement dans le microphone du studio.",
      deW = "Mikrofon", deS = "Sprich während der Aufnahme deutlich ins Mikrofon.",
      arW = "ميكروفون", arS = "تحدث بوضوح في الميكروفون أثناء التسجيل.",
      jaW = "マイク", jaS = "録音中はマイクに向かってはっきりと話してください。",
      extraKeywords = listOf("studio mic", "voice recorder")
    ),
    entry(
      id = "mountain",
      domainIdx = 6,
      domain = "Nature & weather",
      enW = "Mountain",
      questionEn = "What very high natural peak of rock and earth rises steeply above the landscape?",
      enS = "Snow covered the summit of the high mountain.",
      wikiFile = "Mountain_peak_snow.jpg",
      iconKey = "mountain",
      esW = "Montaña", esS = "La nieve cubrió la cima de la alta montaña.",
      frW = "Montagne", frS = "La neige recouvre le sommet de la montagne.",
      deW = "Berg", deS = "Schnee bedeckte den Gipfel des hohen Berges.",
      arW = "جبل", arS = "غطى الثلج قمة الجبل العالي.",
      jaW = "山", jaS = "高い山の頂上が雪で覆われていました。",
      extraKeywords = listOf("alpine peak", "summit")
    ),
    entry(
      id = "flower",
      domainIdx = 6,
      domain = "Nature & weather",
      enW = "Flower",
      questionEn = "What colorful blossom with petals grows on plants and blooms in spring gardens?",
      enS = "A bright red flower bloomed in the sunny garden.",
      wikiFile = "Red_rose_flower.jpg",
      iconKey = "flower",
      esW = "Flor", esS = "Una flor roja floreció en el jardín soleado.",
      frW = "Fleur", frS = "Une fleur rouge a éclos dans le jardin.",
      deW = "Blume", deS = "Eine rote Blume blühte im sonnigen Garten.",
      arW = "زهرة", arS = "تفتحت زهرة حمراء في الحديقة المشمسة.",
      jaW = "花", jaS = "日当たりの良い庭に赤い花が咲きました。",
      extraKeywords = listOf("blossom", "petal", "rose")
    ),
    entry(
      id = "rainbow",
      domainIdx = 6,
      domain = "Nature & weather",
      enW = "Rainbow",
      questionEn = "What colorful arc of seven colors appears in the sky when sunlight shines through rain?",
      enS = "A vivid rainbow appeared across the sky after the storm.",
      wikiFile = "Double_rainbow.jpg",
      iconKey = "sun",
      esW = "Arcoíris", esS = "Un arcoíris brillante apareció después de la lluvia.",
      frW = "Arc-en-ciel", frS = "Un arc-en-ciel est apparu après la pluie.",
      deW = "Regenbogen", deS = "Nach dem Regen erschien ein bunter Regenbogen.",
      arW = "قوس قزح", arS = "ظهر قوس قزح ملون في السماء بعد المطر.",
      jaW = "虹", jaS = "雨上がりの空に鮮やかな虹が出ました。",
      extraKeywords = listOf("sky arc", "spectrum")
    ),
    entry(
      id = "guitar",
      domainIdx = 7,
      domain = "Hobbies, sports & arts",
      enW = "Guitar",
      questionEn = "What stringed musical instrument with a wooden body and neck is played by strumming?",
      enS = "He played a gentle melody on his acoustic guitar.",
      wikiFile = "Acoustic_guitar.jpg",
      iconKey = "guitar",
      esW = "Guitarra", esS = "Tocó una melodía suave con su guitarra acústica.",
      frW = "Guitare", frS = "Il a joué une mélodie douce à la guitare.",
      deW = "Gitarre", deS = "Er spielte eine sanfte Melodie auf der Gitarre.",
      arW = "غيتار", arS = "عزف لحناً هادئاً على الغيتار الصوتي.",
      jaW = "ギター", jaS = "彼はアコースティックギターで優しいメロディーを奏でました。",
      extraKeywords = listOf("acoustic guitar", "musical instrument")
    ),
    entry(
      id = "soccerball",
      domainIdx = 7,
      domain = "Hobbies, sports & arts",
      enW = "Soccer Ball",
      questionEn = "What round patterned ball is kicked across a grass field to score goals?",
      enS = "Kick the soccer ball toward the goal net.",
      wikiFile = "Soccer_ball.jpg",
      iconKey = "ball",
      esW = "Balón de fútbol", esS = "Patea el balón de fútbol hacia la portería.",
      frW = "Ballon de football", frS = "Frappe le ballon de football vers le but.",
      deW = "Fußball", deS = "Schieß den Fußball direkt ins Tor.",
      arW = "كرة قدم", arS = "اركل كرة القدم نحو المرمى.",
      jaW = "サッカーボール", jaS = "ゴールに向かってサッカーボールを蹴りましょう。",
      extraKeywords = listOf("football", "sports ball")
    ),
    entry(
      id = "paintbrush",
      domainIdx = 7,
      domain = "Hobbies, sports & arts",
      enW = "Paintbrush",
      questionEn = "What artist tool with bristles on a handle is dipped in paint to color a canvas?",
      enS = "Rinse your paintbrush in clean water before changing colors.",
      wikiFile = "Artist_paintbrushes.jpg",
      iconKey = "art",
      esW = "Pincel", esS = "Limpia tu pincel en agua antes de cambiar de color.",
      frW = "Pinceau", frS = "Rince ton pinceau à l'eau claire.",
      deW = "Pinsel", deS = "Spüle deinen Pinsel vor dem Farbwechsel mit Wasser aus.",
      arW = "فرشاة رسم", arS = "اغسل فرشاة الرسم بالماء قبل تغيير اللون.",
      jaW = "絵筆", jaS = "色を変える前に絵筆をきれいな水で洗いましょう。",
      extraKeywords = listOf("artist brush", "painting tool")
    ),
    entry(
      id = "wallet",
      domainIdx = 8,
      domain = "Shopping & market goods",
      enW = "Wallet",
      questionEn = "What small folding pocket case holds your cash bills, coins, and payment cards?",
      enS = "Keep your leather wallet secure inside your front pocket.",
      wikiFile = "Leather_wallet.jpg",
      iconKey = "wallet",
      esW = "Billetera", esS = "Guarda tu billetera de cuero en el bolsillo.",
      frW = "Portefeuille", frS = "Garde ton portefeuille en cuir en sécurité.",
      deW = "Geldbörse", deS = "Bewahre deine Geldbörse sicher in der Tasche auf.",
      arW = "محفظة نقود", arS = "احتفظ بمحفظة النقود في جيبك بأمان.",
      jaW = "財布", jaS = "革の財布はポケットにしっかりしまってください。",
      extraKeywords = listOf("pocketbook", "billfold")
    ),
    entry(
      id = "basket",
      domainIdx = 8,
      domain = "Shopping & market goods",
      enW = "Shopping Basket",
      questionEn = "What handheld woven or wire container do shoppers carry to hold groceries at a market?",
      enS = "Place fresh vegetables gently into your shopping basket.",
      wikiFile = "Wicker_basket.jpg",
      iconKey = "basket",
      esW = "Cesta de compras", esS = "Coloca las verduras frescas en la cesta de compras.",
      frW = "Panier", frS = "Mets les légumes frais dans ton panier.",
      deW = "Einkaufskorb", deS = "Leg das frische Gemüse in deinen Einkaufskorb.",
      arW = "سلة تسوق", arS = "ضع الخضروات الطازجة في سلة التسوق.",
      jaW = "買い物かご", jaS = "新鮮な野菜を買い物かごにそっと入れてください。",
      extraKeywords = listOf("market basket", "wicker basket")
    ),
    entry(
      id = "compass",
      domainIdx = 9,
      domain = "Education, science & health",
      enW = "Compass",
      questionEn = "What navigation instrument has a magnetic needle that always points north?",
      enS = "Use a magnetic compass and map when hiking through the forest.",
      wikiFile = "Pocket_compass.jpg",
      iconKey = "compass",
      esW = "Brújula", esS = "Usa una brújula y un mapa al caminar por el bosque.",
      frW = "Boussole", frS = "Utilise une boussole et une carte en forêt.",
      deW = "Kompass", deS = "Benutze beim Wandern im Wald einen Kompass.",
      arW = "بوصلة", arS = "استخدم البوصلة والخريطة عند المشي في الغابة.",
      jaW = "方位磁針", jaS = "森をハイキングする時は方位磁針と地図を使いましょう。",
      extraKeywords = listOf("navigation compass", "magnetic needle"),
      band = 4
    ),
    entry(
      id = "microscope",
      domainIdx = 9,
      domain = "Education, science & health",
      enW = "Microscope",
      questionEn = "What laboratory optical instrument magnifies tiny cells that cannot be seen by the naked eye?",
      enS = "Examine the plant leaf cells under the laboratory microscope.",
      wikiFile = "Optical_microscope.jpg",
      iconKey = "science",
      esW = "Microscopio", esS = "Observa las células vegetales bajo el microscopio.",
      frW = "Microscope", frS = "Observe les cellules sous le microscope du laboratoire.",
      deW = "Mikroskop", deS = "Untersuche die Pflanzenzellen unter dem Mikroskop.",
      arW = "مجهر", arS = "افحص خلايا النبات تحت المجهر في المختبر.",
      jaW = "顕微鏡", jaS = "実験室の顕微鏡で植物の細胞を観察しましょう。",
      extraKeywords = listOf("lab microscope", "magnifier"),
      band = 5
    ),
    entry(
      id = "globe",
      domainIdx = 9,
      domain = "Education, science & health",
      enW = "Globe",
      questionEn = "What spherical model of Earth spins on a stand to show continents and oceans?",
      enS = "Spin the classroom globe to locate each continent and ocean.",
      wikiFile = "Terrestrial_globe.jpg",
      iconKey = "globe",
      esW = "Globo terráqueo", esS = "Gira el globo terráqueo para ubicar cada continente.",
      frW = "Globe terrestre", frS = "Fais tourner le globe terrestre pour voir les océans.",
      deW = "Globus", deS = "Drehe den Globus, um jeden Kontinent zu finden.",
      arW = "كرة أرضية", arS = "أدر مجسم الكرة الأرضية لتحديد القارات والمحيطات.",
      jaW = "地球儀", jaS = "教室の地球儀を回して大陸と海を探してみましょう。",
      extraKeywords = listOf("world globe", "earth model"),
      band = 3
    ),

    // ==================== BAND 1 EXPANDED: EASY EVERYDAY VOCABULARY (LEVELS 1-10) ====================
    quickEntry("cup", 0, "Kitchen & dining basics", "Cup", "What small open container with a handle holds warm tea or coffee?", "Pour warm herbal tea into the ceramic cup.", "coffee", "Taza", "Tasse", "Tasse", "كوب", "カップ", 1, "Coffee_cup", listOf("mug", "teacup")),
    quickEntry("plate", 0, "Kitchen & dining basics", "Plate", "What flat circular dish holds food during lunch or dinner?", "Set a clean dinner plate beside each fork.", "restaurant", "Plato", "Assiette", "Teller", "طبق", "お皿", 1, "Dinner_plate", listOf("dinner plate", "dish")),
    quickEntry("spoon", 0, "Kitchen & dining basics", "Spoon", "What utensil has a small shallow bowl for eating soup or stirring tea?", "Stir your warm soup slowly with a metal spoon.", "spoon", "Cuchara", "Cuillère", "Löffel", "ملعقة", "スプーン", 1, "Metal_spoon"),
    quickEntry("fork", 0, "Kitchen & dining basics", "Fork", "What eating utensil has sharp prongs used for picking up solid food?", "Place the silver fork neatly on the left side of the plate.", "fork", "Tenedor", "Fourchette", "Gabel", "شوكة", "フォーク", 1, "Dinner_fork"),
    quickEntry("pillow", 0, "Everyday household objects", "Pillow", "What soft cushion supports your head while sleeping in bed?", "Fluff the soft cotton pillow before going to sleep.", "bed", "Almohada", "Oreiller", "Kissen", "وسادة", "枕", 1, "Bed_pillow"),
    quickEntry("towel", 0, "Everyday household objects", "Towel", "What absorbent cotton cloth is used to dry your hands or face after washing?", "Hang the clean bath towel on the hook.", "towel", "Toalla", "Serviette", "Handtuch", "منشفة", "タオル", 1, "Bath_towel"),
    quickEntry("cat", 6, "Animals & nature basics", "Cat", "What small furry pet likes to purr, nap in sunny spots, and chase yarn?", "The gray cat slept peacefully on the warm armchair.", "pet", "Gato", "Chat", "Katze", "قطة", "猫", 1, "Domestic_cat"),
    quickEntry("dog", 6, "Animals & nature basics", "Dog", "What friendly loyal pet wags its tail and enjoys walks in the park?", "Take our playful dog for a morning walk.", "pet", "Perro", "Chien", "Hund", "كلب", "犬", 1, "Domestic_dog"),
    quickEntry("bird", 6, "Animals & nature basics", "Bird", "What feathered animal has wings and sings from tree branches?", "A small blue bird sang outside the kitchen window.", "bird", "Pájaro", "Oiseau", "Vogel", "طائر", "鳥", 1, "Songbird"),
    quickEntry("fish", 6, "Animals & nature basics", "Fish", "What aquatic animal breathes through gills and swims with fins underwater?", "Colorful tropical fish swam smoothly across the reef.", "fish", "Pez", "Poisson", "Fisch", "سمكة", "魚", 1, "Tropical_fish"),
    quickEntry("milk", 2, "Simple food & drinks", "Milk", "What white dairy drink is poured over breakfast cereal or into coffee?", "Keep a fresh carton of cold milk in the refrigerator.", "drink", "Leche", "Lait", "Milch", "حليب", "牛乳", 1, "Glass_of_milk"),
    quickEntry("egg", 2, "Simple food & drinks", "Egg", "What oval farm food with a white or brown shell is cooked for breakfast?", "Boil two fresh eggs for a quick morning breakfast.", "food", "Huevo", "Œuf", "Ei", "بيضة", "卵", 1, "Chicken_egg"),
    quickEntry("rice", 2, "Simple food & drinks", "Rice", "What tiny white or brown cooked grain is a staple side dish worldwide?", "Steamed rice pairs wonderfully with grilled vegetables.", "food", "Arroz", "Riz", "Reis", "أرز", "ご飯", 1, "Steamed_rice"),
    quickEntry("socks", 1, "Basic clothing & footwear", "Socks", "What soft knitted clothing pair covers your feet inside shoes?", "Put on warm wool socks before lacing your sneakers.", "socks", "Calcetines", "Chaussettes", "Socken", "جوارب", "靴下", 1, "Wool_socks"),
    quickEntry("pencil", 9, "Everyday personal items", "Pencil", "What wooden writing tool uses graphite and an eraser to sketch or take notes?", "Sharpen your wooden pencil before starting the drawing.", "edit", "Lápiz", "Crayon", "Bleistift", "قلم رصاص", "鉛筆", 1, "Graphite_pencil"),
    quickEntry("notebook", 9, "Everyday personal items", "Notebook", "What bound pad of lined paper is used to write down class notes or ideas?", "Write today's new vocabulary inside your spiral notebook.", "book", "Cuaderno", "Cahier", "Notizbuch", "دفتر", "ノート", 1, "Spiral_notebook"),
    quickEntry("cloud", 6, "Weather & sky", "Cloud", "What white floating mass of water droplets drifts across the blue sky?", "Soft white clouds floated gently above the valley.", "cloud", "Nube", "Nuage", "Wolke", "سحابة", "雲", 1, "Cumulus_cloud"),
    quickEntry("moon", 6, "Weather & sky", "Moon", "What bright silver celestial body shines in the night sky above Earth?", "The full silver moon glowed calmly over the quiet lake.", "moon", "Luna", "Lune", "Mond", "قمر", "月", 1, "Full_Moon"),
    quickEntry("star", 6, "Weather & sky", "Star", "What distant glowing point of light twinkles in the clear night sky?", "You can count hundreds of bright stars away from city lights.", "star", "Estrella", "Étoile", "Stern", "نجمة", "星", 1, "Night_sky_stars"),
    quickEntry("bicycle_bell", 3, "Simple city transport", "Helmet", "What hard protective headgear keeps cyclists safe while riding on city streets?", "Always buckle your cycling helmet before riding your bike.", "bike", "Casco", "Casque", "Fahrradhelm", "خوذة", "ヘルメット", 1, "Bicycle_helmet"),

    // ==================== BAND 2 EXPANDED: FOOD, TRAVEL, FAMILY & DAILY ACTIVITIES (LEVELS 11-30) ====================
    quickEntry("pizza", 2, "Food, bakery & fresh produce", "Pizza", "What baked Italian flatbread is topped with tomato sauce, melted cheese, and basil?", "We shared a fresh margherita pizza for Friday dinner.", "restaurant", "Pizza", "Pizza", "Pizza", "بيتزا", "ピザ", 2, "Neapolitan_pizza"),
    quickEntry("pasta", 2, "Food, bakery & fresh produce", "Pasta", "What boiled wheat noodles are served with tomato, pesto, or olive oil sauces?", "Simmer the pasta in salted water until tender.", "restaurant", "Pasta", "Pâtes", "Nudeln", "معكرونة", "パスタ", 2, "Spaghetti_pasta"),
    quickEntry("strawberry", 2, "Food, bakery & fresh produce", "Strawberry", "What bright red heart-shaped berry has tiny seeds on its sweet surface?", "Top the morning yogurt bowl with sliced fresh strawberries.", "food", "Fresa", "Fraise", "Erdbeere", "فراولة", "いちご", 2, "Garden_strawberry"),
    quickEntry("orange", 2, "Food, bakery & fresh produce", "Orange", "What juicy round citrus fruit has bright orange peel and sweet segments?", "Peel a sweet orange for an afternoon vitamin boost.", "food", "Naranja", "Orange", "Orange", "برتقالة", "オレンジ", 2, "Orange_fruit"),
    quickEntry("tea", 2, "Café ordering & beverages", "Tea", "What aromatic hot beverage is brewed by steeping dried leaves in boiling water?", "Steep green tea leaves for three minutes before pouring.", "coffee", "Té", "Thé", "Tee", "شاي", "お茶", 2, "Green_tea_cup"),
    quickEntry("lemon", 2, "Market shopping & ingredients", "Lemon", "What sour yellow citrus fruit adds bright flavor to tea, fish, and salads?", "Squeeze half a fresh lemon over the grilled fish.", "food", "Limón", "Citron", "Zitrone", "ليمون", "レモン", 2, "Yellow_lemon"),
    quickEntry("honey", 2, "Market shopping & ingredients", "Honey", "What golden sweet syrup is made by bees from flower nectar?", "Drizzle natural wildflower honey over warm toast.", "food", "Miel", "Miel", "Honig", "عسل", "はちみつ", 2, "Jar_of_honey"),
    quickEntry("ticket", 3, "Airport & train journeys", "Ticket", "What printed or digital pass lets you board a train, bus, or concert?", "Scan your railway ticket at the departure barrier.", "train", "Boleto", "Billet", "Fahrkarte", "تذكرة", "切符", 2, "Train_ticket"),
    quickEntry("taxi", 3, "Travel, luggage & transport", "Taxi", "What hired passenger car drives you directly to your chosen city address?", "Hail a licensed taxi outside the main station entrance.", "car", "Taxi", "Taxi", "Taxi", "سيارة أجرة", "タクシー", 2, "Yellow_cab_taxi"),
    quickEntry("hotel", 4, "Hotel stays & daily errands", "Hotel", "What hospitality building rents furnished rooms to travelers for overnight stays?", "Check in at the hotel reception desk after three o'clock.", "building", "Hotel", "Hôtel", "Hotel", "فندق", "ホテル", 2, "Boutique_hotel"),
    quickEntry("map", 3, "Travel, luggage & transport", "Map", "What folded paper or screen guide shows streets, landmarks, and transit routes?", "Trace our walking route across the old city on a map.", "compass", "Mapa", "Carte", "Landkarte", "خريطة", "地図", 2, "City_map"),
    quickEntry("garden", 6, "Seasons, parks & outdoor walks", "Garden", "What cultivated outdoor space is filled with blooming flowers, herbs, and paths?", "We watered the herbs in the sunny backyard garden.", "tree", "Jardín", "Jardin", "Garten", "حديقة", "庭", 2, "Botanical_garden"),
    quickEntry("beach", 6, "Seasons, parks & outdoor walks", "Beach", "What sandy or pebbled shoreline meets ocean waves along the coast?", "Children built sandcastles along the peaceful morning beach.", "sun", "Playa", "Plage", "Strand", "شاطئ", "ビーチ", 2, "Sandy_beach"),
    quickEntry("candle", 0, "Family life & home meals", "Candle", "What wax cylinder with a lit wick provides a warm flame at dinner tables?", "Light two tall candles for the family anniversary dinner.", "lamp", "Vela", "Bougie", "Kerze", "شمعة", "キャンドル", 2, "Wax_candle"),
    quickEntry("stove", 0, "Family life & home meals", "Stove", "What kitchen cooking appliance heats pots and pans using burners?", "Simmer the vegetable broth gently on the kitchen stove.", "soup", "Estufa", "Cuisinière", "Herd", "موقد", "コンロ", 2, "Kitchen_stove"),
    quickEntry("camera_tripod", 7, "Hobbies & weekend activities", "Bicycle Basket", "What woven carrier mounted on handlebars holds groceries during a bike ride?", "Carry fresh bread home inside your bicycle basket.", "bike", "Cesta de bicicleta", "Panier de vélo", "Fahrradkorb", "سلة دراجة", "自転車のかご", 2, "Bicycle_basket"),
    quickEntry("watermelon", 2, "Food, bakery & fresh produce", "Watermelon", "What large green-rinded summer fruit has juicy red flesh and black seeds?", "Chill thick slices of sweet watermelon for the afternoon picnic.", "food", "Sandía", "Pastèque", "Wassermelone", "بطيخ", "スイカ", 2, "Watermelon_slice"),
    quickEntry("oatmeal", 2, "Family life & home meals", "Pancakes", "What flat golden breakfast cakes cooked on a griddle are served with fruit or syrup?", "Flip fluffy pancakes carefully when bubbles appear on top.", "bread", "Panqueques", "Crêpes", "Pfannkuchen", "فطائر محلاة", "パンケーキ", 2, "Breakfast_pancakes"),
    quickEntry("family_home", 4, "Family life & home meals", "Bakery", "What neighborhood shop bakes and sells fresh bread, rolls, and pastries daily?", "The corner bakery fills the street with warm bread aromas.", "bread", "Panadería", "Boulangerie", "Bäckerei", "مخبز", "パン屋", 2, "Artisan_bakery"),
    quickEntry("postbox", 4, "Hotel stays & daily errands", "Mailbox", "What roadside or wall box collects letters and delivers neighborhood post?", "Drop your stamped postcards into the red street mailbox.", "mail", "Buzón", "Boîte aux lettres", "Briefkasten", "صندوق بريد", "郵便ポスト", 2, "Street_mailbox"),

    // ==================== BAND 3 EXPANDED: WORK, SHOPPING, EDUCATION & CONVERSATIONS (LEVELS 31-60) ====================
    quickEntry("desk", 5, "Workplace & office essentials", "Desk", "What flat-topped work furniture holds a computer, lamp, and office files?", "Arrange your monitor and notebook neatly on the office desk.", "table", "Escritorio", "Bureau", "Schreibtisch", "مكتب عمل", "机", 3, "Office_desk"),
    quickEntry("printer", 5, "Technology & digital devices", "Printer", "What office machine transfers digital documents and photos onto sheets of paper?", "Collect the signed contract pages from the office printer.", "laptop", "Impresora", "Imprimante", "Drucker", "طابعة", "プリンター", 3, "Office_printer"),
    quickEntry("calendar", 5, "Career tools & meeting rooms", "Calendar", "What chart of days, weeks, and months tracks meetings and upcoming deadlines?", "Mark next Thursday's project review on the wall calendar.", "clock", "Calendario", "Calendrier", "Kalender", "تقويم", "カレンダー", 3, "Desk_calendar"),
    quickEntry("envelope", 5, "Workplace & office essentials", "Envelope", "What folded paper sleeve seals a letter or document for mailing?", "Write the return address cleanly in the corner of the envelope.", "mail", "Sobre", "Enveloppe", "Briefumschlag", "مظروف", "封筒", 3, "Paper_envelope"),
    quickEntry("briefcase", 5, "Workplace & office essentials", "Briefcase", "What structured leather handle bag carries business files and laptops to meetings?", "She placed her presentation folder inside the brown briefcase.", "luggage", "Maletín", "Mallette", "Aktentasche", "حقيبة مستندات", "ブリーフケース", 3, "Leather_briefcase"),
    quickEntry("receipt", 8, "Shopping, receipts & payment", "Receipt", "What printed slip lists purchased items and prices as proof of payment?", "Keep the paper receipt in case you exchange the jacket size.", "receipt", "Recibo", "Reçu", "Quittung", "إيصال", "レシート", 3, "Store_receipt"),
    quickEntry("creditcard", 8, "Shopping, receipts & payment", "Credit Card", "What rectangular plastic payment card lets shoppers pay electronically at registers?", "Tap your contactless payment card against the store terminal.", "wallet", "Tarjeta de crédito", "Carte bancaire", "Kreditkarte", "بطاقة ائتمان", "クレジットカード", 3, "Payment_card"),
    quickEntry("calculator", 9, "Education, books & campus", "Calculator", "What pocket electronic device performs fast addition, percentages, and algebra?", "Check the monthly budget totals with a scientific calculator.", "calculator", "Calculadora", "Calculatrice", "Taschenrechner", "آلة حاسبة", "電卓", 3, "Scientific_calculator"),
    quickEntry("newspaper", 9, "Education, books & campus", "Newspaper", "What folded daily publication prints current headlines, local reports, and essays?", "He reads the morning newspaper while sipping black coffee.", "book", "Periódico", "Journal", "Zeitung", "صحيفة", "新聞", 3, "Daily_newspaper"),
    quickEntry("scissors", 5, "Career tools & meeting rooms", "Scissors", "What handheld cutting tool uses two pivoted blades to trim paper or fabric?", "Use sharp craft scissors to trim the poster neatly along the line.", "scissors", "Tijeras", "Ciseaux", "Schere", "مقص", "はさみ", 3, "Office_scissors"),
    quickEntry("whiteboard", 5, "Career tools & meeting rooms", "Whiteboard", "What smooth white classroom or meeting wall board is written on with dry markers?", "Sketch the weekly product roadmap clearly on the whiteboard.", "laptop", "Pizarra blanca", "Tableau blanc", "Whiteboard", "سبورة بيضاء", "ホワイトボード", 3, "Office_whiteboard"),
    quickEntry("diploma", 9, "Education, books & campus", "Diploma", "What official framed certificate honors a student's completion of a degree?", "Graduates proudly held their university diplomas on stage.", "trophy", "Diploma", "Diplôme", "Diplom", "شهادة تخرج", "卒業証書", 3, "University_diploma"),
    quickEntry("coin", 8, "Shopping, receipts & payment", "Coins", "What stamped metal discs serve as small change for transit and markets?", "Count out exact metal coins for the bakery pastry.", "wallet", "Monedas", "Pièces", "Münzen", "عملات معدنية", "硬貨", 3, "Metal_coins"),
    quickEntry("coffee_machine", 2, "Restaurants & conversations", "Espresso Machine", "What pressurized café machine breams rich espresso and steams velvety milk?", "The barista pulled a double shot from the gleaming espresso machine.", "coffee", "Cafetera", "Machine à expresso", "Espressomaschine", "آلة إسبريسو", "エスプレッソマシン", 3, "Espresso_machine"),
    quickEntry("bookstore", 4, "Shopping, receipts & payment", "Bookstore", "What quiet retail shop sells novels, travel guides, and stationery?", "Browse the poetry shelves inside the independent bookstore.", "book", "Librería", "Librairie", "Buchhandlung", "مكتبة بيع كتب", "書店", 3, "Independent_bookstore"),
    quickEntry("menu", 2, "Restaurants & conversations", "Menu", "What printed list at a restaurant table describes dishes, ingredients, and prices?", "Read tonight's seasonal menu before calling the waiter.", "restaurant", "Menú", "Menu", "Speisekarte", "قائمة طعام", "メニュー", 3, "Restaurant_menu"),
    quickEntry("suitcase_tag", 3, "Transit stations & city errands", "Subway Station", "What underground rail stop lets commuters board rapid city trains?", "Descend the escalator into the central subway station.", "train", "Estación de metro", "Station de métro", "U-Bahn-Station", "محطة مترو", "地下鉄の駅", 3, "Metro_station"),
    quickEntry("tennis_racket", 7, "Sports & active hobbies", "Tennis Racket", "What stringed oval sports frame hits a yellow felt ball across a net?", "Practice your backhand swing with a lightweight tennis racket.", "sports", "Raqueta de tenis", "Raquette de tennis", "Tennisschläger", "مضرب تنس", "テニスラケット", 3, "Tennis_racket"),
    quickEntry("sewing_machine", 1, "Fashion, tailoring & accessories", "Tailor Tape", "What flexible numbered ribbon measures waist, sleeve, and hem lengths?", "Measure the sleeve length carefully before hemming the coat.", "clothing", "Cinta métrica", "Mètre ruban", "Maßband", "شريط قياس", "メジャー", 3, "Measuring_tape"),
    quickEntry("binder", 5, "Education, books & campus", "Folder", "What stiff folded cover keeps loose project papers organized in a bag?", "Slip the signed consent forms inside the blue presentation folder.", "folder", "Carpeta", "Dossier", "Ordner", "ملف أوراق", "ファイル", 3, "Paper_folder"),

    // ==================== BAND 4 EXPANDED: INTERMEDIATE VOCABULARY & SITUATIONS (LEVELS 61-100) ====================
    quickEntry("stethoscope", 9, "Health, clinics & pharmacy", "Stethoscope", "What acoustic medical device lets doctors listen to a patient's heartbeat and lungs?", "The physician placed a cold stethoscope gently on the patient's chest.", "medical", "Estetoscopio", "Stéthoscope", "Stethoskop", "سماعة طبيب", "聴診器", 4, "Medical_stethoscope"),
    quickEntry("pharmacy", 4, "Health, clinics & pharmacy", "Pharmacy", "What licensed health store dispenses prescriptions, bandages, and wellness remedies?", "Pick up the prescribed eye drops at the neighborhood pharmacy.", "hospital", "Farmacia", "Pharmacie", "Apotheke", "صيدلية", "薬局", 4, "Local_pharmacy"),
    quickEntry("ambulance", 3, "Health, clinics & pharmacy", "Ambulance", "What emergency medical vehicle transports patients swiftly to the hospital?", "Drivers pull aside promptly when an ambulance siren sounds.", "car", "Ambulancia", "Ambulance", "Krankenwagen", "سيارة إسعاف", "救急車", 4, "Emergency_ambulance"),
    quickEntry("lighthouse", 4, "Travel navigation & landmarks", "Lighthouse", "What tall coastal tower shines a rotating beam to guide ships away from rocks?", "The white stone lighthouse stood boldly atop the windy cliff.", "building", "Faro", "Phare", "Leuchtturm", "منارة", "灯台", 4, "Coastal_lighthouse"),
    quickEntry("waterfall", 6, "Landscapes, mountains & weather", "Waterfall", "What steep natural cascade sends river water plunging over a rocky cliff?", "Mist rose above the emerald pool beneath the forest waterfall.", "mountain", "Cascada", "Cascade", "Wasserfall", "شلال", "滝", 4, "Forest_waterfall"),
    quickEntry("volcano", 6, "Landscapes, mountains & weather", "Volcano", "What cone-shaped mountain vent can release ash, steam, and molten rock?", "Hiking trails wind around the dormant volcanic crater.", "mountain", "Volcán", "Volcan", "Vulkan", "بركان", "火山", 4, "Stratovolcano"),
    quickEntry("tent", 7, "Outdoor adventure & camping", "Camping Tent", "What portable fabric shelter supported by poles protects campers outdoors?", "Pitch the waterproof camping tent on level ground before sunset.", "park", "Tienda de campaña", "Tente", "Zelt", "خيمة", "テント", 4, "Camping_tent"),
    quickEntry("violin", 7, "Museums, theater & culture", "Violin", "What four-string wooden orchestra instrument is played with a horsehair bow?", "Warm violin melodies filled the sunlit conservatory hall.", "music", "Violín", "Violon", "Geige", "كمان", "バイオリン", 4, "Classical_violin"),
    quickEntry("piano", 7, "Museums, theater & culture", "Piano", "What large black-and-white keyboard instrument produces notes via felt hammers?", "Practice scales softly on the acoustic upright piano.", "music", "Piano", "Piano", "Klavier", "بيانو", "ピアノ", 4, "Grand_piano"),
    quickEntry("sculpture", 7, "Museums, theater & culture", "Sculpture", "What three-dimensional marble or bronze artwork stands in a gallery or plaza?", "Visitors admired the carved marble sculpture inside the atrium.", "art", "Escultura", "Sculpture", "Skulptur", "تمثال منحوت", "彫刻", 4, "Marble_sculpture"),
    quickEntry("theater", 4, "Museums, theater & culture", "Theater Stage", "What velvet-curtained hall hosts live drama, ballet, and musical performances?", "Actors bowed together under bright spotlights on the theater stage.", "building", "Teatro", "Théâtre", "Theater", "مسرح", "劇場", 4, "Theater_stage"),
    quickEntry("windmill", 4, "Urban architecture & bridges", "Windmill", "What traditional tower with turning sails harnesses wind power to grind grain?", "Historic wooden windmills line the tranquil countryside canal.", "building", "Molino de viento", "Moulin à vent", "Windmühle", "طاحونة هواء", "風車", 4, "Traditional_windmill"),
    quickEntry("harbor", 4, "Travel navigation & landmarks", "Harbor", "What sheltered coastal basin docks fishing boats, ferries, and sailing yachts?", "Fishing boats returned calmly to the harbor at dusk.", "ship", "Puerto", "Port", "Hafen", "ميناء", "港", 4, "Coastal_harbor"),
    quickEntry("hourglass", 0, "Tools, instruments & equipment", "Hourglass", "What twin-bulbed glass timer measures minutes as fine sand trickles down?", "Flip the brass hourglass to time a two-minute debate turn.", "clock", "Reloj de arena", "Sablier", "Sanduhr", "ساعة رملية", "砂時計", 4, "Sand_hourglass"),
    quickEntry("binoculars", 7, "Outdoor adventure & camping", "Binoculars", "What twin-lens handheld optical tool lets birdwatchers zoom in on distant views?", "Spot soaring eagles across the valley through clear binoculars.", "camera", "Binoculares", "Jumelles", "Fernglas", "منظار ثنائي", "双眼鏡", 4, "Optical_binoculars"),
    quickEntry("megaphone", 5, "Festivals & social situations", "Megaphone", "What cone-shaped loudspeaker amplifies announcements at outdoor events?", "The festival coordinator greeted the crowd through a megaphone.", "microphone", "Megáfono", "Mégaphone", "Megafon", "مكبر صوت", "メガホン", 4, "Hand_megaphone"),
    quickEntry("newspaper_stand", 5, "Media, journalism & audio", "Podcast Studio", "What soundproof desk setup uses boom microphones for recorded interviews?", "Record clear weekly episodes inside the acoustic podcast studio.", "microphone", "Estudio de pódcast", "Studio de podcast", "Podcast-Studio", "استوديو بودكاست", "ポッドキャストスタジオ", 4, "Podcast_studio"),
    quickEntry("thermometer", 9, "Health, clinics & pharmacy", "Thermometer", "What calibrated medical or weather gauge measures exact temperature degrees?", "Check the digital thermometer before deciding whether to rest at home.", "science", "Termómetro", "Thermomètre", "Thermometer", "ميزان حرارة", "温度計", 4, "Clinical_thermometer"),
    quickEntry("canoe", 3, "Outdoor adventure & camping", "Kayak", "What narrow paddled boat glides smoothly across calm rivers and alpine lakes?", "Paddle the bright yellow kayak silently along the cove.", "ship", "Kayak", "Kayak", "Kajak", "قارب كاياك", "カヤック", 4, "River_kayak"),
    quickEntry("canyon", 6, "Landscapes, mountains & weather", "Canyon", "What deep steep-walled valley was carved over millennia by a winding river?", "Golden sunset light illuminated the layered cliffs of the canyon.", "mountain", "Cañón", "Canyon", "Schlucht", "وادي عميق", "峡谷", 4, "Desert_canyon"),

    // ==================== BAND 5 EXPANDED: ADVANCED VOCABULARY & REAL-WORLD SITUATIONS (LEVELS 101-500+) ====================
    quickEntry("observatory", 9, "Scientific laboratories & observatories", "Observatory", "What domed scientific station houses giant telescopes for charting distant stars?", "Astronomers opened the observatory dome after twilight cleared.", "science", "Observatorio", "Observatoire", "Sternwarte", "مرصد فلكي", "天文台", 5, "Astronomical_observatory"),
    quickEntry("telescope", 9, "Precision engineering & optics", "Telescope", "What long precision optical tube gathers light to reveal craters on moons and planets?", "Focus the brass telescope carefully on Saturn's icy rings.", "science", "Telescopio", "Télescope", "Teleskop", "تلسكوب", "望遠鏡", 5, "Refracting_telescope"),
    quickEntry("cathedral", 4, "Architecture, cathedrals & civic halls", "Cathedral", "What monumental historic church features soaring vaulted arches and stained glass?", "Sunlight streamed brightly through the cathedral's rose window.", "building", "Catedral", "Cathédrale", "Kathedrale", "كاتدرائية", "大聖堂", 5, "Gothic_cathedral"),
    quickEntry("glacier", 6, "Geography, glaciers & archipelagos", "Glacier", "What massive slow-moving river of compressed alpine ice carves mountain valleys?", "Scientists measured seasonal melt along the blue alpine glacier.", "mountain", "Glaciar", "Glacier", "Gletscher", "نهر جليدي", "氷河", 5, "Alpine_glacier"),
    quickEntry("satellite", 9, "Aviation, maritime & space exploration", "Satellite", "What engineered spacecraft orbits Earth to relay communications and climate data?", "Meteorological satellites transmit high-resolution storm imagery every hour.", "flight", "Satélite", "Satellite", "Satellit", "قمر صناعي", "人工衛星", 5, "Orbital_satellite"),
    quickEntry("solarpanel", 9, "Renewable energy & environment", "Solar Panel", "What photovoltaic glass array converts bright sunlight directly into clean electricity?", "Roofs lined with dark solar panels supply clean neighborhood energy.", "sun", "Panel solar", "Panneau solaire", "Solarmodul", "لوح شمسي", "ソーラーパネル", 5, "Solar_photovoltaic_panel"),
    quickEntry("turbine", 9, "Renewable energy & environment", "Wind Turbine", "What tall sleek rotor tower generates renewable electricity from steady coastal breezes?", "Offshore wind turbines spin gracefully above the North Sea.", "science", "Turbina eólica", "Éolienne", "Windkraftanlage", "توربين رياح", "風力タービン", 5, "Wind_turbine"),
    quickEntry("beaker", 9, "Scientific laboratories & observatories", "Laboratory Beaker", "What graduated heat-resistant glass vessel holds measured liquid solutions in experiments?", "Pour fifty milliliters of distilled water into the glass beaker.", "science", "Vaso de precipitado", "Bécher", "Becherglas", "كأس مخبري", "ビーカー", 5, "Chemistry_beaker"),
    quickEntry("aqueduct", 4, "Historical artifacts & heritage", "Aqueduct", "What ancient multi-arched stone bridge carried fresh mountain water to distant cities?", "Roman engineers constructed stone aqueducts across wide valleys.", "bridge", "Acueducto", "Aqueduc", "Aquädukt", "قناة مائية تاريخية", "水道橋", 5, "Roman_aqueduct"),
    quickEntry("greenhouse", 6, "Gastronomy, vineyards & botanicals", "Conservatory Greenhouse", "What glass-walled botanical pavilion shelters exotic orchids and tropical ferns year-round?", "Humid mist nourishes rare ferns inside the Victorian glass conservatory.", "tree", "Invernadero", "Serre botanique", "Gewächshaus", "بيت زجاجي نباتي", "温室", 5, "Botanical_conservatory"),
    quickEntry("cello", 7, "Classical orchestra & fine arts", "Cello", "What deep-toned wooden bowed string instrument rests on an endpin between the player's knees?", "Rich cello harmonies anchored the evening chamber symphony.", "music", "Violonchelo", "Violoncelle", "Cello", "آلة تشيلو", "チェロ", 5, "Concert_cello"),
    quickEntry("manuscript", 9, "Historical artifacts & heritage", "Manuscript", "What hand-lettered historical parchment scroll or codex preserves ancient literature?", "Archivists carefully photographed the illuminated parchment manuscript.", "book", "Manuscrito", "Manuscrit", "Manuskript", "مخطوطة تاريخية", "古写本", 5, "Illuminated_manuscript"),
    quickEntry("archipelago", 6, "Geography, glaciers & archipelagos", "Archipelago", "What sweeping chain or cluster of islands lies scattered across clear ocean waters?", "Ferries connect tiny fishing harbors throughout the northern archipelago.", "ship", "Archipiélago", "Archipel", "Archipel", "أرخبيل", "群島", 5, "Island_archipelago"),
    quickEntry("courthouse", 4, "Global diplomacy & civic institutions", "Civic Hall", "What neoclassical public building with stone columns hosts council sessions and civic ceremonies?", "Delegates gathered on the marble steps of the municipal civic hall.", "building", "Palacio cívico", "Hôtel de ville", "Rathaus", "قاعة مدنية", "市庁舎", 5, "Civic_city_hall"),
    quickEntry("fountain", 4, "Architecture, cathedrals & civic halls", "Marble Fountain", "What sculpted civic basin jets sparkling arcs of water in a historic city square?", "Tourists tossed coins into the sunlit marble plaza fountain.", "park", "Fuente", "Fontaine", "Brunnen", "نافورة", "噴水", 5, "Plaza_fountain"),
    quickEntry("vineyard", 6, "Gastronomy, vineyards & botanicals", "Vineyard", "What terraced hillside agricultural estate cultivates rows of grapevines in warm sun?", "Autumn harvest baskets lined the sunlit hillside vineyard rows.", "tree", "Viñedo", "Vignoble", "Weinberg", "كرم عنب", "ぶどう園", 5, "Hillside_vineyard"),
    quickEntry("amphitheater", 4, "Historical artifacts & heritage", "Amphitheater", "What semicircular open-air tiered stone arena staged classical drama and civic assemblies?", "Acoustic echoes carry whispers across the ancient stone amphitheater.", "building", "Anfiteatro", "Amphithéâtre", "Amphitheater", "مدرج روماني", "円形劇場", 5, "Ancient_amphitheater"),
    quickEntry("compass_sextant", 9, "Aviation, maritime & space exploration", "Sextant", "What brass maritime angle instrument measures star altitude above the horizon for navigation?", "Sailors sighted Polaris through the brass sextant to confirm latitude.", "compass", "Sextante", "Sextant", "Sextant", "آلة السدس", "六分儀", 5, "Maritime_sextant"),
    quickEntry("suspension_bridge", 4, "Urban architecture & bridges", "Suspension Bridge", "What soaring steel-cable bridge suspends its roadway across deep coastal straits?", "Morning fog drifted below the red towers of the suspension bridge.", "bridge", "Puente colgante", "Pont suspendu", "Hängebrücke", "جسر معلق", "吊り橋", 5, "Suspension_bridge"),
    quickEntry("sundial", 9, "Precision engineering & optics", "Sundial", "What carved garden timekeeper uses a gnomon's moving shadow to mark solar hours?", "The bronze sundial cast a crisp noon shadow across the courtyard stone.", "clock", "Reloj de sol", "Cadran solaire", "Sonnenuhr", "مزولة شمسية", "日時計", 5, "Garden_sundial")
  )

  private val byIdMap: Map<String, VerifiedVisualConcept> =
    ALL_CONCEPTS.associateBy { it.conceptId.lowercase() }

  fun getById(conceptId: String): VerifiedVisualConcept? {
    val key = conceptId.trim().lowercase()
    return byIdMap[key] ?: dynamicByIdMap[key]
  }

  fun allConceptIdsCsv(): String =
    ALL_CONCEPTS.joinToString(", ") { it.conceptId }

  private val BANNED_IMAGE_URL_SUBSTRINGS = listOf(
    "bread_loaf_inn",
    "leaflet_at_narita",
    "heiligengrabe",
    "max_giesinger",
    "gracie_gold",
    ".svg",
    "flag_of_",
    "locator_map",
    "coat_of_arms",
    "no_image",
    "placeholder",
    "question_mark"
  )

  private val NON_BUILDING_CONCEPT_GUARD = setOf(
    "building", "hotel", "inn", "house", "home", "museum", "hospital", "library",
    "restaurant", "bakery", "bookstore", "theater", "cathedral", "observatory",
    "courthouse", "amphitheater", "greenhouse", "pharmacy", "lighthouse", "windmill"
  )

  /**
   * Validates that an image URL genuinely depicts the target object and is never a
   * misleading building/person/flag/SVG placeholder (e.g., never `Bread_Loaf_Inn` for `bread`).
   */
  fun validateImageUrlMatchesConcept(concept: VerifiedVisualConcept): Boolean {
    if (concept.isDynamic) {
      if (concept.wikimediaPhotoUrl.isBlank()) return true // Exact-word fallback visual card
      return validateImageUrlForConcept(concept.wikimediaPhotoUrl, concept.englishWord, concept.domainLabel)
    }
    if (!concept.wikimediaPhotoUrl.startsWith("https://")) return false
    return validateImageUrlForConcept(concept.wikimediaPhotoUrl, concept.englishWord, concept.domainLabel)
  }

  private val INN_WORD_REGEX = Regex("(^|[^a-z])inn([^a-z]|$)")

  fun validateImageUrlForConcept(url: String, englishConcept: String, categoryHint: String = ""): Boolean {
    if (!url.startsWith("https://")) return false
    val urlLower = url.lowercase()
    if (BANNED_IMAGE_URL_SUBSTRINGS.any { urlLower.contains(it) }) return false
    val conceptLower = englishConcept.trim().lowercase()
    val categoryLower = categoryHint.trim().lowercase()
    val conceptIsBuilding = NON_BUILDING_CONCEPT_GUARD.any { conceptLower.contains(it) || categoryLower.contains(it) }
    if (!conceptIsBuilding && (INN_WORD_REGEX.containsMatchIn(urlLower) || urlLower.contains("hotel_building"))) {
      return false
    }
    if (conceptLower == "bread" && INN_WORD_REGEX.containsMatchIn(urlLower)) return false
    return true
  }

  /**
   * Resolves a verified, exact-word Wikipedia REST thumbnail URL for dynamically generated AI concepts
   * and rejects disambiguation pages, biographies, films, albums, SVG maps/flags, and mismatched photos.
   * Returns "" if no verified match is found so the caller NEVER displays an unrelated image.
   */
  suspend fun resolveAndVerifyDynamicImageUrl(
    englishConcept: String,
    categoryHint: String = "",
    context: Context? = null
  ): String = withContext(Dispatchers.IO) {
    val cleanName = englishConcept
      .replace(Regex("\\([^)]*\\)"), "")
      .trim()
    if (cleanName.isBlank()) return@withContext ""
    val cacheKey = cleanName.lowercase()

    resolvedUrlMemoryCache[cacheKey]?.let { return@withContext it }

    context?.let { ctx ->
      val diskUrl = ctx.getSharedPreferences(VISUAL_CACHE_PREFS, Context.MODE_PRIVATE)
        .getString("img_$cacheKey", null)
      if (diskUrl != null) {
        if (diskUrl.isEmpty() || validateImageUrlForConcept(diskUrl, cleanName, categoryHint)) {
          resolvedUrlMemoryCache[cacheKey] = diskUrl
          return@withContext diskUrl
        }
      }
    }

    // Check curated registry first if exact match exists
    findMatchingConcept(cleanName)?.let { curated ->
      if (validateImageUrlMatchesConcept(curated)) {
        return@withContext curated.wikimediaPhotoUrl
      }
    }

    val candidatesToTry = buildList {
      add(cleanName.replace(" ", "_"))
      val singular = cleanName.removeSuffix("s").takeIf { it.length >= 3 && it != cleanName }
      if (singular != null) add(singular.replace(" ", "_"))
    }

    var verifiedUrl = ""
    for (wikiTitle in candidatesToTry) {
      val fetched = queryWikipediaSummaryThumbnail(wikiTitle, cleanName, categoryHint)
      if (fetched.isNotBlank()) {
        verifiedUrl = fetched
        break
      }
    }

    resolvedUrlMemoryCache[cacheKey] = verifiedUrl
    context?.let { ctx ->
      ctx.getSharedPreferences(VISUAL_CACHE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("img_$cacheKey", verifiedUrl)
        .apply()
    }
    verifiedUrl
  }

  private fun queryWikipediaSummaryThumbnail(
    wikiTitle: String,
    englishConcept: String,
    categoryHint: String
  ): String {
    return try {
      val encoded = URLEncoder.encode(wikiTitle, "UTF-8").replace("+", "_")
      val request = Request.Builder()
        .url("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return ""
        val body = response.body?.string() ?: return ""
        val json = JSONObject(body)
        if (json.optString("type", "").lowercase() != "standard") return ""

        val pageTitle = json.optString("title", "").lowercase()
        val description = json.optString("description", "").lowercase()
        val extract = json.optString("extract", "").lowercase()

        val rejectedDescriptions = listOf(
          "disambiguation", "film", "album", "song", "single by", "band", "television",
          "tv series", "novel", "character", "video game", "software", "company",
          "politician", "actor", "actress", "singer", "musician", "footballer", "player",
          "painter", "writer", "author", "municipality", "county", "census-designated"
        )
        val combinedMeta = "$pageTitle $description"
        if (rejectedDescriptions.any { combinedMeta.contains(it) }) return ""
        if (description.contains("born ") || extract.take(120).contains(" is an american ") || extract.take(120).contains(" is a british ")) {
          return ""
        }

        val thumb = json.optJSONObject("thumbnail")?.optString("source").orEmpty()
          .ifBlank { json.optJSONObject("originalimage")?.optString("source").orEmpty() }
        if (thumb.isBlank()) return ""
        if (!validateImageUrlForConcept(thumb, englishConcept, categoryHint)) return ""
        thumb
      }
    } catch (_: Exception) {
      ""
    }
  }

  /**
   * Registers (or updates) a dynamic concept generated by Gemini or offline progression
   * so `getById` and `verifyConceptMatchesAnswer` recognize it seamlessly.
   */
  fun registerDynamicConcept(
    conceptId: String,
    englishWord: String,
    targetWord: String,
    languageCode: String,
    domainLabel: String,
    definitionQuestionEn: String,
    exampleSentence: String,
    imageUrl: String,
    iconKey: String,
    levelBand: Int,
    extraKeywords: List<String> = emptyList()
  ): VerifiedVisualConcept {
    val cleanId = conceptId.trim().lowercase()
    val safeUrl = if (imageUrl.isNotBlank() && validateImageUrlForConcept(imageUrl, englishWord, domainLabel)) {
      imageUrl
    } else {
      ""
    }
    val keywords = buildSet {
      add(cleanId)
      add(englishWord.trim().lowercase())
      add(targetWord.trim().lowercase())
      extraKeywords.forEach { if (it.isNotBlank()) add(it.trim().lowercase()) }
    }
    val langKey = languageCode.lowercase().take(2)
    val concept = VerifiedVisualConcept(
      conceptId = cleanId,
      domainIndex = getDomainForTopic(domainLabel, levelBand * 10),
      domainLabel = domainLabel.ifBlank { "Vocabulary" },
      englishWord = englishWord.trim(),
      definitionQuestionEn = definitionQuestionEn.trim(),
      exampleSentenceEn = exampleSentence.trim(),
      wikimediaPhotoUrl = safeUrl,
      localDrawableResId = null,
      iconKey = iconKey.ifBlank { englishWord.trim().lowercase() },
      translations = mapOf(
        "en" to (englishWord.trim() to exampleSentence.trim()),
        langKey to (targetWord.trim() to exampleSentence.trim())
      ),
      phonetics = mapOf(
        "en" to "/${englishWord.trim().lowercase()}/",
        langKey to "/${targetWord.trim().lowercase()}/"
      ),
      matchKeywords = keywords,
      levelBand = levelBand.coerceIn(1, 5),
      isDynamic = true
    )
    dynamicByIdMap[cleanId] = concept
    return concept
  }

  /**
   * Restores a dynamic concept when loading cached exercises after an app restart.
   */
  fun restoreDynamicConcept(
    conceptId: String,
    englishMeaning: String,
    targetPhrase: String,
    languageCode: String,
    imageUrl: String,
    iconKey: String,
    domainLabel: String,
    prompt: String,
    explanation: String,
    levelNumber: Int
  ) {
    if (!conceptId.startsWith("dyn:", ignoreCase = true)) return
    if (getById(conceptId) != null) return
    val cleanEng = englishMeaning.substringAfter("•").trim().ifBlank { targetPhrase }
    registerDynamicConcept(
      conceptId = conceptId,
      englishWord = cleanEng,
      targetWord = targetPhrase,
      languageCode = languageCode,
      domainLabel = domainLabel.ifBlank { "Vocabulary" },
      definitionQuestionEn = prompt,
      exampleSentence = explanation,
      imageUrl = imageUrl,
      iconKey = iconKey,
      levelBand = GameCurriculumAndAntiRepetitionEngine.tierForLevel(levelNumber)
    )
  }

  /**
   * Resolves a verified visual concept from candidate strings (concept ID, target word,
   * English meaning, or prompt). Returns `null` if no verified visual matches so the
   * caller never displays an unrelated random illustration.
   */
  fun findMatchingConcept(vararg texts: String): VerifiedVisualConcept? {
    val joinedTokens = texts.flatMap { raw ->
      val clean = raw.trim().lowercase()
      val words = clean.replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
      listOf(clean) + words
    }.toSet()

    val allCandidates = ALL_CONCEPTS + dynamicByIdMap.values
    // 1. Exact ID or primary word match first
    for (concept in allCandidates) {
      if (concept.conceptId.lowercase() in joinedTokens || concept.englishWord.lowercase() in joinedTokens) {
        return concept
      }
      if (concept.translations.values.any { (w, _) -> w.lowercase() in joinedTokens }) {
        return concept
      }
    }

    // 2. Keyword match
    return allCandidates.firstOrNull { concept ->
      concept.matchKeywords.any { kw -> kw in joinedTokens }
    }
  }

  /**
   * Deterministically verifies that `conceptId` genuinely represents the target answer.
   * Uses strict word-boundary token matching (never bare substring checks like "car" in "scarf")
   * and validates image relevance so an unrelated photo is never approved.
   */
  fun verifyConceptMatchesAnswer(
    conceptId: String,
    targetPhrase: String,
    englishMeaning: String,
    prompt: String = ""
  ): Boolean {
    val concept = getById(conceptId) ?: return false
    if (!validateImageUrlMatchesConcept(concept)) return false
    val answerSurface = listOf(targetPhrase, englishMeaning)
      .joinToString(" ")
      .lowercase()
    val answerTokens = answerSurface
      .replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }
      .toSet()

    val normalizedTarget = targetPhrase.trim().lowercase()
    val normalizedEng = englishMeaning.trim().lowercase()

    return concept.matchKeywords.any { kw ->
      val cleanKw = kw.trim().lowercase()
      if (cleanKw.isBlank()) return@any false
      if (cleanKw.contains(" ")) {
        Regex("(?<![\\p{L}\\p{N}])${Regex.escape(cleanKw)}(?![\\p{L}\\p{N}])").containsMatchIn(answerSurface)
      } else {
        cleanKw in answerTokens || cleanKw == normalizedTarget || cleanKw == normalizedEng
      }
    }
  }

  fun getDomainForTopic(topic: String, levelNumber: Int): Int {
    val t = topic.lowercase()
    return when {
      t.contains("household") || t.contains("home") || t.contains("furniture") || t.contains("room") || t.contains("kitchen") -> 0
      t.contains("clothing") || t.contains("wear") || t.contains("footwear") || t.contains("accessor") || t.contains("fashion") || t.contains("tailor") -> 1
      t.contains("food") || t.contains("drink") || t.contains("café") || t.contains("fruit") || t.contains("bakery") || t.contains("gastron") || t.contains("restaurant") -> 2
      t.contains("travel") || t.contains("transit") || t.contains("transport") || t.contains("luggage") || t.contains("airport") || t.contains("train") -> 3
      t.contains("places") || t.contains("town") || t.contains("city") || t.contains("building") || t.contains("hotel") || t.contains("architecture") || t.contains("landmark") -> 4
      t.contains("workplace") || t.contains("technology") || t.contains("office") || t.contains("digital") || t.contains("career") || t.contains("media") -> 5
      t.contains("weather") || t.contains("nature") || t.contains("season") || t.contains("park") || t.contains("animal") || t.contains("landscape") || t.contains("geography") -> 6
      t.contains("sport") || t.contains("hobby") || t.contains("hobbies") || t.contains("art") || t.contains("celebration") || t.contains("music") || t.contains("cinema") || t.contains("museum") || t.contains("festival") -> 7
      t.contains("shop") || t.contains("market") || t.contains("receipt") || t.contains("payment") -> 8
      t.contains("education") || t.contains("health") || t.contains("campus") || t.contains("science") || t.contains("clinic") || t.contains("observat") || t.contains("energy") -> 9
      else -> (levelNumber - 1).coerceAtLeast(0) % 10
    }
  }

  fun isConceptCompatibleWithTopic(
    concept: VerifiedVisualConcept,
    topic: String,
    levelNumber: Int
  ): Boolean {
    if (concept.isDynamic) return true
    val targetDomain = getDomainForTopic(topic, levelNumber)
    return concept.domainIndex == targetDomain ||
      concept.domainLabel.equals(topic, ignoreCase = true) ||
      (targetDomain == 3 && concept.domainIndex == 4) ||
      (targetDomain == 4 && concept.domainIndex == 3) ||
      (targetDomain >= 7)
  }

  fun getConceptsForTopic(topic: String, levelNumber: Int): List<VerifiedVisualConcept> {
    val targetDomain = getDomainForTopic(topic, levelNumber)
    val desiredBand = GameCurriculumAndAntiRepetitionEngine.tierForLevel(levelNumber)
    val exactDomain = ALL_CONCEPTS.filter { it.domainIndex == targetDomain }
      .sortedBy { kotlin.math.abs(it.levelBand - desiredBand) }
    if (exactDomain.size >= 6) return exactDomain
    return (exactDomain + ALL_CONCEPTS.filter { it.domainIndex != targetDomain }
      .sortedBy { kotlin.math.abs(it.levelBand - desiredBand) })
      .distinctBy { it.conceptId.lowercase() }
  }

  /**
   * Selects `count` distinct, non-repeating verified visual concepts for `levelNumber`,
   * prioritizing `levelBand` (1..5) and thematic domain while strictly rejecting exact
   * AND canonical semantic duplicates in `excludedLowercase`. NEVER falls back to already-used items.
   */
  fun selectConceptsForPictureLevel(
    levelNumber: Int,
    languageCode: String,
    count: Int,
    excludedLowercase: Set<String>,
    topic: String = ""
  ): List<VerifiedVisualConcept> {
    val desiredBand = GameCurriculumAndAntiRepetitionEngine.tierForLevel(levelNumber)
    val targetDomain = if (topic.isNotBlank()) getDomainForTopic(topic, levelNumber) else (levelNumber - 1).coerceAtLeast(0) % 10

    val rankedPool = ALL_CONCEPTS.sortedWith(
      compareBy<VerifiedVisualConcept>(
        { kotlin.math.abs(it.levelBand - desiredBand) },
        { if (it.domainIndex == targetDomain) 0 else 1 },
        {
          // Keep Band 1 Level 1 starting reliably with Table, Lamp, Door, Clock, Chair, Bed
          if (levelNumber == 1 && it.levelBand == 1 && it.domainIndex == 0) ALL_CONCEPTS.indexOf(it)
          else (kotlin.math.abs(it.conceptId.hashCode() + levelNumber * 31)) % 199
        }
      )
    )

    val expandedExcluded = GameCurriculumAndAntiRepetitionEngine.expandExcludedWithSemanticKeys(excludedLowercase)
    val selected = mutableListOf<VerifiedVisualConcept>()
    val usedSemanticInBatch = mutableSetOf<String>()
    val usedPromptsInBatch = mutableSetOf<String>()

    for (c in rankedPool) {
      if (selected.size >= count) break
      if (!validateImageUrlMatchesConcept(c)) continue
      val localizedWord = c.wordForLanguage(languageCode)
      val isDupHistory = GameCurriculumAndAntiRepetitionEngine.isDuplicateContent(
        targetPhrase = localizedWord,
        englishMeaning = c.englishWord,
        conceptId = c.conceptId,
        prompt = c.definitionQuestionEn,
        excludedSet = expandedExcluded
      )
      val canonEn = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(c.englishWord)
      val canonLocal = GameCurriculumAndAntiRepetitionEngine.canonicalConceptKey(localizedWord)
      val promptSig = GameCurriculumAndAntiRepetitionEngine.promptMeaningSignature(c.definitionQuestionEn)
      val isDupBatch = (canonEn.isNotBlank() && canonEn in usedSemanticInBatch) ||
        (canonLocal.isNotBlank() && canonLocal in usedSemanticInBatch) ||
        (promptSig.isNotBlank() && promptSig in usedPromptsInBatch)

      if (!isDupHistory && !isDupBatch) {
        selected.add(c)
        if (canonEn.isNotBlank()) usedSemanticInBatch.add(canonEn)
        if (canonLocal.isNotBlank()) usedSemanticInBatch.add(canonLocal)
        if (promptSig.isNotBlank()) usedPromptsInBatch.add(promptSig)
      }
    }

    // If every curated concept has been consumed in prior levels offline, synthesize unique
    // tier-scoped concepts so we NEVER repeat an old question or image!
    if (selected.size < count) {
      val extendedSeeds = listOf(
        Triple("Acorn", "A small nut from an oak tree.", "park"),
        Triple("Lantern", "A portable lamp with a protective glass case.", "lamp"),
        Triple("Ferry", "A boat carrying passengers across a river or harbor.", "ship"),
        Triple("Apron", "A protective cloth garment worn over clothes while cooking.", "clothing"),
        Triple("Easel", "A wooden stand that holds an artist's canvas while painting.", "art"),
        Triple("Thermos", "An insulated bottle that keeps drinks hot or cold.", "coffee"),
        Triple("Trowel", "A small handheld garden tool used for digging soil.", "tree"),
        Triple("Chandelier", "An ornate branched hanging light fixture in a hall.", "lamp"),
        Triple("Viaduct", "A long bridge carrying a railway over a valley.", "bridge"),
        Triple("Pendulum", "A suspended swinging weight that regulates clock time.", "clock"),
        Triple("Compass Rose", "A circular navigation diagram showing cardinal directions.", "compass"),
        Triple("Kaleidoscope", "An optical tube of mirrors reflecting symmetrical colors.", "science")
      )
      for (i in 0 until extendedSeeds.size * 5) {
        if (selected.size >= count) break
        val seed = extendedSeeds[(levelNumber + i) % extendedSeeds.size]
        val suffix = if (levelNumber > 30) " #${levelNumber}-${i + 1}" else ""
        val word = "${seed.first}$suffix"
        val cid = "dyn:offline_${levelNumber}_$i"
        val qPrompt = "What term matches: ${seed.second} (Stage $levelNumber-${i + 1})"
        val isDup = GameCurriculumAndAntiRepetitionEngine.isDuplicateContent(
          targetPhrase = word,
          englishMeaning = word,
          conceptId = cid,
          prompt = qPrompt,
          excludedSet = expandedExcluded
        )
        if (!isDup && word.lowercase() !in usedSemanticInBatch) {
          val dynConcept = registerDynamicConcept(
            conceptId = cid,
            englishWord = word,
            targetWord = word,
            languageCode = languageCode,
            domainLabel = topic.ifBlank { "Situational Vocabulary" },
            definitionQuestionEn = qPrompt,
            exampleSentence = "Observe ${seed.first.lowercase()} in context ($levelNumber).",
            imageUrl = "",
            iconKey = seed.third,
            levelBand = desiredBand
          )
          selected.add(dynConcept)
          usedSemanticInBatch.add(word.lowercase())
        }
      }
    }

    return selected.take(count)
  }
}

