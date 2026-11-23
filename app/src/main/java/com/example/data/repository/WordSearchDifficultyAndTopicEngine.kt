package com.example.data.repository

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

enum class WordSearchDifficultyTier(val label: String) {
  BEGINNER("Beginner"),
  EASY("Easy"),
  MEDIUM("Medium"),
  HARD("Hard"),
  EXPERT("Expert"),
  MASTER("Master")
}

data class WordSearchDifficultySpec(
  val levelNumber: Int,
  val tier: WordSearchDifficultyTier,
  val wordCount: Int,
  val gridSize: Int,
  val allowVertical: Boolean,
  val allowDiagonal: Boolean,
  val allowReverse: Boolean,
  val preferOverlaps: Boolean,
  val directionSummary: String
)

data class WordSearchPlacement(
  val word: String,
  val clue: String,
  val startRow: Int,
  val startCol: Int,
  val dRow: Int,
  val dCol: Int
)

data class WordSearchBuildResult(
  val grid: List<String>,
  val placedWords: List<Pair<String, String>>,
  val placements: List<WordSearchPlacement>,
  val overlapCount: Int
)

data class WordSearchTopicWord(
  val enWord: String,
  val translations: Map<String, String>,
  val clueEn: String,
  val minTier: Int = 1 // 1 = everyday, 2 = intermediate, 3 = advanced/master
)

data class WordSearchTopicBank(
  val themeKey: String,
  val keywords: List<String>,
  val words: List<WordSearchTopicWord>
)

object WordSearchDifficultyAndTopicEngine {

  private const val WS_HISTORY_PREFS = "lingosphere_word_search_history_prefs"

  private val LATIN_GRID_LANGUAGES = setOf("en", "es", "fr", "de", "it", "pt", "nl", "sv", "no", "da", "tr", "pl", "ro", "id", "vi")

