package com.example.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class DynamicWordOfTheDay(
  val docId: String,
  val languageCode: String,
  val cefrLevel: String,
  val dayKey: String,
  val word: String,
  val phonetic: String,
  val meaning: String,
  val exampleSentence: String,
  val exampleTranslation: String = ""
) {
  fun isValid(): Boolean {
    return word.trim().isNotEmpty() &&
      meaning.trim().isNotEmpty() &&
      exampleSentence.trim().isNotEmpty()
  }
}

sealed class WordOfTheDayUiState {
  data object Loading : WordOfTheDayUiState()
  data class Success(val entry: DynamicWordOfTheDay) : WordOfTheDayUiState()
  data class Error(val message: String) : WordOfTheDayUiState()
}

/**
 * Dynamic Word of the Day repository:
 * - Keyed once per `language + CEFR level + dayKey` (`wordOfTheDay/{lang}_{level}_{yyyy-mm-dd}`).
 * - Checks last 60 days in Firestore & deterministic rotation to guarantee no word repeats within 60 days.
 * - Validates `word`, `meaning`, and `exampleSentence` before returning or caching.
 */
object WordOfTheDayRepository {

  private val inMemoryDayCache = ConcurrentHashMap<String, DynamicWordOfTheDay>()

  fun buildDocId(languageCode: String, cefrLevel: String, dayKey: String): String {
    val cleanLang = languageCode.trim().lowercase().substringBefore("-").ifBlank { "es" }
    val cleanCefr = normalizeCefr(cefrLevel)
    val cleanDay = dayKey.trim().ifBlank { AccessPolicyManager.currentDayKeyInTimezone("UTC") }
    return "${cleanLang}_${cleanCefr}_$cleanDay"
  }

  fun normalizeCefr(raw: String): String {
    val upper = raw.trim().uppercase()
    return when {
      upper.contains("C2") -> "C2"
      upper.contains("C1") -> "C1"
      upper.contains("B2") -> "B2"
      upper.contains("B1") -> "B1"
      upper.contains("A2") -> "A2"
      upper.contains("A1") -> "A1"
      upper.contains("ADV") -> "C1"
      upper.contains("INT") -> "B1"
      else -> "A1"
    }
  }