  fun specForLevel(levelNumber: Int): WordSearchDifficultySpec {
    val lvl = levelNumber.coerceAtLeast(1)
    return when {
      lvl <= 10 -> {
        val grid = when {
          lvl <= 4 -> 5
          lvl <= 7 -> 6
          else -> 7
        }
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.BEGINNER,
          wordCount = 4,
          gridSize = grid,
          allowVertical = lvl >= 7,
          allowDiagonal = false,
          allowReverse = false,
          preferOverlaps = false,
          directionSummary = if (lvl >= 7) "Mostly Horizontal + Vertical" else "Horizontal"
        )
      }
      lvl <= 25 -> {
        val count = if (lvl <= 17) 5 else 6
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.EASY,
          wordCount = count,
          gridSize = 8,
          allowVertical = true,
          allowDiagonal = false,
          allowReverse = false,
          preferOverlaps = false,
          directionSummary = "Horizontal & Vertical"
        )
      }
      lvl <= 50 -> {
        val count = when {
          lvl <= 33 -> 6
          lvl <= 42 -> 7
          else -> 8
        }
        val grid = if (lvl <= 37) 9 else 10
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.MEDIUM,
          wordCount = count,
          gridSize = grid,
          allowVertical = true,
          allowDiagonal = true,
          allowReverse = false,
          preferOverlaps = false,
          directionSummary = "Horizontal, Vertical & Diagonal"
        )
      }
      lvl <= 100 -> {
        val count = when {
          lvl <= 66 -> 8
          lvl <= 84 -> 9
          else -> 10
        }
        val grid = if (lvl <= 75) 11 else 12
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.HARD,
          wordCount = count,
          gridSize = grid,
          allowVertical = true,
          allowDiagonal = true,
          allowReverse = true,
          preferOverlaps = false,
          directionSummary = "Forward, Reverse & Diagonal"
        )
      }
      lvl <= 200 -> {
        val count = when {
          lvl <= 133 -> 10
          lvl <= 166 -> 11
          else -> 12
        }
        val grid = if (lvl <= 150) 13 else 14
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.EXPERT,
          wordCount = count,
          gridSize = grid,
          allowVertical = true,
          allowDiagonal = true,
          allowReverse = true,
          preferOverlaps = true,
          directionSummary = "All Directions + Overlapping"
        )
      }
      else -> {
        val count = when {
          lvl <= 275 -> 12
          lvl <= 350 -> 13
          lvl <= 425 -> 14
          else -> 15
        }
        WordSearchDifficultySpec(
          levelNumber = lvl,
          tier = WordSearchDifficultyTier.MASTER,
          wordCount = count,
          gridSize = 15,
          allowVertical = true,
          allowDiagonal = true,
          allowReverse = true,
          preferOverlaps = true,
          directionSummary = "Master 15×15 • Overlapping & Reverse"
        )
      }
    }
  }

  fun normalizeGridWord(raw: String): String {
    val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
    val stripped = decomposed.replace(Regex("\\p{M}+"), "")
    return stripped.uppercase().filter { it in 'A'..'Z' }
  }

  private fun scopeKey(userId: String, languageCode: String): String {
    val safeUser = userId.ifBlank { "default_learner" }.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
    return "${safeUser}_${languageCode.lowercase().take(5)}"
  }

  fun getUsedWordSearchWords(
    context: Context?,
    userId: String,
    languageCode: String
  ): Set<String> {
    if (context == null) return emptySet()
    val prefs = context.getSharedPreferences(WS_HISTORY_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString("used_words_${scopeKey(userId, languageCode)}", null) ?: return emptySet()
    val out = LinkedHashSet<String>()
    runCatching {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val w = normalizeGridWord(arr.optString(i))
        if (w.isNotBlank()) out.add(w)
      }
    }
    return out
  }

  fun recordUsedWordSearchWords(
    context: Context?,
    userId: String,
    languageCode: String,
    words: Collection<String>
  ) {
    if (context == null || words.isEmpty()) return
    val prefs = context.getSharedPreferences(WS_HISTORY_PREFS, Context.MODE_PRIVATE)
    val key = "used_words_${scopeKey(userId, languageCode)}"
    val current = getUsedWordSearchWords(context, userId, languageCode).toMutableList()
    words.forEach { w ->
      val norm = normalizeGridWord(w)
      if (norm.isNotBlank() && norm !in current) {
        current.add(norm)
      }
    }
    prefs.edit().putString(key, JSONArray(current).toString()).apply()
  }

  fun clearUsedWordSearchWords(context: Context?, userId: String, languageCode: String) {
    if (context == null) return
    val prefs = context.getSharedPreferences(WS_HISTORY_PREFS, Context.MODE_PRIVATE)
    prefs.edit().remove("used_words_${scopeKey(userId, languageCode)}").apply()
  }

  private fun w(
    en: String,
    es: String,
    fr: String,
    de: String,
    clue: String,
    tier: Int = 1
  ): WordSearchTopicWord = WordSearchTopicWord(
    enWord = en,
    translations = mapOf("en" to en, "es" to es, "fr" to fr, "de" to de),
    clueEn = clue,
    minTier = tier
  )

  private val TOPIC_BANKS: List<WordSearchTopicBank> = listOf(
    WordSearchTopicBank(
      themeKey = "health",
      keywords = listOf("health", "doctor", "wellness", "clinic", "hospital", "medical"),
      words = listOf(
        w("CARE", "CURA", "SOIN", "ARZT", "Medical help", 1),
        w("REST", "PAUSA", "REPOS", "RUHE", "Quiet recovery", 1),
        w("PAIN", "DOLOR", "MAL", "WEH", "Physical ache", 1),
        w("CURE", "SANAR", "CURE", "HEIL", "Healing treatment", 1),
        w("PILL", "DOSIS", "DOSE", "PILLE", "Tablet medicine", 1),
        w("PULSE", "PULSO", "POULS", "PULS", "Heartbeat rate", 1),
        w("NURSE", "SALUD", "SANTE", "PFLEGE", "Medical caregiver", 1),
        w("FEVER", "FIEBRE", "FIEVRE", "FIEBER", "High body temperature", 1),
        w("COUGH", "GRIPE", "TOUX", "HUSTEN", "Throat reflex", 1),
        w("CLINIC", "CLINICA", "CLINIC", "KLINIK", "Outpatient care center", 1),
        w("DOCTOR", "MEDICO", "DOCTEUR", "DOKTOR", "Licensed physician", 1),
        w("REMEDY", "REMEDIO", "REMEDE", "MITTEL", "Healing solution", 2),
        w("SYRUP", "JARABE", "SIROP", "SAFT", "Liquid cough medicine", 2),
        w("ALLERGY", "ALERGIA", "ALLERGIE", "ALLERGIE", "Immune sensitivity", 2),
        w("VACCINE", "VACUNA", "VACCIN", "IMPFUNG", "Preventive shot", 2),
        w("THERAPY", "TERAPIA", "THERAPIE", "THERAPIE", "Guided recovery care", 2),
        w("BANDAGE", "VENDAJE", "BANDAGE", "VERBAND", "Wound wrap", 2),
        w("DENTIST", "DENTISTA", "DENTISTE", "ZAHNARZT", "Tooth specialist", 2),
        w("VITAMIN", "VITAMINA", "VITAMINE", "VITAMIN", "Essential nutrient", 2),
        w("WELLNESS", "BIENESTAR", "BIENETRE", "GESUND", "Overall health", 2),
        w("PHARMACY", "FARMACIA", "PHARMACIE", "APOTHEKE", "Medicine shop", 2),
        w("HOSPITAL", "HOSPITAL", "HOPITAL", "SPITAL", "Acute care center", 2),
        w("PATIENT", "PACIENTE", "PATIENT", "PATIENT", "Person under care", 2),
        w("CHECKUP", "CHEQUEO", "BILAN", "VORSORGE", "Routine health exam", 2),
        w("SURGEON", "CIRUJANO", "CHIRURG", "CHIRURG", "Operating specialist", 3),
        w("SYMPTOM", "SINTOMA", "SYMPTOME", "SYMPTOM", "Illness indicator", 3),
        w("DIAGNOSIS", "DIAGNOSI", "DIAGNOSTIC", "DIAGNOSE", "Medical finding", 3),
        w("IMMUNITY", "INMUNIDAD", "IMMUNITE", "IMMUNITAT", "Body defense", 3),
        w("HYDRATION", "HIDRATA", "HYDRATE", "TRINKEN", "Fluid balance", 3),
        w("RECOVERY", "RECUPERA", "GUERISON", "GENESUNG", "Return to strength", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "greetings",
      keywords = listOf("greeting", "introduction", "welcome", "polite", "meet"),
      words = listOf(
        w("WAVE", "HOLA", "SALUT", "HALLO", "Friendly hand sign", 1),
        w("NAME", "NOMBRE", "NOM", "NAME", "Personal title", 1),
        w("GLAD", "FELIZ", "RAVI", "FROH", "Pleased to meet", 1),
        w("KIND", "AMABLE", "GENTIL", "NETT", "Warm and courteous", 1),
        w("SMILE", "SONRISA", "SOURIRE", "LACHEN", "Warm facial greeting", 1),
        w("HELLO", "SALUDO", "BONJOUR", "GRUSS", "Standard welcome", 1),
        w("WELCOME", "BIENVEN", "ACCUEIL", "WILLKOM", "Warm reception", 2),
        w("FRIEND", "AMIGO", "AMI", "FREUND", "Trusted companion", 1),
        w("MEET", "CONOCER", "VOIR", "TREFFEN", "First encounter", 1),
        w("THANK", "GRACIAS", "MERCI", "DANKE", "Polite gratitude", 1),
        w("PLEASE", "FAVOR", "PLAIT", "BITTE", "Courteous request", 1),
        w("GUEST", "VISITA", "INVITE", "GAST", "Welcomed visitor", 2),
        w("ORIGIN", "ORIGEN", "ORIGINE", "HERKUNFT", "Where you are from", 2),
        w("INTRO", "CHARLA", "PRESENT", "VORSTEL", "Opening remarks", 2),
        w("POLITE", "CORTES", "POLI", "HOFLICH", "Respectful manners", 2),
        w("HANDSHAKE", "APRETON", "POIGNEE", "HANDDRUCK", "Formal greeting", 3),
        w("COURTESY", "CORTESIA", "COURTOIS", "ANSTAND", "Gracious behavior", 3),
        w("RESPECT", "RESPETO", "RESPECT", "RESPEKT", "High regard", 3),
        w("HOSPITAL", "HOSPEDA", "ACCUEIL", "GASTLICH", "Warm hosting", 3),
        w("REUNION", "REENCUEN", "RETROUV", "TREFFEN", "Meeting again", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "home",
      keywords = listOf("home", "house", "object", "room", "furniture"),
      words = listOf(
        w("SOFA", "SOFA", "SOFA", "SOFA", "Living room seat", 1),
        w("DOOR", "PUERTA", "PORTE", "TUR", "Room entrance", 1),
        w("LAMP", "LAMPARA", "LAMPE", "LAMPE", "Reading light", 1),
        w("ROOF", "TECHO", "TOIT", "DACH", "House top", 1),
        w("DESK", "MESA", "BUREAU", "TISCH", "Study surface", 1),
        w("WALL", "PARED", "MUR", "WAND", "Room divider", 1),
        w("CHAIR", "SILLA", "CHAISE", "STUHL", "Single seat", 1),
        w("CLOCK", "RELOJ", "HORLOGE", "UHR", "Timekeeper", 1),
        w("WINDOW", "VENTANA", "FENETRE", "FENSTER", "Glass opening", 2),
        w("MIRROR", "ESPEJO", "MIROIR", "SPIEGEL", "Reflective glass", 2),
        w("PILLOW", "ALMOHADA", "OREILLER", "KISSEN", "Bed cushion", 2),
        w("GARDEN", "JARDIN", "JARDIN", "GARTEN", "Backyard plants", 2),
        w("SHELF", "ESTANTE", "ETAGERE", "REGAL", "Book ledge", 2),
        w("KITCHEN", "COCINA", "CUISINE", "KUCHE", "Cooking room", 2),
        w("BALCONY", "BALCON", "BALCON", "BALKON", "Outdoor ledge", 2),
        w("CURTAIN", "CORTINA", "RIDEAU", "VORHANG", "Window drape", 2),
        w("BLANKET", "MANTA", "COUVERT", "DECKE", "Warm bed cover", 3),
        w("HALLWAY", "PASILLO", "COULOIR", "FLUR", "Connecting corridor", 3),
        w("CUPBOARD", "ARMARIO", "PLACARD", "SCHRANK", "Dish storage", 3),
        w("FURNISH", "MUEBLES", "MEUBLES", "MOBEL", "Home fittings", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "clothing",
      keywords = listOf("clothing", "style", "accessory", "outfit", "wear", "fashion"),
      words = listOf(
        w("COAT", "ABRIGO", "VESTE", "MANTEL", "Outer garment", 1),
        w("BELT", "CINTO", "CEINT", "GURT", "Waist strap", 1),
        w("SHOE", "ZAPATO", "CHAUSS", "SCHUH", "Footwear piece", 1),
        w("HAT", "GORRO", "CHAPEAU", "HUT", "Head cover", 1),
        w("SOCK", "MEDIA", "CHAUSSET", "SOCKE", "Foot warmer", 1),
        w("SHIRT", "CAMISA", "CHEMISE", "HEMD", "Button top", 1),
        w("SCARF", "BUFANDA", "ECHARPE", "SCHAL", "Neck wrap", 1),
        w("BOOTS", "BOTAS", "BOTTES", "STIEFEL", "Tall footwear", 2),
        w("DRESS", "VESTIDO", "ROBE", "KLEID", "One-piece outfit", 2),
        w("GLOVE", "GUANTE", "GANT", "HANDSCH", "Hand warmer", 2),
        w("HOODIE", "SUDADERA", "CAPUCHE", "PULLI", "Cozy hooded top", 2),
        w("JACKET", "CHAQUETA", "BLOUSON", "JACKE", "Light coat", 2),
        w("FABRIC", "TELA", "TISSU", "STOFF", "Woven material", 2),
        w("COLLAR", "CUELLO", "COL", "KRAGEN", "Shirt neckline", 2),
        w("POCKET", "BOLSILLO", "POCHE", "TASCHE", "Sewn pouch", 2),
        w("SNEAKER", "TENIS", "BASKET", "TURNSCH", "Running shoe", 3),
        w("TAILOR", "SASTRE", "TAILLEUR", "SCHNEID", "Garment fitter", 3),
        w("BUTTON", "BOTON", "BOUTON", "KNOPF", "Shirt fastener", 2),
        w("WARDROBE", "ROPERO", "GARDERO", "KLEIDER", "Outfit collection", 3),
        w("ELEGANCE", "ESTILO", "ELEGANCE", "ELEGANZ", "Refined dress sense", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "food",
      keywords = listOf("food", "drink", "cafe", "dining", "restaurant", "ingredient", "coffee"),
      words = listOf(
        w("CAKE", "TARTA", "GATEAU", "KUCHEN", "Sweet baked treat", 1),
        w("SOUP", "SOPA", "SOUPE", "SUPPE", "Warm bowl dish", 1),
        w("RICE", "ARROZ", "RIZ", "REIS", "Staple grain", 1),
        w("MILK", "LECHE", "LAIT", "MILCH", "Dairy beverage", 1),
        w("BREAD", "PAN", "PAIN", "BROT", "Baked loaf", 1),
        w("FRUIT", "FRUTA", "FRUIT", "OBST", "Sweet produce", 1),
        w("SALAD", "ENSALADA", "SALADE", "SALAT", "Crisp greens", 1),
        w("JUICE", "JUGO", "JUS", "SAFT", "Pressed drink", 1),
        w("PASTA", "PASTA", "PATES", "NUDELN", "Italian noodles", 2),
        w("ROAST", "ASADO", "ROTI", "BRATEN", "Oven-cooked dish", 2),
        w("SPICE", "ESPECIA", "EPICE", "GEWURZ", "Aromatic seasoning", 2),
        w("CHEESE", "QUESO", "FROMAGE", "KASE", "Aged dairy", 2),
        w("COFFEE", "CAFE", "CAFE", "KAFFEE", "Morning brew", 2),
        w("WAITER", "MESERO", "SERVEUR", "KELLNER", "Dining server", 2),
        w("PASTRY", "BOLLO", "BRIOCHE", "GEBACK", "Bakery delight", 2),
        w("DESSERT", "POSTRE", "DESSERT", "DESSERT", "Final sweet course", 2),
        w("FLAVOR", "SABOR", "SAVEUR", "GESCHM", "Distinct taste", 3),
        w("RECIPE", "RECETA", "RECETTE", "REZEPT", "Cooking instructions", 3),
        w("GARNISH", "ADORNO", "GARNIT", "GARNIER", "Plate decoration", 3),
        w("CUISINE", "COCINA", "CUISINE", "KUCHE", "Regional cooking style", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "daily_actions",
      keywords = listOf("action", "routine", "morning", "habit"),
      words = listOf(
        w("WALK", "CAMINAR", "MARCHER", "GEHEN", "Go on foot", 1),
        w("WAKE", "DESPERT", "EVEIL", "WACHEN", "Leave sleep", 1),
        w("READ", "LEER", "LIRE", "LESEN", "Scan written text", 1),
        w("COOK", "COCINAR", "CUISIN", "KOCHEN", "Prepare a meal", 1),
        w("WASH", "LAVAR", "LAVER", "WASCHEN", "Clean with water", 1),
        w("PLAN", "PLANEAR", "PLANIF", "PLANEN", "Organize ahead", 1),
        w("WRITE", "ESCRIBIR", "ECRIRE", "SCHREIB", "Compose notes", 1),
        w("STUDY", "ESTUDIAR", "ETUDIER", "LERNEN", "Learn carefully", 1),
        w("STRETCH", "ESTIRAR", "ETIRER", "DEHNEN", "Warm up muscles", 2),
        w("LISTEN", "ESCUCHAR", "ECOUTER", "HOREN", "Pay attention to sound", 2),
        w("PREPARE", "PREPARAR", "PREPARER", "BEREIT", "Get ready early", 2),
        w("COMMUTE", "VIAJAR", "TRAJET", "PENDELN", "Daily work trip", 2),
        w("TIDYING", "ORDENAR", "RANGER", "ORDNEN", "Put things away", 2),
        w("PRACTICE", "PRACTICA", "PRATIQUE", "UBUNG", "Repeat to master", 2),
        w("ORGANIZE", "ORGANIZA", "ORGANISE", "ORDNUNG", "Structure your day", 3),
        w("HYDRATE", "BEBER", "BOIRE", "TRINKEN", "Drink water", 2),
        w("REVIEW", "REPASAR", "REVISER", "PRUFEN", "Look over notes", 3),
        w("EXERCISE", "EJERCIT", "EXERCICE", "SPORT", "Active workout", 3),
        w("REFLECT", "PENSAR", "MEDITER", "DENKEN", "Pause and reflect", 3),
        w("HABITUAL", "HABITO", "HABITUDE", "GEWOHNT", "Steady daily pattern", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "city",
      keywords = listOf("city", "place", "direction", "transit", "town", "street"),
      words = listOf(
        w("PARK", "PARQUE", "PARC", "PARK", "Green public space", 1),
        w("BANK", "BANCO", "BANQUE", "BANK", "Financial branch", 1),
        w("ROAD", "CALLE", "RUE", "STRASSE", "Paved street", 1),
        w("TRAM", "TRANVIA", "TRAMWAY", "BAHN", "Street rail car", 1),
        w("METRO", "METRO", "METRO", "UBAHN", "Underground train", 1),
        w("PLAZA", "PLAZA", "PLACE", "PLATZ", "Open town square", 1),
        w("BRIDGE", "PUENTE", "PONT", "BRUCKE", "River crossing", 2),
        w("MUSEUM", "MUSEO", "MUSEE", "MUSEUM", "Art and history hall", 2),
        w("AVENUE", "AVENIDA", "AVENUE", "ALLEE", "Wide tree-lined street", 2),
        w("MARKET", "MERCADO", "MARCHE", "MARKT", "Stalls and produce", 2),
        w("STATION", "ESTACION", "GARE", "BAHNHOF", "Train hub", 2),
        w("CORNER", "ESQUINA", "COIN", "ECKE", "Street intersection", 2),
        w("CROSSING", "CRUCE", "PASSAGE", "KREUZ", "Pedestrian path", 3),
        w("SUBURB", "BARRIO", "QUARTIER", "VORORT", "Residential zone", 2),
        w("DOWNTOWN", "CENTRO", "CENTRE", "ZENTRUM", "City core", 3),
        w("SIDEWALK", "ACERA", "TROTTOIR", "GEHWEG", "Walking path", 3),
        w("LIBRARY", "BIBLIOT", "BIBLIO", "BIBLIOT", "Book archive", 2),
        w("LANDMARK", "MONUMENT", "REPERE", "DENKMAL", "Famous sight", 3),
        w("HARBOR", "PUERTO", "PORT", "HAFEN", "Ship dock", 3),
        w("ROUNDABOUT", "ROTONDA", "ROND", "KREISEL", "Circular junction", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "people",
      keywords = listOf("people", "family", "description", "appearance"),
      words = listOf(
        w("AUNT", "TIA", "TANTE", "TANTE", "Parent's sister", 1),
        w("TALL", "ALTO", "GRAND", "GROSS", "Above average height", 1),
        w("KIND", "GENTIL", "BON", "LIEB", "Caring nature", 1),
        w("CALM", "SERENO", "CALME", "RUHIG", "Peaceful manner", 1),
        w("BRAVE", "VALIENTE", "BRAVE", "MUTIG", "Courageous person", 1),
        w("SISTER", "HERMANA", "SOEUR", "SCHWEST", "Female sibling", 1),
        w("COUSIN", "PRIMO", "COUSIN", "COUSIN", "Extended relative", 2),
        w("HONEST", "HONRADO", "HONNETE", "EHRLICH", "Truthful character", 2),
        w("PARENTS", "PADRES", "PARENTS", "ELTERN", "Mother and father", 2),
        w("LOYAL", "FIEL", "FIDELE", "TREU", "Dependable friend", 2),
        w("GENTLE", "SUAVE", "DOUX", "SANFT", "Tender attitude", 2),
        w("HUMBLE", "HUMILDE", "HUMBLE", "DEMUTIG", "Modest spirit", 2),
        w("NEPHEW", "SOBRINO", "NEVEU", "NEFFE", "Sibling's son", 2),
        w("CHEERFUL", "ALEGRE", "JOYEUX", "FROHLICH", "Bright mood", 3),
        w("SINCERE", "SINCERO", "SINCERE", "AUFRICH", "Genuine heart", 3),
        w("CREATIVE", "CREATIVO", "CREATIF", "KREATIV", "Imaginative thinker", 3),
        w("CONFIDENT", "SEGURO", "CONFIANT", "SICHER", "Self-assured manner", 3),
        w("GENEROUS", "GENEROSO", "GENEREUX", "GROSSZUG", "Willing to share", 3),
        w("PATIENT", "PACIENTE", "PATIENT", "GEDULDIG", "Never rushes", 3),
        w("RELATIVE", "PARIENTE", "PARENT", "VERWANDT", "Family member", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "weather",
      keywords = listOf("weather", "season", "nature", "climate", "sky"),
      words = listOf(
        w("RAIN", "LLUVIA", "PLUIE", "REGEN", "Falling drops", 1),
        w("WIND", "VIENTO", "VENT", "WIND", "Moving air", 1),
        w("SNOW", "NIEVE", "NEIGE", "SCHNEE", "Winter flakes", 1),
        w("WARM", "CALIDO", "CHAUD", "WARM", "Gentle heat", 1),
        w("CLOUD", "NUBE", "NUAGE", "WOLKE", "Sky mist", 1),
        w("STORM", "TORMENTA", "ORAGE", "STURM", "Thunder and rain", 1),
        w("FROST", "HELADA", "GIVRE", "FROST", "Morning ice", 2),
        w("BREEZE", "BRISA", "BRISE", "BRISE", "Soft coastal wind", 2),
        w("AUTUMN", "OTONO", "AUTOMNE", "HERBST", "Leaf-fall season", 2),
        w("SPRING", "PRIMAVER", "PRINTEMP", "FRUHLING", "Blossom season", 2),
        w("WINTER", "INVIERNO", "HIVER", "WINTER", "Coldest season", 2),
        w("SUMMER", "VERANO", "ETE", "SOMMER", "Sunniest season", 2),
        w("RAINBOW", "ARCOIRIS", "ARCENCIEL", "REGENBO", "Colored sky arc", 2),
        w("THUNDER", "TRUENO", "TONNERRE", "DONNER", "Storm boom", 2),
        w("SUNSHINE", "SOL", "SOLEIL", "SONNE", "Bright rays", 2),
        w("CLIMATE", "CLIMA", "CLIMAT", "KLIMA", "Long-term pattern", 3),
        w("FORECAST", "PRONOST", "METEO", "WETTER", "Daily outlook", 3),
        w("HUMIDITY", "HUMEDAD", "HUMIDITE", "FEUCHTE", "Moisture in air", 3),
        w("BLIZZARD", "VENTISCA", "TEMPETE", "SCHNEEST", "Heavy snow gale", 3),
        w("HORIZON", "HORIZONT", "HORIZON", "HORIZONT", "Where sky meets sea", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "travel",
      keywords = listOf("airport", "hotel", "travel", "luggage", "check-in", "flight"),
      words = listOf(
        w("GATE", "PUERTA", "PORTE", "GATE", "Boarding doorway", 1),
        w("TRIP", "VIAJE", "VOYAGE", "REISE", "Short journey", 1),
        w("MAP", "MAPA", "CARTE", "KARTE", "Navigation guide", 1),
        w("TAXI", "TAXI", "TAXI", "TAXI", "Hired car", 1),
        w("VISA", "VISA", "VISA", "VISUM", "Entry permit", 1),
        w("ROOM", "CUARTO", "CHAMBRE", "ZIMMER", "Hotel suite", 1),
        w("PILOT", "PILOTO", "PILOTE", "PILOT", "Aircraft captain", 1),
        w("HOTEL", "HOTEL", "HOTEL", "HOTEL", "Travel lodging", 1),
        w("FLIGHT", "VUELO", "VOL", "FLUG", "Air journey", 2),
        w("TICKET", "BOLETO", "BILLET", "TICKET", "Travel pass", 2),
        w("LUGGAGE", "EQUIPAJE", "BAGAGE", "GEPACK", "Packed bags", 2),
        w("PASSPORT", "PASAPORT", "PASSEPOR", "REISEPAS", "Border ID book", 2),
        w("SUITCASE", "MALETA", "VALISE", "KOFFER", "Wheeled case", 2),
        w("TERMINAL", "TERMINAL", "TERMINAL", "TERMINAL", "Airport concourse", 2),
        w("BOARDING", "EMBARQUE", "EMBARQU", "EINSTEIG", "Entering plane", 2),
        w("CUSTOMS", "ADUANA", "DOUANE", "ZOLL", "Border inspection", 3),
        w("BOOKING", "RESERVA", "RESERVAT", "BUCHUNG", "Reserved stay", 3),
        w("RUNWAY", "PISTA", "PISTE", "BAHN", "Takeoff strip", 3),
        w("ITINERARY", "RUTA", "PARCOURS", "REISEPLA", "Planned schedule", 3),
        w("DEPARTURE", "SALIDA", "DEPART", "ABFLUG", "Leaving gate", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "feelings",
      keywords = listOf("feeling", "mood", "emotion", "reaction"),
      words = listOf(
        w("GLAD", "ALEGRE", "JOYEUX", "FROH", "Happy feeling", 1),
        w("HOPE", "ESPERA", "ESPOIR", "HOFFEN", "Optimistic wish", 1),
        w("CALM", "CALMA", "CALME", "RUHE", "Quiet peace", 1),
        w("BOLD", "AUDAZ", "HARDI", "KUHN", "Fearless spirit", 1),
        w("PROUD", "ORGULLO", "FIER", "STOLZ", "Earned satisfaction", 1),
        w("EAGER", "ANSIOSO", "AVIDE", "EIFRIG", "Full of keen interest", 2),
        w("WORRY", "MIEDO", "SOUCI", "SORGE", "Anxious concern", 2),
        w("JOYFUL", "GOZOSO", "JOYEUX", "FREUDIG", "Full of delight", 2),
        w("AMAZED", "ASOMBRO", "ETONNE", "STAUNEN", "Deeply surprised", 2),
        w("EXCITED", "ANIMADO", "EXCITE", "AUFGEREG", "High anticipation", 2),
        w("CURIOUS", "CURIOSO", "CURIEUX", "NEUGIER", "Eager to learn", 2),
        w("GRATEFUL", "GRATO", "GRATITUD", "DANKBAR", "Deeply thankful", 2),
        w("RELIEVED", "ALIVIADO", "SOULAGE", "ERLEICHT", "Stress lifted", 3),
        w("SERENE", "SERENO", "SEREIN", "HEITER", "Untroubled calm", 3),
        w("EMPATHY", "EMPATIA", "EMPATHIE", "EMPATHIE", "Shared feeling", 3),
        w("INSPIRED", "INSPIRA", "INSPIRE", "BEGEIST", "Moved to create", 3),
        w("OPTIMISM", "OPTIMISM", "OPTIMISM", "OPTIMISM", "Positive outlook", 3),
        w("CONTENT", "CONTENTO", "CONTENT", "ZUFRIED", "Peaceful satisfaction", 3),
        w("COURAGE", "CORAJE", "COURAGE", "MUT", "Strength in fear", 3),
        w("HARMONY", "ARMONIA", "HARMONIE", "HARMONIE", "Inner balance", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "shopping",
      keywords = listOf("shopping", "price", "market", "store", "payment", "receipt"),
      words = listOf(
        w("SALE", "OFERTA", "SOLDE", "ANGEBOT", "Reduced price", 1),
        w("CASH", "EFECTIV", "ARGENT", "BARGELD", "Paper money", 1),
        w("SHOP", "TIENDA", "BOUTIQUE", "LADEN", "Retail store", 1),
        w("COIN", "MONEDA", "PIECE", "MUNZE", "Metal currency", 1),
        w("COST", "COSTO", "COUT", "KOSTEN", "Item price", 1),
        w("PRICE", "PRECIO", "PRIX", "PREIS", "Tagged amount", 1),
        w("STORE", "ALMACEN", "MAGASIN", "GESCHAFT", "Shopping venue", 1),
        w("WALLET", "CARTERA", "PORTEFEU", "BORSE", "Card holder", 2),
        w("BASKET", "CESTA", "PANIER", "KORB", "Hand-carried bin", 2),
        w("BUDGET", "CUENTA", "BUDGET", "BUDGET", "Spending plan", 2),
        w("CHANGE", "CAMBIO", "MONNAIE", "WECHSEL", "Returned coins", 2),
        w("COUNTER", "CAJA", "CAISSE", "KASSE", "Checkout desk", 2),
        w("RECEIPT", "RECIBO", "RECU", "QUITTUNG", "Printed proof", 2),
        w("BARGAIN", "GANGA", "AFFAIRE", "SCHNAPP", "Great value deal", 2),
        w("DISCOUNT", "DESCUENT", "REMISE", "RABATT", "Percent off", 3),
        w("REFUND", "REEMBOLS", "REMBOUR", "ERSTATT", "Money back", 3),
        w("CHECKOUT", "PAGO", "PAIEMENT", "ZAHLUNG", "Final register", 3),
        w("VENDOR", "VENDEDOR", "VENDEUR", "HANDLER", "Market seller", 3),
        w("MERCHANT", "COMERCIA", "MARCHAND", "KAUFMANN", "Shop owner", 3),
        w("WARRANTY", "GARANTIA", "GARANTIE", "GARANTIE", "Product guarantee", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "work",
      keywords = listOf("work", "meeting", "career", "office", "business", "project"),
      words = listOf(
        w("GOAL", "META", "BUT", "ZIEL", "Target objective", 1),
        w("TEAM", "EQUIPO", "EQUIPE", "TEAM", "Collaborating group", 1),
        w("TASK", "TAREA", "TACHE", "AUFGABE", "Assigned work", 1),
        w("HIRE", "EMPLEO", "EMBAUCH", "ARBEIT", "Recruit staff", 1),
        w("MEMO", "NOTA", "MEMO", "NOTIZ", "Office brief", 1),
        w("STAFF", "PERSONAL", "STAFF", "PERSONAL", "Employees", 1),
        w("OFFICE", "OFICINA", "BUREAU", "BURO", "Workspace", 1),
        w("CLIENT", "CLIENTE", "CLIENT", "KUNDE", "Served customer", 2),
        w("RESUME", "CURRICUL", "CV", "LEBENS", "Career summary", 2),
        w("SALARY", "SALARIO", "SALAIRE", "GEHALT", "Monthly pay", 2),
        w("AGENDA", "AGENDA", "AGENDA", "AGENDA", "Meeting topics", 2),
        w("REPORT", "INFORME", "RAPPORT", "BERICHT", "Status summary", 2),
        w("MANAGER", "GERENTE", "GERANT", "LEITER", "Team lead", 2),
        w("PROJECT", "PROYECTO", "PROJET", "PROJEKT", "Planned initiative", 2),
        w("MEETING", "REUNION", "REUNION", "TREFFEN", "Team discussion", 2),
        w("DEADLINE", "PLAZO", "ECHEANCE", "FRIST", "Due date", 3),
        w("COLLEAGUE", "COLEGA", "COLLEGUE", "KOLLEGE", "Coworker", 3),
        w("STRATEGY", "ESTRATE", "STRATEGI", "STRATEG", "Long-term plan", 3),
        w("CONTRACT", "CONTRATO", "CONTRAT", "VERTRAG", "Signed terms", 3),
        w("MENTOR", "MENTOR", "MENTOR", "MENTOR", "Career advisor", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "hobbies",
      keywords = listOf("hobby", "hobbies", "music", "sport", "art"),
      words = listOf(
        w("SONG", "CANCION", "CHANSON", "LIED", "Vocal melody", 1),
        w("SWIM", "NADAR", "NAGER", "SCHWIMM", "Pool exercise", 1),
        w("DRUM", "TAMBOR", "TAMBOUR", "TROMMEL", "Rhythm instrument", 1),
        w("GAME", "JUEGO", "JEU", "SPIEL", "Leisure match", 1),
        w("TUNE", "TONO", "MELODIE", "KLANG", "Catchy notes", 1),
        w("PAINT", "PINTURA", "PEINTURE", "MALEN", "Brush color", 1),
        w("CHESS", "AJEDREZ", "ECHECS", "SCHACH", "Strategy board game", 2),
        w("PIANO", "PIANO", "PIANO", "KLAVIER", "88-key instrument", 2),
        w("DANCE", "BAILE", "DANSE", "TANZ", "Rhythmic movement", 1),
        w("GUITAR", "GUITARRA", "GUITARE", "GITARRE", "Six-string music", 2),
        w("SOCCER", "FUTBOL", "FOOTBALL", "FUSSBALL", "Pitch team sport", 2),
        w("TENNIS", "TENIS", "TENNIS", "TENNIS", "Racket match", 2),
        w("HIKING", "SENDERO", "RANDO", "WANDERN", "Mountain trail walk", 2),
        w("SKETCH", "BOCETO", "CROQUIS", "SKIZZE", "Quick pencil art", 2),
        w("CONCERT", "CONCIERT", "CONCERT", "KONZERT", "Live performance", 3),
        w("GALLERY", "GALERIA", "GALERIE", "GALERIE", "Artwork display", 3),
        w("RHYTHM", "RITMO", "RYTHME", "RHYTHMUS", "Musical beat", 3),
        w("SCULPT", "ESCULTUR", "SCULPTER", "BILDHAU", "Carved art", 3),
        w("CYCLING", "CICLISMO", "CYCLISME", "RADSPORT", "Bike touring", 3),
        w("SYMPHONY", "SINFONIA", "SYMPHON", "SINFONIE", "Orchestral work", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "tech",
      keywords = listOf("tech", "technology", "call", "message", "digital", "phone", "computer"),
      words = listOf(
        w("CODE", "CODIGO", "CODE", "CODE", "Program lines", 1),
        w("CHAT", "CHARLA", "CHAT", "CHAT", "Instant text", 1),
        w("LINK", "ENLACE", "LIEN", "LINK", "Web address", 1),
        w("WIFI", "WIFI", "WIFI", "WLAN", "Wireless net", 1),
        w("DATA", "DATOS", "DONNEES", "DATEN", "Stored info", 1),
        w("ICON", "ICONO", "ICONE", "SYMBOL", "App symbol", 1),
        w("EMAIL", "CORREO", "COURRIEL", "EMAIL", "Digital mail", 1),
        w("CLOUD", "NUBE", "NUAGE", "CLOUD", "Remote storage", 2),
        w("PHONE", "TELEFONO", "TELEPHON", "TELEFON", "Voice device", 2),
        w("SCREEN", "PANTALLA", "ECRAN", "BILDSCH", "Visual display", 2),
        w("SIGNAL", "SENAL", "SIGNAL", "SIGNAL", "Network bars", 2),
        w("CAMERA", "CAMARA", "CAMERA", "KAMERA", "Photo lens", 2),
        w("BACKUP", "RESPALDO", "SAUVEGAR", "SICHERN", "Safety copy", 2),
        w("DEVICE", "APARATO", "APPAREIL", "GERAT", "Electronic tool", 2),
        w("UPLOAD", "SUBIR", "CHARGER", "HOCHLAD", "Send to cloud", 3),
        w("BATTERY", "BATERIA", "BATTERIE", "AKKU", "Power cell", 2),
        w("NETWORK", "RED", "RESEAU", "NETZWERK", "Connected nodes", 3),
        w("BROWSER", "NAVEGAD", "NAVIGAT", "BROWSER", "Web viewer", 3),
        w("SOFTWARE", "PROGRAMA", "LOGICIEL", "SOFTWARE", "App system", 3),
        w("SECURITY", "SEGURID", "SECURITE", "SICHER", "Data protection", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "time",
      keywords = listOf("time", "schedule", "appointment", "calendar", "hour"),
      words = listOf(
        w("NOON", "MEDIO", "MIDI", "MITTAG", "12:00 PM", 1),
        w("WEEK", "SEMANA", "SEMAINE", "WOCHE", "Seven days", 1),
        w("DAWN", "ALBA", "AUBE", "MORGEN", "First light", 1),
        w("SOON", "PRONTO", "BIENTOT", "BALD", "In a short while", 1),
        w("HOUR", "HORA", "HEURE", "STUNDE", "Sixty minutes", 1),
        w("TODAY", "HOY", "AUJOURD", "HEUTE", "Present day", 1),
        w("LATER", "LUEGO", "TARD", "SPATER", "Future moment", 1),
        w("MONTH", "MES", "MOIS", "MONAT", "Calendar division", 1),
        w("MORNING", "MANANA", "MATIN", "MORGEN", "Early hours", 2),
        w("EVENING", "TARDE", "SOIREE", "ABEND", "Dusk hours", 2),
        w("MINUTE", "MINUTO", "MINUTE", "MINUTE", "60 seconds", 2),
        w("SECOND", "SEGUNDO", "SECONDE", "SEKUNDE", "Brief tick", 2),
        w("FUTURE", "FUTURO", "FUTUR", "ZUKUNFT", "Time ahead", 2),
        w("WEEKEND", "FINDE", "WEEKEND", "WOCHENEN", "Saturday & Sunday", 2),
        w("CALENDAR", "CALEND", "CALENDR", "KALENDER", "Date planner", 3),
        w("SCHEDULE", "HORARIO", "HORAIRE", "ZEITPLAN", "Timed agenda", 3),
        w("TIMELINE", "CRONOL", "CHRONO", "ZEITLIN", "Event sequence", 3),
        w("PUNCTUAL", "PUNTUAL", "PONCTUEL", "PUNKTLIC", "Right on time", 3),
        w("DURATION", "DURACION", "DUREE", "DAUER", "Length of time", 3),
        w("CENTURY", "SIGLO", "SIECLE", "JAHRHUND", "Hundred years", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "social",
      keywords = listOf("social", "plan", "making friends", "party", "invite"),
      words = listOf(
        w("CLUB", "CLUB", "CLUB", "KLUB", "Shared group", 1),
        w("GIFT", "REGALO", "CADEAU", "GESCHENK", "Thoughtful present", 1),
        w("CHAT", "CHARLA", "CAUSER", "PLAUDER", "Friendly talk", 1),
        w("HOST", "ANFITRI", "HOTE", "GASTGEB", "Party organizer", 1),
        w("TOAST", "BRINDIS", "TOAST", "PROST", "Celebration wish", 1),
        w("EVENT", "EVENTO", "EVENEM", "EVENT", "Planned gathering", 1),
        w("PARTY", "FIESTA", "FETE", "PARTY", "Festive meetup", 1),
        w("DINNER", "CENA", "DINER", "ABENDES", "Evening meal together", 2),
        w("PICNIC", "PICNIC", "PIQUEN", "PICKNICK", "Outdoor meal", 2),
        w("BUDDY", "COLEGA", "COPAIN", "KUMPEL", "Close pal", 2),
        w("INVITE", "INVITAR", "INVITER", "EINLADEN", "Ask someone along", 2),
        w("FESTIVE", "FESTIVO", "FESTIF", "FESTLICH", "Joyful mood", 2),
        w("GATHER", "REUNIR", "REUNIR", "SAMMELN", "Meet as a group", 2),
        w("CONCERT", "SHOW", "SPECTAC", "KONZERT", "Evening outing", 2),
        w("LOUNGE", "SALON", "SALON", "LOUNGE", "Relaxed hangout", 3),
        w("DIALOGUE", "DIALOGO", "DIALOGUE", "DIALOG", "Two-way talk", 3),
        w("OUTING", "SALIDA", "SORTIE", "AUSFLUG", "Group excursion", 3),
        w("TOGETHER", "JUNTOS", "ENSEMBLE", "ZUSAMMEN", "Side by side", 3),
        w("COMPANY", "COMPANIA", "COMPAGN", "GESELL", "Good friends", 3),
        w("BONDING", "LAZO", "LIEN", "BINDUNG", "Growing closer", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "study",
      keywords = listOf("study", "reading", "campus", "school", "education", "book"),
      words = listOf(
        w("BOOK", "LIBRO", "LIVRE", "BUCH", "Bound pages", 1),
        w("QUIZ", "PRUEBA", "QUIZ", "QUIZ", "Quick check", 1),
        w("NOTE", "APUNTE", "NOTE", "NOTIZ", "Written reminder", 1),
        w("EXAM", "EXAMEN", "EXAMEN", "PRUFUNG", "Final test", 1),
        w("DESK", "PUPITRE", "PUPITRE", "PULT", "Study table", 1),
        w("CLASS", "CLASE", "COURS", "KLASSE", "Course session", 1),
        w("ESSAY", "ENSAYO", "ESSAI", "AUFSATZ", "Written paper", 1),
        w("GRADE", "NOTA", "NOTE", "NOTE", "Course score", 2),
        w("CAMPUS", "CAMPUS", "CAMPUS", "CAMPUS", "University grounds", 2),
        w("DEGREE", "TITULO", "DIPLOME", "ABSCHLU", "Earned credential", 2),
        w("NOVEL", "NOVELA", "ROMAN", "ROMAN", "Long fiction book", 2),
        w("TUTOR", "TUTOR", "TUTEUR", "TUTOR", "Personal teacher", 2),
        w("LESSON", "LECCION", "LECON", "LEKTION", "Learning unit", 2),
        w("COURSE", "CURSO", "STAGE", "KURS", "Series of classes", 2),
        w("CHAPTER", "CAPITUL", "CHAPITRE", "KAPITEL", "Book section", 2),
        w("LECTURE", "CHARLA", "COURS", "VORTRAG", "Professor talk", 3),
        w("GRAMMAR", "GRAMAT", "GRAMMAIR", "GRAMMAT", "Language rules", 3),
        w("SCHOLAR", "BECARIO", "BOURSIER", "STIPEND", "Dedicated student", 3),
        w("RESEARCH", "ESTUDIO", "RECHERCH", "FORSCH", "Deep investigation", 3),
        w("ACADEMIC", "ACADEMIC", "ACADEMIQ", "AKADEMIS", "Scholarly study", 3)
      )
    ),
    WordSearchTopicBank(
      themeKey = "culture",
      keywords = listOf("culture", "tradition", "etiquette", "heritage", "custom"),
      words = listOf(
        w("FOLK", "FOLK", "FOLK", "VOLK", "Traditional local", 1),
        w("MYTH", "MITO", "MYTHE", "MYTHOS", "Ancient story", 1),
        w("SONG", "CANTO", "CHANT", "GESANG", "Heritage music", 1),
        w("ARTE", "ARTE", "ART", "KUNST", "Creative craft", 1),
        w("RITE", "RITO", "RITE", "RITUS", "Ceremony act", 1),
        w("CRAFT", "OFICIO", "METIER", "HANDWERK", "Artisan skill", 1),
        w("DANCE", "DANZA", "DANSE", "TANZ", "Cultural movement", 2),
        w("FEAST", "FIESTA", "FESTIN", "FESTMAHL", "Traditional banquet", 2),
        w("CUSTOM", "USO", "COUTUME", "BRAUCH", "Local habit", 2),
        w("MANNER", "MODO", "MANIERE", "MANIER", "Social etiquette", 2),
        w("POETRY", "POESIA", "POESIE", "POESIE", "Verse art", 2),
        w("TEMPLE", "TEMPLO", "TEMPLE", "TEMPEL", "Sacred building", 2),
        w("PARADE", "DESFILE", "DEFILE", "UMZUG", "Festive procession", 2),
        w("MUSEUM", "MUSEO", "MUSEE", "MUSEUM", "Heritage hall", 2),
        w("FESTIVAL", "FESTIVAL", "FESTIVAL", "FEST", "Annual celebration", 3),
        w("HERITAGE", "LEGADO", "PATRIM", "ERBE", "Passed-down roots", 3),
        w("ETIQUETTE", "ETIQUET", "ETIQUET", "ETIKETTE", "Polite customs", 3),
        w("CEREMONY", "CEREMON", "CEREMON", "ZEREMON", "Formal ritual", 3),
        w("FOLKLORE", "FOLCLORE", "FOLKLORE", "FOLKLORE", "Oral traditions", 3),
        w("ANCESTRY", "RAICES", "ANCETRE", "AHNEN", "Family history", 3)
      )
    )
  )

  fun findTopicBank(objectiveTitle: String): WordSearchTopicBank {
    val lower = objectiveTitle.lowercase()
    return TOPIC_BANKS.firstOrNull { bank ->
      bank.keywords.any { kw -> lower.contains(kw) }
    } ?: TOPIC_BANKS.first()
  }

  fun isCandidateTopicRelevant(
    word: String,
    clue: String,
    objectiveTitle: String
  ): Boolean {
    val norm = normalizeGridWord(word)
    if (norm.length < 3) return false
    val bank = findTopicBank(objectiveTitle)
    // Reject obvious cross-topic leaks (e.g., Platform/Currency/Deadline showing up inside Health!)
    val allOtherBanks = TOPIC_BANKS.filter { it.themeKey != bank.themeKey }
    val inThisBank = bank.words.any { tw ->
      normalizeGridWord(tw.enWord) == norm || tw.translations.values.any { normalizeGridWord(it) == norm }
    }
    if (inThisBank) return true

    val inWrongBank = allOtherBanks.any { other ->
      other.words.any { tw ->
        normalizeGridWord(tw.enWord) == norm || tw.translations.values.any { normalizeGridWord(it) == norm }
      }
    }
    if (inWrongBank) return false

    // Also inspect clue against off-topic keywords if Gemini generated an unfamiliar word
    val clueLower = clue.lowercase()
    if (bank.themeKey == "health") {
      val offTopicHealthMarkers = listOf("train", "platform", "currency", "airport", "deadline", "calm", "office")
      if (offTopicHealthMarkers.any { norm.equals(it, ignoreCase = true) || clueLower.contains(it) }) {
        return false
      }
    }
    return true
  }

  fun generateValidatedWordSearchBoard(
    context: Context?,
    userId: String,
    languageCode: String,
    levelNumber: Int,
    objectiveTitle: String,
    geminiCandidates: List<Pair<String, String>> = emptyList(),
    additionalExcluded: Set<String> = emptySet(),
    persistUsedWords: Boolean = true
  ): WordSearchBuildResult {
    val spec = specForLevel(levelNumber)
    val langKey = languageCode.lowercase().take(2)
    val useNativeGridWord = langKey in LATIN_GRID_LANGUAGES

    val persistedUsed = getUsedWordSearchWords(context, userId, languageCode)
    val excludedNorm = (persistedUsed + additionalExcluded.map { normalizeGridWord(it) })
      .filter { it.length >= 3 }
      .toMutableSet()

    // 1. Filter Gemini candidates strictly by length, topic relevance, and exclusion list
    val filteredGemini = geminiCandidates.mapNotNull { (rawWord, rawClue) ->
      val gridWord = normalizeGridWord(rawWord)
      if (gridWord.length in 3..spec.gridSize &&
        gridWord !in excludedNorm &&
        isCandidateTopicRelevant(gridWord, rawClue, objectiveTitle)
      ) {
        gridWord to rawClue.ifBlank { objectiveTitle }
      } else null
    }.distinctBy { it.first }

    // 2. Gather topic-matched bank words fitting gridSize & tier
    val topicBank = findTopicBank(objectiveTitle)
    val minTierAllowed = when (spec.tier) {
      WordSearchDifficultyTier.BEGINNER -> 2
      WordSearchDifficultyTier.EASY, WordSearchDifficultyTier.MEDIUM -> 3
      else -> 3
    }
    val orderedTopicWords = topicBank.words
      .filter { it.minTier <= minTierAllowed || spec.levelNumber > 50 }
      .sortedWith(
        compareBy<WordSearchTopicWord>(
          { if (spec.tier == WordSearchDifficultyTier.MASTER || spec.tier == WordSearchDifficultyTier.EXPERT) -it.minTier else it.minTier },
          { abs((it.enWord.hashCode() + spec.levelNumber * 31) % 97) }
        )
      )

    val topicPool = orderedTopicWords.mapNotNull { item ->
      val chosen = if (useNativeGridWord) (item.translations[langKey] ?: item.enWord) else item.enWord
      val gridWord = normalizeGridWord(chosen)
      if (gridWord.length in 3..spec.gridSize && gridWord !in excludedNorm) {
        val clueText = if (useNativeGridWord && langKey != "en") {
          "${item.clueEn} (${item.enWord.lowercase().replaceFirstChar { it.uppercase() }})"
        } else {
          item.clueEn
        }
        gridWord to clueText
      } else null
    }.distinctBy { it.first }

    // Cross-theme safe general expansion pool ONLY if a user has played so many levels on the same theme
    // that the theme's own 30-word bank was exhausted without repeating a word.
    val secondaryPool = if (filteredGemini.size + topicPool.size < spec.wordCount) {
      TOPIC_BANKS.filter { it.themeKey != topicBank.themeKey }
        .flatMap { it.words }
        .mapNotNull { item ->
          val chosen = if (useNativeGridWord) (item.translations[langKey] ?: item.enWord) else item.enWord
          val gridWord = normalizeGridWord(chosen)
          if (gridWord.length in 3..spec.gridSize && gridWord !in excludedNorm) {
            gridWord to item.clueEn
          } else null
        }
        .distinctBy { it.first }
    } else emptyList()

    val combinedPool = (filteredGemini + topicPool + secondaryPool).distinctBy { it.first }

    var bestResult: WordSearchBuildResult? = null
    for (attempt in 0 until 6) {
      val rotatedPool = if (attempt == 0 || combinedPool.size <= spec.wordCount) {
        combinedPool
      } else {
        val shift = (attempt * 3) % combinedPool.size
        combinedPool.drop(shift) + combinedPool.take(shift)
      }
      val candidateResult = buildProgressiveWordSearchGrid(
        candidates = rotatedPool,
        spec = spec,
        seed = spec.levelNumber * 37 + attempt * 53 + langKey.hashCode()
      )
      if (bestResult == null || candidateResult.placedWords.size > bestResult.placedWords.size) {
        bestResult = candidateResult
      }
      val validation = validateWordSearchBoard(
        grid = candidateResult.grid,
        targets = candidateResult.placedWords,
        spec = spec,
        objectiveTitle = objectiveTitle,
        excludedNormalizedWords = excludedNorm
      )
      if (validation.isValid) {
        bestResult = candidateResult
        break
      }
    }

    val finalResult = bestResult ?: WordSearchBuildResult(emptyList(), emptyList(), emptyList(), 0)
    if (persistUsedWords && finalResult.placedWords.isNotEmpty()) {
      recordUsedWordSearchWords(
        context = context,
        userId = userId,
        languageCode = languageCode,
        words = finalResult.placedWords.map { it.first }
      )
    }
    return finalResult
  }

  fun buildProgressiveWordSearchGrid(
    candidates: List<Pair<String, String>>,
    spec: WordSearchDifficultySpec,
    seed: Int
  ): WordSearchBuildResult {
    val gridSize = spec.gridSize.coerceIn(5, 16)
    val grid = Array(gridSize) { CharArray(gridSize) { ' ' } }
    val placed = mutableListOf<Pair<String, String>>()
    val placements = mutableListOf<WordSearchPlacement>()
    var totalOverlaps = 0

    // Allowed direction vectors (dRow, dCol) ordered per tier
    val allowedDirs = mutableListOf<Pair<Int, Int>>()
    allowedDirs.add(0 to 1) // Horizontal L->R always allowed
    if (spec.allowVertical) allowedDirs.add(1 to 0)
    if (spec.allowDiagonal) {
      allowedDirs.add(1 to 1)
      allowedDirs.add(1 to -1)
    }
    if (spec.allowReverse) {
      allowedDirs.add(0 to -1)
      if (spec.allowVertical) allowedDirs.add(-1 to 0)
      if (spec.allowDiagonal) {
        allowedDirs.add(-1 to -1)
        allowedDirs.add(-1 to 1)
      }
    }

    val cleanCandidates = candidates.mapNotNull { (word, clue) ->
      val norm = normalizeGridWord(word)
      if (norm.length in 3..gridSize) norm to clue else null
    }.distinctBy { it.first }

    // Sort longer words first on harder levels to guarantee snug placement & intersections
    val orderedCandidates = if (gridSize >= 9) {
      val initialSlice = cleanCandidates.take((spec.wordCount + 4).coerceAtMost(cleanCandidates.size))
      initialSlice.sortedByDescending { it.first.length }
    } else {
      cleanCandidates
    }

    for ((idx, pair) in orderedCandidates.withIndex()) {
      if (placed.size >= spec.wordCount) break
      val (word, clue) = pair

      // Beginner tiers stay mostly horizontal (at least 75% horizontal when <= 10)
      val preferredDirs = if (spec.tier == WordSearchDifficultyTier.BEGINNER && spec.allowVertical) {
        if ((idx + abs(seed)) % 4 == 3) listOf(1 to 0, 0 to 1) else listOf(0 to 1, 1 to 0)
      } else {
        val rot = abs(seed + idx * 7) % allowedDirs.size
        allowedDirs.drop(rot) + allowedDirs.take(rot)
      }

      var bestPlacement: Triple<WordSearchPlacement, Int, Boolean>? = null // (placement, overlaps, matchesPreferredDir)

      for ((dirOrderIdx, dir) in preferredDirs.withIndex()) {
        val (dr, dc) = dir
        for (r in 0 until gridSize) {
          for (c in 0 until gridSize) {
            val startR = (r + abs(seed + idx * 5)) % gridSize
            val startC = (c + abs(seed * 3 + idx * 11)) % gridSize
            val endR = startR + dr * (word.length - 1)
            val endC = startC + dc * (word.length - 1)
            if (endR !in 0 until gridSize || endC !in 0 until gridSize) continue

            var fits = true
            var overlapHits = 0
            for (k in word.indices) {
              val rr = startR + dr * k
              val cc = startC + dc * k
              val existing = grid[rr][cc]
              if (existing != ' ' && existing != word[k]) {
                fits = false
                break
              }
              if (existing == word[k]) overlapHits++
            }
            if (!fits) continue

            val placement = WordSearchPlacement(word, clue, startR, startC, dr, dc)
            val isPreferredDir = dirOrderIdx == 0
            if (spec.preferOverlaps) {
              val currentBestHits = bestPlacement?.second ?: -1
              if (overlapHits > currentBestHits || (overlapHits == currentBestHits && bestPlacement == null)) {
                bestPlacement = Triple(placement, overlapHits, isPreferredDir)
              }
              if (overlapHits > 0 && isPreferredDir) break
            } else {
              if (bestPlacement == null) {
                bestPlacement = Triple(placement, overlapHits, isPreferredDir)
              }
              break
            }
          }
          if (!spec.preferOverlaps && bestPlacement != null) break
          if (spec.preferOverlaps && (bestPlacement?.second ?: 0) > 0 && bestPlacement?.third == true) break
        }
        if (!spec.preferOverlaps && bestPlacement != null) break
      }

      if (bestPlacement != null) {
        val p = bestPlacement.first
        for (k in word.indices) {
          grid[p.startRow + p.dRow * k][p.startCol + p.dCol * k] = word[k]
        }
        placed.add(word to clue)
        placements.add(p)
        totalOverlaps += bestPlacement.second
      }
    }

    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    for (r in 0 until gridSize) {
      for (c in 0 until gridSize) {
        if (grid[r][c] == ' ') {
          val charIdx = abs(seed * 19 + r * 37 + c * 17 + gridSize * 7) % alphabet.length
          grid[r][c] = alphabet[charIdx]
        }
      }
    }

    return WordSearchBuildResult(
      grid = grid.map { String(it) },
      placedWords = placed,
      placements = placements,
      overlapCount = totalOverlaps
    )
  }

  data class ValidationOutcome(
    val isValid: Boolean,
    val failureReasons: List<String>
  )

  /**
   * Pre-display 7-point integrity verification for every Word Search board.
   */
  fun validateWordSearchBoard(
    grid: List<String>,
    targets: List<Pair<String, String>>,
    spec: WordSearchDifficultySpec,
    objectiveTitle: String,
    excludedNormalizedWords: Set<String> = emptySet()
  ): ValidationOutcome {
    val reasons = mutableListOf<String>()

    // 1. Exact square grid dimension & clean uppercase alphabetic cells
    if (grid.size != spec.gridSize || grid.any { it.length != spec.gridSize || it.any { ch -> ch !in 'A'..'Z' } }) {
      reasons.add("Grid must be ${spec.gridSize}x${spec.gridSize} uppercase A-Z.")
    }

    // 2. Word count matches level difficulty tier
    if (targets.size < spec.wordCount) {
      reasons.add("Expected at least ${spec.wordCount} words for level ${spec.levelNumber}, got ${targets.size}.")
    }

    // 3. No duplicates within level and no previously used words
    val normWords = targets.map { normalizeGridWord(it.first) }
    if (normWords.distinct().size != normWords.size) {
      reasons.add("Duplicate target word found inside level grid.")
    }
    val repeatedFromHistory = normWords.filter { it in excludedNormalizedWords }
    if (repeatedFromHistory.isNotEmpty()) {
      reasons.add("Repeated previously used words: $repeatedFromHistory")
    }

    // 4. Every target word is intact in a straight line inside grid following allowed directions
    val allowedDirs = buildList {
      add(0 to 1)
      if (spec.allowVertical) add(1 to 0)
      if (spec.allowDiagonal) {
        add(1 to 1)
        add(1 to -1)
      }
      if (spec.allowReverse) {
        add(0 to -1)
        if (spec.allowVertical) add(-1 to 0)
        if (spec.allowDiagonal) {
          add(-1 to -1)
          add(-1 to 1)
        }
      }
    }
    val cellWordMap = HashMap<Pair<Int, Int>, Int>()
    for (w in normWords) {
      // 5. Length fits grid cleanly
      if (w.length !in 3..spec.gridSize) {
        reasons.add("Word '$w' length ${w.length} out of range 3..${spec.gridSize}.")
        continue
      }
      var matchedCoords: List<Pair<Int, Int>>? = null
      outer@ for (r in grid.indices) {
        for (c in grid[r].indices) {
          for ((dr, dc) in allowedDirs) {
            val endR = r + dr * (w.length - 1)
            val endC = c + dc * (w.length - 1)
            if (endR !in grid.indices || endC !in grid[r].indices) continue
            var ok = true
            val coords = ArrayList<Pair<Int, Int>>(w.length)
            for (k in w.indices) {
              val rr = r + dr * k
              val cc = c + dc * k
              if (grid[rr][cc] != w[k]) {
                ok = false
                break
              }
              coords.add(rr to cc)
            }
            if (ok) {
              matchedCoords = coords
              break@outer
            }
          }
        }
      }
      if (matchedCoords == null) {
        reasons.add("Target word '$w' not found intact along allowed directions in grid.")
      } else {
        matchedCoords.forEach { coord ->
          cellWordMap[coord] = (cellWordMap[coord] ?: 0) + 1
        }
      }
    }

    // 6. Topic consistency
    val offTopic = targets.filter { (w, clue) -> !isCandidateTopicRelevant(w, clue, objectiveTitle) }
    if (offTopic.isNotEmpty()) {
      reasons.add("Off-topic words detected for '$objectiveTitle': ${offTopic.map { it.first }}")
    }

    // 7. Overlap verification on Expert/Master levels (if grid & words allow overlaps)
    if (spec.preferOverlaps && targets.size >= 8 && cellWordMap.values.none { it >= 2 }) {
      reasons.add("Expert/Master board expected at least 1 overlapping word intersection.")
    }

    return ValidationOutcome(isValid = reasons.isEmpty(), failureReasons = reasons)
  }

  fun validateWordSearchExercise(
    exercise: DynamicLevelExercise,
    levelNumber: Int
  ): Boolean {
    if (exercise.mechanic != GameInteractionMechanic.WORD_SEARCH_GRID) return true
    val spec = specForLevel(levelNumber)
    val outcome = validateWordSearchBoard(
      grid = exercise.wordSearchGrid,
      targets = exercise.wordSearchTargets,
      spec = spec,
      objectiveTitle = exercise.objectiveLabel
    )
    return outcome.isValid
  }

  /**
   * Line selection helper supporting Horizontal, Vertical, Diagonal, and Reverse sweeps.
   */
  fun resolveLineCoords(r1: Int, c1: Int, r2: Int, c2: Int): List<Pair<Int, Int>>? {
    val dr = r2 - r1
    val dc = c2 - c1
    if (dr == 0 && dc == 0) return listOf(r1 to c1)
    if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return null
    val steps = max(abs(dr), abs(dc))
    val stepR = dr.sign
    val stepC = dc.sign
    return (0..steps).map { k -> (r1 + stepR * k) to (c1 + stepC * k) }
  }
}