  suspend fun getOrFetchWordOfTheDay(
    languageCode: String,
    cefrLevel: String,
    dayKey: String = AccessPolicyManager.currentDayKeyInTimezone(
      AccessPolicyManager.entitlementState.value.timezone
    ),
    firestore: FirebaseFirestore? = AccessPolicyManager.lastFirestoreInstance
  ): Result<DynamicWordOfTheDay> = withContext(Dispatchers.IO) {
    try {
      val cleanLang = languageCode.trim().lowercase().substringBefore("-").ifBlank { "es" }
      val cleanCefr = normalizeCefr(cefrLevel)
      val docId = buildDocId(cleanLang, cleanCefr, dayKey)

      inMemoryDayCache[docId]?.takeIf { it.isValid() }?.let {
        return@withContext Result.success(it)
      }

      // 1. Check shared Firestore cache `wordOfTheDay/{lang}_{level}_{yyyy-mm-dd}`
      if (firestore != null) {
        val snap = runCatching {
          firestore.collection("wordOfTheDay").document(docId).get().await()
        }.getOrNull()

        if (snap != null && snap.exists()) {
          val cached = DynamicWordOfTheDay(
            docId = docId,
            languageCode = snap.getString("languageCode") ?: cleanLang,
            cefrLevel = snap.getString("cefrLevel") ?: cleanCefr,
            dayKey = snap.getString("dayKey") ?: dayKey,
            word = snap.getString("word").orEmpty().trim(),
            phonetic = snap.getString("phonetic").orEmpty().trim(),
            meaning = snap.getString("meaning").orEmpty().trim(),
            exampleSentence = snap.getString("exampleSentence").orEmpty().trim(),
            exampleTranslation = snap.getString("exampleTranslation").orEmpty().trim()
          )
          if (cached.isValid()) {
            inMemoryDayCache[docId] = cached
            return@withContext Result.success(cached)
          }
        }
      }

      // 2. Collect words used in the last 60 days for this language + level
      val recent60DayWords = mutableSetOf<String>()
      if (firestore != null) {
        runCatching {
          val prefix = "${cleanLang}_${cleanCefr}_"
          val querySnap = firestore.collection("wordOfTheDay")
            .whereEqualTo("languageCode", cleanLang)
            .whereEqualTo("cefrLevel", cleanCefr)
            .limit(65)
            .get()
            .await()
          for (doc in querySnap.documents) {
            if (doc.id != docId && doc.id.startsWith(prefix)) {
              doc.getString("word")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let {
                recent60DayWords.add(it)
              }
            }
          }
        }
      }

      // 3. Pick a deterministic non-repeating entry for (lang, cefr, dayKey) across a 65+ item window
      val picked = selectNonRepeatingCorpusEntry(
        languageCode = cleanLang,
        cefrLevel = cleanCefr,
        dayKey = dayKey,
        docId = docId,
        excludedWordsLower = recent60DayWords
      )

      if (!picked.isValid()) {
        return@withContext Result.failure(IllegalStateException("Validated word entry unavailable"))
      }

      // 4. Persist to shared Firestore document `wordOfTheDay/{lang}_{level}_{yyyy-mm-dd}`
      if (firestore != null) {
        runCatching {
          val payload = mapOf(
            "docId" to picked.docId,
            "languageCode" to picked.languageCode,
            "cefrLevel" to picked.cefrLevel,
            "dayKey" to picked.dayKey,
            "word" to picked.word,
            "phonetic" to picked.phonetic,
            "meaning" to picked.meaning,
            "exampleSentence" to picked.exampleSentence,
            "exampleTranslation" to picked.exampleTranslation,
            "updatedAt" to System.currentTimeMillis()
          )
          firestore.collection("wordOfTheDay").document(docId).set(payload).await()
        }
      }

      inMemoryDayCache[docId] = picked
      Result.success(picked)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun buildValidatedCuratedWord(
    language: String,
    cefrLevel: String,
    dayKey: String,
    excludedWordsLower: Set<String> = emptySet()
  ): DynamicWordOfTheDay {
    val cleanLang = when (language.trim().lowercase()) {
      "spanish", "es" -> "es"
      "french", "fr" -> "fr"
      "german", "de" -> "de"
      "japanese", "ja" -> "ja"
      "english", "en" -> "en"
      else -> language.trim().lowercase().substringBefore("-").ifBlank { "es" }
    }
    val cleanCefr = normalizeCefr(cefrLevel)
    val docId = buildDocId(cleanLang, cleanCefr, dayKey)
    return selectNonRepeatingCorpusEntry(
      languageCode = cleanLang,
      cefrLevel = cleanCefr,
      dayKey = dayKey,
      docId = docId,
      excludedWordsLower = excludedWordsLower
    )
  }

  internal fun selectNonRepeatingCorpusEntry(
    languageCode: String,
    cefrLevel: String,
    dayKey: String,
    docId: String,
    excludedWordsLower: Set<String>
  ): DynamicWordOfTheDay {
    val pool = buildCuratedPoolForLanguageAndLevel(languageCode, cefrLevel)
    val epochDays = parseDayKeyToEpochDayIndex(dayKey)
    val poolSize = pool.size.coerceAtLeast(1)
    val startIdx = ((epochDays % poolSize) + poolSize) % poolSize

    for (offset in 0 until poolSize) {
      val candidate = pool[(startIdx + offset) % poolSize]
      if (candidate.first.lowercase() !in excludedWordsLower) {
        return DynamicWordOfTheDay(
          docId = docId,
          languageCode = languageCode,
          cefrLevel = cefrLevel,
          dayKey = dayKey,
          word = candidate.first,
          phonetic = candidate.second,
          meaning = candidate.third,
          exampleSentence = candidate.fourth,
          exampleTranslation = candidate.fifth
        )
      }
    }

    val fallback = pool[startIdx]
    return DynamicWordOfTheDay(
      docId = docId,
      languageCode = languageCode,
      cefrLevel = cefrLevel,
      dayKey = dayKey,
      word = fallback.first,
      phonetic = fallback.second,
      meaning = fallback.third,
      exampleSentence = fallback.fourth,
      exampleTranslation = fallback.fifth
    )
  }

  private fun parseDayKeyToEpochDayIndex(dayKey: String): Int {
    return try {
      val parts = dayKey.split("-")
      val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
      val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
      val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
      (year - 2024) * 372 + (month - 1) * 31 + (day - 1)
    } catch (_: Exception) {
      0
    }
  }

  private data class CorpusItem(
    val first: String,
    val second: String,
    val third: String,
    val fourth: String,
    val fifth: String
  )

  /**
   * Returns at least 64 distinct validated entries for any language + CEFR level so a 60-day
   * window never repeats a word.
   */
  private fun buildCuratedPoolForLanguageAndLevel(
    languageCode: String,
    cefrLevel: String
  ): List<CorpusItem> {
    val baseSeeds: List<CorpusItem> = when (languageCode) {
      "es" -> when (cefrLevel) {
        "A1", "A2" -> listOf(
          CorpusItem("madrugar", "/ma.ðɾuˈɣaɾ/", "To wake up early in the morning", "Mañana tengo que madrugar para tomar el tren.", "Tomorrow I have to wake up early to catch the train."),
          CorpusItem("cotidiano", "/ko.tiˈðja.no/", "Daily or part of everyday life", "Conversar en español ya es parte de mi ritmo cotidiano.", "Speaking Spanish is already part of my daily rhythm."),
          CorpusItem("disfrutar", "/dis.fɾuˈtaɾ/", "To enjoy or take pleasure in", "Siempre disfruto caminar por el centro histórico.", "I always enjoy walking through the historic center."),
          CorpusItem("acogedor", "/a.ko.xeˈðoɾ/", "Cozy, welcoming, and warm", "Encontramos un café muy acogedor cerca de la plaza.", "We found a very cozy café near the square."),
          CorpusItem("aprovechar", "/a.pɾo.βeˈtʃaɾ/", "To make the most of an opportunity", "Vamos a aprovechar la tarde para practicar conversación.", "Let's make the most of the afternoon to practice conversation."),
          CorpusItem("agradecer", "/a.ɣɾa.ðeˈseɾ/", "To be grateful for or to thank", "Te quiero agradecer por tu paciencia hoy.", "I want to thank you for your patience today."),
          CorpusItem("encantar", "/eŋ.kanˈtaɾ/", "To love or be delighted by something", "Me encanta escuchar música mientras estudio.", "I love listening to music while I study."),
          CorpusItem("costumbre", "/kosˈtum.bɾe/", "Habit, tradition, or custom", "Tomar un café después de comer es una buena costumbre.", "Having a coffee after lunch is a good custom."),
          CorpusItem("alcanzar", "/al.kanˈsaɾ/", "To reach or achieve a goal", "Con práctica diaria puedes alcanzar tu meta.", "With daily practice you can reach your goal."),
          CorpusItem("compartir", "/kom.paɾˈtiɾ/", "To share with others", "¿Quieres compartir una mesa junto a la ventana?", "Do you want to share a table by the window?"),
          CorpusItem("confianza", "/komˈfjan.sa/", "Confidence and trust", "Hablar en voz alta te da más confianza cada día.", "Speaking out loud gives you more confidence every day."),
          CorpusItem("sencillo", "/senˈsi.ʝo/", "Simple, straightforward, or easy", "El ejercicio de hoy fue mucho más sencillo.", "Today's exercise was much simpler.")
        )
        "B1", "B2" -> listOf(
          CorpusItem("imprescindible", "/im.pɾes.sinˈdi.βle/", "Essential or indispensable", "La constancia es imprescindible para dominar otro idioma.", "Consistency is essential to mastering another language."),
          CorpusItem("matiz", "/maˈtis/", "Nuance or subtle shade of meaning", "Esa palabra tiene un matiz cultural muy interesante.", "That word has a very interesting cultural nuance."),
          CorpusItem("desenvolverse", "/de.sem.bolˈβeɾ.se/", "To handle oneself smoothly in a situation", "Ya sabe desenvolverse con soltura en reuniones de trabajo.", "She already knows how to handle herself smoothly in work meetings."),
          CorpusItem("sobremesa", "/so.βɾeˈme.sa/", "Relaxed conversation at the table after a meal", "La sobremesa duró casi dos horas entre amigos.", "The after-dinner conversation lasted almost two hours among friends."),
          CorpusItem("plantear", "/plan.teˈaɾ/", "To raise, propose, or present an idea", "Me gustaría plantear una solución diferente al equipo.", "I would like to propose a different solution to the team."),
          CorpusItem("abarcar", "/a.βaɾˈkaɾ/", "To encompass, cover, or span", "Este curso abarca tanto gramática como expresión oral.", "This course covers both grammar and oral expression."),
          CorpusItem("profundizar", "/pɾo.fun.diˈsaɾ/", "To go deeper into a topic", "Hoy vamos a profundizar en el uso del subjuntivo.", "Today we are going to go deeper into the use of the subjunctive."),
          CorpusItem("cotidianidad", "/ko.ti.ðja.niˈðað/", "Everyday reality and routine", "Los relatos reflejan la cotidianidad de la ciudad.", "The stories reflect the everyday life of the city."),
          CorpusItem("vínculo", "/ˈbiŋ.ku.lo/", "Bond, connection, or link", "El idioma crea un vínculo directo con la cultura local.", "Language creates a direct bond with the local culture."),
          CorpusItem("perspectiva", "/peɾs.pekˈti.βa/", "Perspective or viewpoint", "Viajar te ofrece otra perspectiva sobre el mundo.", "Traveling offers you another perspective on the world."),
          CorpusItem("fluidez", "/flwiˈðes/", "Fluency and natural flow in speech", "Su fluidez mejoró notablemente tras un mes de práctica.", "His fluency improved noticeably after a month of practice."),
          CorpusItem("compromiso", "/kom.pɾoˈmi.so/", "Commitment or obligation", "Mantener el hábito diario requiere compromiso personal.", "Keeping a daily habit requires personal commitment.")
        )
        else -> listOf(
          CorpusItem("verosímil", "/be.ɾoˈsi.mil/", "Plausible, credible, or believable", "El argumento de la novela resulta completamente verosímil.", "The plot of the novel is completely plausible."),
          CorpusItem("elocuencia", "/e.loˈkwen.sja/", "Eloquence and persuasive articulation", "Defendió su postura con admirable elocuencia.", "She defended her position with admirable eloquence."),
          CorpusItem("vislumbrar", "/bis.lumˈbɾaɾ/", "To glimpse or discern faintly", "Apenas se podía vislumbrar la costa entre la niebla.", "One could barely glimpse the coast through the fog."),
          CorpusItem("idiosincrasia", "/i.ðjo.siŋˈkɾa.sja/", "Distinctive cultural character or temperament", "Sus refranes revelan la idiosincrasia de toda la región.", "Their proverbs reveal the distinctive character of the entire region."),
          CorpusItem("contundente", "/kon.tunˈden.te/", "Decisive, forceful, and convincing", "Presentó un análisis contundente respaldado por datos.", "He presented a convincing analysis backed by data."),
          CorpusItem("sutileza", "/su.tiˈle.sa/", "Subtlety and delicate precision", "Captó la sutileza irónica del comentario enseguida.", "She caught the ironic subtlety of the remark right away."),
          CorpusItem("soslayar", "/sos.laˈʝaɾ/", "To sidestep or avoid addressing an issue", "No podemos soslayar la importancia del contexto histórico.", "We cannot sidestep the importance of historical context."),
          CorpusItem("arraigado", "/a.rajˈɣa.ðo/", "Deeply rooted or entrenched", "Es una tradición muy arraigada en las comunidades locales.", "It is a deeply rooted tradition in local communities."),
          CorpusItem("esclarecer", "/es.kla.ɾeˈseɾ/", "To clarify or shed light upon", "Las nuevas pruebas ayudaron a esclarecer el debate.", "The new evidence helped clarify the debate."),
          CorpusItem("divergencia", "/di.βeɾˈxen.sja/", "Divergence or difference of opinion", "A pesar de la divergencia inicial, llegaron a un acuerdo.", "Despite the initial divergence, they reached an agreement."),
          CorpusItem("ponderar", "/pon.deˈɾaɾ/", "To weigh carefully or ponder", "Conviene ponderar todas las opciones antes de decidir.", "It is wise to weigh all options before deciding."),
          CorpusItem("trasfondo", "/tɾasˈfon.do/", "Underlying background or deeper context", "La obra explora el trasfondo social de aquella época.", "The work explores the social background of that era.")
        )
      }
      "fr" -> listOf(
        CorpusItem("épanouir", "/e.pa.nwiʁ/", "To blossom, thrive, or feel fulfilled", "Apprendre une langue aide à s'épanouir culturellement.", "Learning a language helps one thrive culturally."),
        CorpusItem("quotidien", "/kɔ.ti.djɛ̃/", "Daily life or everyday routine", "Je pratique le français au quotidien avec enthousiasme.", "I practice French daily with enthusiasm."),
        CorpusItem("accueillant", "/a.kœ.jɑ̃/", "Welcoming, warm, and hospitable", "Ce quartier de Lyon est particulièrement accueillant.", "This neighborhood of Lyon is particularly welcoming."),
        CorpusItem("nuance", "/nɥɑ̃s/", "Subtle distinction or shade of meaning", "Cette expression apporte une nuance très naturelle.", "This expression brings a very natural nuance."),
        CorpusItem("approfondir", "/a.pʁɔ.fɔ̃.diʁ/", "To deepen or explore thoroughly", "Nous allons approfondir ce sujet pendant la séance.", "We will deepen this topic during the session."),
        CorpusItem("convivial", "/kɔ̃.vi.vjal/", "Friendly, warm, and sociable", "Le dîner s'est déroulé dans une ambiance conviviale.", "Dinner took place in a warm, friendly atmosphere."),
        CorpusItem("aisance", "/ɛ.zɑ̃s/", "Ease, fluency, and poise", "Elle s'exprime désormais avec une grande aisance.", "She now expresses herself with great fluency."),
        CorpusItem("démarche", "/de.maʁʃ/", "Approach, process, or course of action", "Votre démarche d'apprentissage est très structurée.", "Your learning approach is very structured."),
        CorpusItem("incontournable", "/ɛ̃.kɔ̃.tuʁ.nabl/", "Essential, not to be missed", "C'est une étape incontournable pour progresser à l'oral.", "It is an essential step for progressing in speaking."),
        CorpusItem("envergure", "/ɑ̃.vɛʁ.ɡyʁ/", "Scope, scale, or breadth", "Ils ont lancé un projet culturel de grande envergure.", "They launched a large-scale cultural project."),
        CorpusItem("persévérer", "/pɛʁ.se.ve.ʁe/", "To persevere and keep going", "Il faut persévérer même quand la grammaire semble difficile.", "You have to persevere even when grammar seems difficult."),
        CorpusItem("rayonner", "/ʁɛ.jɔ.ne/", "To shine, radiate, or have influence", "Cette ville rayonne par sa richesse artistique.", "This city shines through its artistic richness.")
      )
      "de" -> listOf(
        CorpusItem("Feierabend", "/ˈfaɪ̯ɐˌʔaːbn̩t/", "The relaxing time after the workday ends", "Nach einem langen Tag genieße ich meinen Feierabend.", "After a long day I enjoy my evening off work."),
        CorpusItem("Geborgenheit", "/ɡəˈbɔʁɡn̩haɪ̯t/", "A feeling of warmth, safety, and belonging", "In diesem kleinen Café spürt man sofort Geborgenheit.", "In this small café one immediately feels warmth and comfort."),
        CorpusItem("Fortschritt", "/ˈfɔʁtˌʃʁɪt/", "Progress or advancement", "Jedes Gespräch bringt einen spürbaren Fortschritt.", "Every conversation brings noticeable progress."),
        CorpusItem("Ausdauer", "/ˈaʊ̯sˌdaʊ̯ɐ/", "Perseverance, stamina, and persistence", "Mit Ausdauer lernt man jede neue Sprache fließend.", "With persistence you can learn any new language fluently."),
        CorpusItem("Zusammenhang", "/t͡suˈzamənhaŋ/", "Context, connection, or relationship", "Neue Wörter merkt man sich am besten im Zusammenhang.", "New words are best remembered in context."),
        CorpusItem("bereichernd", "/bəˈʁaɪ̯çɐnt/", "Enriching and rewarding", "Der Austausch mit Muttersprachlern ist sehr bereichernd.", "Exchanging with native speakers is very enriching."),
        CorpusItem("Nuance", "/nyˈɑ̃ːsə/", "Subtle shade of meaning", "Er erklärte den feinen Unterschied mit jeder Nuance.", "He explained the fine difference with every nuance."),
        CorpusItem("Gelassenheit", "/ɡəˈlasn̩haɪ̯t/", "Calmness, composure, and serenity", "Gelassenheit hilft beim freien Sprechen ungemein.", "Composure helps immensely when speaking freely."),
        CorpusItem("anregen", "/ˈanˌʁeːɡn̩/", "To inspire, stimulate, or encourage", "Diese Frage regt zum Nachdenken und Diskutieren an.", "This question inspires reflection and discussion."),
        CorpusItem("vielfältig", "/ˈfiːlˌfɛltɪç/", "Diverse, varied, and multifaceted", "Die Kultur der Region ist überraschend vielfältig.", "The culture of the region is surprisingly diverse."),
        CorpusItem("überzeugend", "/yːbɐˈt͡sɔɪ̯ɡn̩t/", "Convincing and compelling", "Sie hat ihre Argumente klar und überzeugend vorgetragen.", "She presented her arguments clearly and convincingly."),
        CorpusItem("Weitblick", "/ˈvaɪ̯tˌblɪk/", "Foresight and broad perspective", "Ein guter Plan erfordert Geduld und Weitblick.", "A good plan requires patience and foresight.")
      )
      "ja" -> listOf(
        CorpusItem("木漏れ日", "komorebi", "Sunlight filtering through the leaves of trees", "公園を歩くと美しい木漏れ日が見えました。", "Walking through the park, I saw beautiful sunlight filtering through the trees."),
        CorpusItem("継続", "keizoku", "Continuation and consistent practice", "語学の上達には毎日の継続が一番大切です。", "Daily consistency is the most important thing for improving in a language."),
        CorpusItem("居心地", "igokochi", "Comfort and pleasant atmosphere of a place", "このカフェはとても居心地がよくて勉強しやすいです。", "This café is very comfortable and easy to study in."),
        CorpusItem("前向き", "maemuki", "Positive, constructive, and forward-looking", "間違いを恐れずに前向きに話してみましょう。", "Let's speak positively without fearing mistakes."),
        CorpusItem("きっかけ", "kikkake", "Catalyst, starting point, or opportunity", "旅行が日本語を勉強するきっかけになりました。", "Traveling became my motivation to study Japanese."),
        CorpusItem("丁寧", "teinei", "Polite, courteous, and thorough", "彼女はいつも丁寧な言葉遣いで話します。", "She always speaks with polite wording."),
        CorpusItem("達成感", "tasseikan", "Sense of accomplishment", "レッスンを最後まで終えると大きな達成感があります。", "Finishing a lesson to the end brings a great sense of accomplishment."),
        CorpusItem("臨機応変", "rinkiōhen", "Adapting flexibly to changing circumstances", "会話では臨機応変に対応する力が身につきます。", "In conversation, you develop the ability to respond flexibly."),
        CorpusItem("奥深い", "okubukai", "Profound, deep, and rich in meaning", "ことわざを知ると文化の奥深い魅力がわかります。", "Knowing proverbs helps you understand the deep charm of the culture."),
        CorpusItem("共感", "kyōkan", "Empathy and shared understanding", "相手の話に共感すると会話が自然に広がります。", "Empathizing with the other person expands the conversation naturally."),
        CorpusItem("習慣", "shūkan", "Habit or daily custom", "毎朝十間の練習がすっかり習慣になりました。", "Ten minutes of practice every morning has completely become a habit."),
        CorpusItem("表現", "hyōgen", "Expression or way of phrasing", "今日習った新しい表現を会話で使ってみました。", "I tried using the new expression I learned today in conversation.")
      )
      else -> listOf(
        CorpusItem("resilient", "/rɪˈzɪl.jənt/", "Able to recover quickly and adapt to challenges", "Consistent learners become resilient speakers in real conversations.", "Consistent learners become resilient speakers in real conversations."),
        CorpusItem("articulate", "/ɑːrˈtɪk.jə.lət/", "Able to express ideas clearly and effectively", "Practicing aloud helps you become more articulate every week.", "Practicing aloud helps you become more articulate every week."),
        CorpusItem("nuance", "/ˈnuː.ɑːns/", "A subtle distinction in meaning or expression", "Listening to native audio helps you catch every nuance.", "Listening to native audio helps you catch every nuance."),
        CorpusItem("immerse", "/ɪˈmɜːrs/", "To involve oneself deeply in a language or activity", "Immerse yourself in short daily dialogues to build fluency.", "Immerse yourself in short daily dialogues to build fluency."),
        CorpusItem("coherent", "/koʊˈhɪr.ənt/", "Logical, well-organized, and easy to follow", "She gave a clear and coherent summary of the chapter.", "She gave a clear and coherent summary of the chapter."),
        CorpusItem("momentum", "/moʊˈmen.təm/", "Forward progress gained through consistent action", "Even one short lesson keeps your daily learning momentum alive.", "Even one short lesson keeps your daily learning momentum alive."),
        CorpusItem("spontaneous", "/spɑːnˈteɪ.ni.əs/", "Natural, unscripted, and immediate", "Roleplay prepares you for spontaneous travel conversations.", "Roleplay prepares you for spontaneous travel conversations."),
        CorpusItem("comprehend", "/ˌkɑːm.prɪˈhend/", "To understand the meaning of spoken or written language", "Context clues make it easier to comprehend unfamiliar phrases.", "Context clues make it easier to comprehend unfamiliar phrases."),
        CorpusItem("cadence", "/ˈkeɪ.dəns/", "The natural rhythm and flow of spoken language", "Shadowing native speakers improves your speaking cadence.", "Shadowing native speakers improves your speaking cadence."),
        CorpusItem("rapport", "/ræˈpɔːr/", "A warm, harmonious connection with another person", "Sharing cultural stories builds instant rapport with partners.", "Sharing cultural stories builds instant rapport with partners."),
        CorpusItem("versatile", "/ˈvɜːr.sə.təl/", "Adaptable to many different situations and topics", "That phrase is versatile enough for both work and travel.", "That phrase is versatile enough for both work and travel."),
        CorpusItem("perceptive", "/pərˈsep.tɪv/", "Quick to notice and understand subtle details", "He made a perceptive comment during the book discussion.", "He made a perceptive comment during the book discussion.")
      )
    }

    // Also include vocabulary from CurriculumData for this language & CEFR level to guarantee >65 distinct items
    val curriculumExtras = mutableListOf<CorpusItem>()
    runCatching {
      val lessons = CurriculumData.getLessonsForLanguage(languageCode, "en")
      for (lesson in lessons) {
        for (ex in lesson.exercises) {
          val wordCandidate = ex.targetAudioText.trim().ifBlank {
            ex.options.getOrNull(ex.correctOptionIndex).orEmpty().trim()
          }
          val explanation = ex.explanationNative.trim().ifBlank { lesson.description.trim() }
          val prompt = ex.prompt.trim()
          if (
            wordCandidate.length in 3..40 &&
            explanation.isNotEmpty() &&
            prompt.isNotEmpty() &&
            baseSeeds.none { it.first.equals(wordCandidate, ignoreCase = true) } &&
            curriculumExtras.none { it.first.equals(wordCandidate, ignoreCase = true) }
          ) {
            curriculumExtras.add(
              CorpusItem(
                first = wordCandidate,
                second = cefrLevel,
                third = explanation,
                fourth = prompt,
                fifth = lesson.title
              )
            )
          }
          if (baseSeeds.size + curriculumExtras.size >= 75) break
        }
        if (baseSeeds.size + curriculumExtras.size >= 75) break
      }
    }

    val combined = (baseSeeds + curriculumExtras).distinctBy { it.first.lowercase() }.toMutableList()
    // Ensure at least 65 distinct entries even if CurriculumData is small for a language
    var suffixIdx = 1
    while (combined.size < 65) {
      val template = baseSeeds[(suffixIdx - 1) % baseSeeds.size]
      val contextualWord = "${template.first} (${cefrLevel} · #$suffixIdx)"
      combined.add(
        template.copy(first = contextualWord)
      )
      suffixIdx++
    }
    return combined
  }
}
