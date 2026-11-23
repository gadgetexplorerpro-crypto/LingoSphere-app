package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.UserAccountSettings
import com.example.data.model.UserNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SmartNotificationCategory(
  val id: String,
  val channelId: String,
  val channelName: String,
  val channelDescription: String
) {
  WORD_OF_THE_DAY(
    id = "word_of_the_day",
    channelId = "lingosphere_word_of_the_day_v1",
    channelName = "Daily English Word of the Day",
    channelDescription = "Daily English vocabulary words with meaning, pronunciation, and example sentences"
  ),
  VOCAB_CHALLENGE(
    id = "vocab_challenge",
    channelId = "lingosphere_vocab_challenge_v1",
    channelName = "Daily Vocabulary Challenges",
    channelDescription = "Interactive daily English vocabulary quizzes and flashcard challenges"
  ),
  GRAMMAR_TIP(
    id = "grammar_tip",
    channelId = "lingosphere_grammar_tips_v1",
    channelName = "English Grammar Tips",
    channelDescription = "Bite-sized English grammar rules and real-world usage examples"
  ),
  SPEAKING_REMINDER(
    id = "speaking_reminder",
    channelId = "lingosphere_speaking_reminders_v1",
    channelName = "Speaking Practice Reminders",
    channelDescription = "Reminders to practice English speaking and maintain your fluency streak"
  ),
  SPEAKING_INVITATION(
    id = "speaking_invitation",
    channelId = "lingosphere_speaking_invitations_v1",
    channelName = "Speaking Invitations & Partner Alerts",
    channelDescription = "Real-time notifications when a language partner invites you to speak or chat"
  ),
  MISSED_CALL(
    id = "missed_call",
    channelId = "lingosphere_missed_calls_v1",
    channelName = "Missed Speaking Calls",
    channelDescription = "Alerts when you miss an incoming voice or video speaking call"
  ),
  STREAK_REMINDER(
    id = "streak_reminder",
    channelId = "lingosphere_streak_reminders_v1",
    channelName = "Daily Streak Reminders",
    channelDescription = "Friendly daily reminders at your chosen time to help you maintain your learning streak"
  )
}

data class WordOfTheDayEntry(
  val word: String,
  val meaning: String,
  val pronunciation: String,
  val exampleSentence: String,
  val lessonId: String = "en_vocab_1"
)

data class VocabChallengeEntry(
  val id: String,
  val title: String,
  val challengeQuestion: String,
  val targetWords: String,
  val lessonId: String = "en_vocab_1"
)

data class GrammarTipEntry(
  val id: String,
  val title: String,
  val tipExplanation: String,
  val exampleCorrect: String,
  val lessonId: String = "en_gram_1"
)

data class SpeakingReminderEntry(
  val id: String,
  val title: String,
  val promptMessage: String,
  val scenarioTitle: String
)

data class StreakReminderEntry(
  val id: String,
  val title: String,
  val friendlyMessage: String,
  val actionTip: String
)

data class SmartNotificationDeepLink(
  val route: String, // word_of_the_day, vocab_challenge, grammar_tip, speaking_reminder, streak_reminder, speaking_invitation, missed_call
  val word: String = "",
  val meaning: String = "",
  val pronunciation: String = "",
  val exampleSentence: String = "",
  val lessonId: String = "",
  val partnerId: String = "",
  val partnerName: String = "",
  val roomId: String = ""
)

object SmartNotificationContract {
  const val ACTION_OPEN_SMART_NOTIFICATION = "com.example.lingosphere.action.OPEN_SMART_NOTIFICATION"
  const val ACTION_TRIGGER_SCHEDULED_NOTIFICATION = "com.example.lingosphere.action.TRIGGER_SCHEDULED_NOTIFICATION"

  const val EXTRA_ROUTE = "extra_smart_notif_route"
  const val EXTRA_WORD = "extra_smart_notif_word"
  const val EXTRA_MEANING = "extra_smart_notif_meaning"
  const val EXTRA_PRONUNCIATION = "extra_smart_notif_pronunciation"
  const val EXTRA_EXAMPLE = "extra_smart_notif_example"
  const val EXTRA_LESSON_ID = "extra_smart_notif_lesson_id"
  const val EXTRA_PARTNER_ID = "extra_smart_notif_partner_id"
  const val EXTRA_PARTNER_NAME = "extra_smart_notif_partner_name"
  const val EXTRA_ROOM_ID = "extra_smart_notif_room_id"
  const val EXTRA_SCHEDULED_CATEGORY = "extra_scheduled_category"

  fun applyToIntent(intent: Intent, deepLink: SmartNotificationDeepLink): Intent {
    return intent.apply {
      action = ACTION_OPEN_SMART_NOTIFICATION
      putExtra(EXTRA_ROUTE, deepLink.route)
      putExtra(EXTRA_WORD, deepLink.word)
      putExtra(EXTRA_MEANING, deepLink.meaning)
      putExtra(EXTRA_PRONUNCIATION, deepLink.pronunciation)
      putExtra(EXTRA_EXAMPLE, deepLink.exampleSentence)
      putExtra(EXTRA_LESSON_ID, deepLink.lessonId)
      putExtra(EXTRA_PARTNER_ID, deepLink.partnerId)
      putExtra(EXTRA_PARTNER_NAME, deepLink.partnerName)
      putExtra(EXTRA_ROOM_ID, deepLink.roomId)
    }
  }

  fun fromIntent(intent: Intent?): SmartNotificationDeepLink? {
    if (intent == null) return null
    val route = intent.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() } ?: return null
    return SmartNotificationDeepLink(
      route = route,
      word = intent.getStringExtra(EXTRA_WORD).orEmpty(),
      meaning = intent.getStringExtra(EXTRA_MEANING).orEmpty(),
      pronunciation = intent.getStringExtra(EXTRA_PRONUNCIATION).orEmpty(),
      exampleSentence = intent.getStringExtra(EXTRA_EXAMPLE).orEmpty(),
      lessonId = intent.getStringExtra(EXTRA_LESSON_ID).orEmpty(),
      partnerId = intent.getStringExtra(EXTRA_PARTNER_ID).orEmpty(),
      partnerName = intent.getStringExtra(EXTRA_PARTNER_NAME).orEmpty(),
      roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
    )
  }
}

object SmartEnglishContentCatalog {
  val WORDS_OF_THE_DAY: List<WordOfTheDayEntry> = listOf(
    WordOfTheDayEntry(
      word = "intriguing",
      meaning = "Very interesting or mysterious.",
      pronunciation = "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)",
      exampleSentence = "That's an intriguing idea!",
      lessonId = "en_vocab_1"
    ),
    WordOfTheDayEntry(
      word = "eloquent",
      meaning = "Fluent, persuasive, and expressive in speaking or writing.",
      pronunciation = "/ˈɛləkwənt/ (EL-uh-kwunt)",
      exampleSentence = "She gave an eloquent speech at the international summit.",
      lessonId = "en_conv_1"
    ),
    WordOfTheDayEntry(
      word = "resilient",
      meaning = "Able to recover quickly from difficult conditions or setbacks.",
      pronunciation = "/rɪˈzɪliənt/ (ri-ZIL-yunt)",
      exampleSentence = "Consistent daily practice builds a resilient language learner.",
      lessonId = "en_vocab_1"
    ),
    WordOfTheDayEntry(
      word = "articulate",
      meaning = "Having or showing the ability to speak fluently and coherently.",
      pronunciation = "/ɑːrˈtɪkjʊlɪt/ (ar-TIK-yuh-lit)",
      exampleSentence = "He is an articulate speaker who explains complex ideas clearly.",
      lessonId = "en_conv_1"
    ),
    WordOfTheDayEntry(
      word = "pragmatic",
      meaning = "Dealing with things sensibly and realistically in a practical way.",
      pronunciation = "/præɡˈmætɪk/ (prag-MAT-ik)",
      exampleSentence = "We took a pragmatic approach to solving the team's schedule.",
      lessonId = "en_gram_1"
    ),
    WordOfTheDayEntry(
      word = "serendipity",
      meaning = "The occurrence of happy or beneficial events by chance.",
      pronunciation = "/ˌsɛrənˈdɪpɪti/ (ser-uhn-DIP-i-tee)",
      exampleSentence = "Meeting my speaking partner in Madrid was pure serendipity.",
      lessonId = "en_vocab_1"
    ),
    WordOfTheDayEntry(
      word = "meticulous",
      meaning = "Showing great attention to detail; very careful and precise.",
      pronunciation = "/məˈtɪkjʊləs/ (muh-TIK-yuh-lus)",
      exampleSentence = "She keeps meticulous notes of every new English idiom she hears.",
      lessonId = "en_vocab_1"
    )
  )

  val VOCAB_CHALLENGES: List<VocabChallengeEntry> = listOf(
    VocabChallengeEntry(
      id = "vc_context_1",
      title = "Daily Vocabulary Challenge: Context Mastery",
      challengeQuestion = "Can you use 'intriguing', 'eloquent', and 'pragmatic' in a 60-second sprint?",
      targetWords = "intriguing • eloquent • pragmatic",
      lessonId = "en_vocab_1"
    ),
    VocabChallengeEntry(
      id = "vc_idioms_2",
      title = "Daily Vocabulary Challenge: Business & Travel",
      challengeQuestion = "Match 5 essential travel & professional collocations before the timer runs out!",
      targetWords = "articulate • resilient • meticulous",
      lessonId = "en_conv_1"
    ),
    VocabChallengeEntry(
      id = "vc_synonyms_3",
      title = "Daily Vocabulary Challenge: Precision Synonyms",
      challengeQuestion = "Upgrade basic adjectives to C1-level English expressions in today's quiz.",
      targetWords = "serendipity • versatile • candid",
      lessonId = "en_vocab_1"
    )
  )

  val GRAMMAR_TIPS: List<GrammarTipEntry> = listOf(
    GrammarTipEntry(
      id = "gt_present_perfect",
      title = "Grammar Tip: Present Perfect vs. Past Simple",
      tipExplanation = "Use Present Perfect ('I have lived here for 3 years') for actions continuing now, and Past Simple ('I lived there in 2020') for finished past times.",
      exampleCorrect = "I have practiced English every day this week.",
      lessonId = "en_gram_1"
    ),
    GrammarTipEntry(
      id = "gt_conditionals",
      title = "Grammar Tip: Second Conditional for Polite & Hypothetical Ideas",
      tipExplanation = "Use 'If + Past Simple, would + base verb' to talk about imaginary or polite situations.",
      exampleCorrect = "If I had more free time, I would practice speaking every evening.",
      lessonId = "en_gram_1"
    ),
    GrammarTipEntry(
      id = "gt_prepositions",
      title = "Grammar Tip: 'Interested IN' vs. 'Excited ABOUT'",
      tipExplanation = "Always pair 'interested' with 'in' + noun/-ing, and 'excited' with 'about' + noun/-ing.",
      exampleCorrect = "That's an intriguing topic — I'm really interested in learning more!",
      lessonId = "en_gram_1"
    )
  )

  val SPEAKING_REMINDERS: List<SpeakingReminderEntry> = listOf(
    SpeakingReminderEntry(
      id = "sr_daily_warmup",
      title = "Time for Your 5-Minute English Speaking Practice!",
      promptMessage = "Speak out loud with the AI Voice Coach or a live partner to keep your pronunciation sharp.",
      scenarioTitle = "Daily Pronunciation & Fluency Warmup"
    ),
    SpeakingReminderEntry(
      id = "sr_partner_exchange",
      title = "Ready for a Live English Conversation?",
      promptMessage = "Connect with a speaking partner or practice real-time roleplay to build confidence.",
      scenarioTitle = "Coffee Shop & Travel Conversation"
    )
  )

  val STREAK_REMINDERS: List<StreakReminderEntry> = listOf(
    StreakReminderEntry(
      id = "streak_friendly_1",
      title = "Keep Your Learning Streak Glowing! 🔥",
      friendlyMessage = "Just 5 minutes of practice today keeps your momentum strong and builds lasting fluency.",
      actionTip = "Complete one quick lesson or speaking warmup to lock in today's streak."
    ),
    StreakReminderEntry(
      id = "streak_friendly_2",
      title = "Ready for Today's Language Win? 🌟",
      friendlyMessage = "Small daily steps lead to big breakthroughs! Your personalized lesson is waiting for you.",
      actionTip = "Jump in for a 3-minute vocabulary or listening sprint to protect your streak."
    ),
    StreakReminderEntry(
      id = "streak_friendly_3",
      title = "Friendly Check-In: Time to Practice! 💬",
      friendlyMessage = "Consistency is your superpower. Take a quick moment to practice a few phrases aloud.",
      actionTip = "Open LingoSphere now to keep your daily learning habit unbroken."
    ),
    StreakReminderEntry(
      id = "streak_friendly_4",
      title = "Don't Let Your Streak Cool Down! 🚀",
      friendlyMessage = "You've been doing great! A quick review session now will keep your brain wired for fluency.",
      actionTip = "Tap below to continue your next recommended lesson."
    )
  )

  fun getTodayWordOfTheDay(preferFeaturedIntriguing: Boolean = false): WordOfTheDayEntry {
    if (preferFeaturedIntriguing) return WORDS_OF_THE_DAY.first()
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return WORDS_OF_THE_DAY[dayOfYear % WORDS_OF_THE_DAY.size]
  }

  fun getTodayVocabChallenge(): VocabChallengeEntry {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return VOCAB_CHALLENGES[dayOfYear % VOCAB_CHALLENGES.size]
  }

  fun getTodayGrammarTip(): GrammarTipEntry {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return GRAMMAR_TIPS[dayOfYear % GRAMMAR_TIPS.size]
  }

  fun getTodaySpeakingReminder(): SpeakingReminderEntry {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return SPEAKING_REMINDERS[dayOfYear % SPEAKING_REMINDERS.size]
  }

  fun getTodayStreakReminder(streakDays: Int = 0, userName: String = ""): StreakReminderEntry {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val base = STREAK_REMINDERS[dayOfYear % STREAK_REMINDERS.size]
    val cleanName = userName.trim().split(" ").firstOrNull().orEmpty()
    val personalizedTitle = when {
      streakDays > 0 && cleanName.isNotBlank() ->
        "Hey $cleanName, keep your $streakDays-day streak alive! 🔥"
      streakDays > 0 ->
        "Keep your $streakDays-day learning streak alive! 🔥"
      cleanName.isNotBlank() ->
        "Hey $cleanName, time for your daily language win! 🌟"
      else -> base.title
    }
    return base.copy(title = personalizedTitle)
  }

  fun findWordByText(word: String): WordOfTheDayEntry? {
    if (word.isBlank()) return null
    return WORDS_OF_THE_DAY.firstOrNull { it.word.equals(word.trim(), ignoreCase = true) }
  }
}

object SmartNotificationLaunchRegistry {
  private val _deepLinkFlow = kotlinx.coroutines.flow.MutableStateFlow<SmartNotificationDeepLink?>(null)
  val deepLinkFlow: kotlinx.coroutines.flow.StateFlow<SmartNotificationDeepLink?> = _deepLinkFlow

  fun dispatchDeepLink(deepLink: SmartNotificationDeepLink) {
    _deepLinkFlow.value = deepLink
  }

  fun consumeDeepLink() {
    _deepLinkFlow.value = null
  }
}

/**
 * Prevents repeated notifications and unnecessary spam by tracking delivered keys and cooldowns.
 */
object NotificationDeduplicator {
  private const val PREFS_NAME = "lingosphere_smart_notif_dedup"
  private const val MIN_CATEGORY_COOLDOWN_MS = 15_000L

  fun shouldDeliver(
    context: Context,
    uniqueKey: String,
    category: SmartNotificationCategory,
    bypassCooldown: Boolean = false
  ): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()

    // 1. Exact deduplication key check (e.g. notif ID or daily key)
    val existingTs = prefs.getLong("key_$uniqueKey", 0L)
    if (existingTs > 0L) {
      return false
    }

    // 2. Per-category anti-spam cooldown check
    if (!bypassCooldown) {
      val lastCatTs = prefs.getLong("cat_last_${category.id}", 0L)
      if (lastCatTs > 0L && (now - lastCatTs) < MIN_CATEGORY_COOLDOWN_MS) {
        return false
      }
    }

    prefs.edit()
      .putLong("key_$uniqueKey", now)
      .putLong("cat_last_${category.id}", now)
      .apply()
    return true
  }

  fun todayDateKey(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}

object SmartNotificationManager {
  private const val TAG = "SmartNotifManager"
  private const val SETTINGS_PREFS = "lingosphere_account_settings"

  fun ensureChannelsCreated(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    SmartNotificationCategory.entries.forEach { category ->
      val importance = when (category) {
        SmartNotificationCategory.SPEAKING_INVITATION,
        SmartNotificationCategory.MISSED_CALL -> NotificationManager.IMPORTANCE_HIGH
        else -> NotificationManager.IMPORTANCE_DEFAULT
      }
      val channel = NotificationChannel(
        category.channelId,
        category.channelName,
        importance
      ).apply {
        description = category.channelDescription
        enableVibration(true)
      }
      manager.createNotificationChannel(channel)
    }
  }

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return try {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) {
      true
    }
  }

  fun isCategoryEnabled(context: Context, category: SmartNotificationCategory): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    return when (category) {
      SmartNotificationCategory.WORD_OF_THE_DAY ->
        prefs.getBoolean("wordOfTheDayNotificationsEnabled", true)
      SmartNotificationCategory.VOCAB_CHALLENGE ->
        prefs.getBoolean("dailyVocabChallengeNotificationsEnabled", true)
      SmartNotificationCategory.GRAMMAR_TIP ->
        prefs.getBoolean("grammarTipNotificationsEnabled", true)
      SmartNotificationCategory.SPEAKING_REMINDER ->
        prefs.getBoolean("speakingReminderNotificationsEnabled", true) &&
          prefs.getBoolean("studyRemindersEnabled", true)
      SmartNotificationCategory.SPEAKING_INVITATION ->
        prefs.getBoolean("speakingInvitationNotificationsEnabled", true)
      SmartNotificationCategory.MISSED_CALL ->
        prefs.getBoolean("callNotificationsEnabled", true)
      SmartNotificationCategory.STREAK_REMINDER ->
        prefs.getBoolean("streakAlertsEnabled", true) &&
          prefs.getBoolean("studyRemindersEnabled", true)
    }
  }

  fun syncSettingsToPreferences(context: Context, settings: UserAccountSettings) {
    context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putBoolean("wordOfTheDayNotificationsEnabled", settings.wordOfTheDayNotificationsEnabled)
      .putBoolean("dailyVocabChallengeNotificationsEnabled", settings.dailyVocabChallengeNotificationsEnabled)
      .putBoolean("grammarTipNotificationsEnabled", settings.grammarTipNotificationsEnabled)
      .putBoolean("speakingReminderNotificationsEnabled", settings.speakingReminderNotificationsEnabled)
      .putBoolean("speakingInvitationNotificationsEnabled", settings.speakingInvitationNotificationsEnabled)
      .putBoolean("callNotificationsEnabled", settings.callNotificationsEnabled)
      .putBoolean("studyRemindersEnabled", settings.studyRemindersEnabled)
      .putBoolean("streakAlertsEnabled", settings.streakAlertsEnabled)
      .putInt("dailyReminderHour", settings.dailyReminderHour.coerceIn(0, 23))
      .putInt("dailyReminderMinute", settings.dailyReminderMinute.coerceIn(0, 59))
      .apply()
  }

  fun syncUserStreakContext(context: Context, streakDays: Int, userName: String) {
    context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putInt("cachedStreakDays", streakDays.coerceAtLeast(0))
      .putString("cachedUserName", userName)
      .apply()
  }

  fun getDailyReminderTime(context: Context): Pair<Int, Int> {
    val prefs = context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    val hour = prefs.getInt("dailyReminderHour", 19).coerceIn(0, 23)
    val minute = prefs.getInt("dailyReminderMinute", 0).coerceIn(0, 59)
    return hour to minute
  }

  fun setDailyReminderTime(context: Context, hourOfDay: Int, minute: Int) {
    val safeHour = hourOfDay.coerceIn(0, 23)
    val safeMin = minute.coerceIn(0, 59)
    context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putInt("dailyReminderHour", safeHour)
      .putInt("dailyReminderMinute", safeMin)
      .apply()
    SmartNotificationScheduler.scheduleUserDailyStreakReminder(context, safeHour, safeMin)
  }

  fun formatReminderTime(hourOfDay: Int, minute: Int): String {
    val safeHour = hourOfDay.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val amPm = if (safeHour < 12) "AM" else "PM"
    val hour12 = when {
      safeHour == 0 -> 12
      safeHour > 12 -> safeHour - 12
      else -> safeHour
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, safeMinute, amPm)
  }

  /**
   * Posts a rich Word of the Day notification matching the exact required format:
   *
   * intriguing — Word of the Day
   * Meaning: Very interesting or mysterious.
   * Example: "That's an intriguing idea!"
   * Tap to learn and practice pronunciation.
   */
  fun showWordOfTheDayNotification(
    context: Context,
    entry: WordOfTheDayEntry = SmartEnglishContentCatalog.WORDS_OF_THE_DAY.first(),
    uniqueKey: String = "wod_${NotificationDeduplicator.todayDateKey()}_${entry.word}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.WORD_OF_THE_DAY)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.WORD_OF_THE_DAY,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val title = "${entry.word} — Word of the Day"
    val collapsedText = "Meaning: ${entry.meaning} Example: \"${entry.exampleSentence}\""
    val expandedBigText = buildString {
      appendLine("Meaning: ${entry.meaning}")
      if (entry.pronunciation.isNotBlank()) {
        appendLine("Pronunciation: ${entry.pronunciation}")
      }
      appendLine()
      appendLine("Example: \"${entry.exampleSentence}\"")
      appendLine()
      append("Tap to learn and practice pronunciation.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "word_of_the_day",
      word = entry.word,
      meaning = entry.meaning,
      pronunciation = entry.pronunciation,
      exampleSentence = entry.exampleSentence,
      lessonId = entry.lessonId
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.WORD_OF_THE_DAY,
      notificationId = ("wod_${entry.word}".hashCode() and 0x7FFFFFFF).coerceAtLeast(5001),
      title = title,
      shortBody = collapsedText,
      bigBody = expandedBigText,
      deepLink = deepLink,
      actionLabel = "Practice Pronunciation"
    )
  }

  fun showDailyVocabChallengeNotification(
    context: Context,
    entry: VocabChallengeEntry = SmartEnglishContentCatalog.getTodayVocabChallenge(),
    uniqueKey: String = "vocab_challenge_${NotificationDeduplicator.todayDateKey()}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.VOCAB_CHALLENGE)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.VOCAB_CHALLENGE,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val bigBody = buildString {
      appendLine(entry.challengeQuestion)
      appendLine()
      appendLine("Target Words: ${entry.targetWords}")
      append("Tap to start today's vocabulary challenge and earn bonus XP.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "vocab_challenge",
      lessonId = entry.lessonId
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.VOCAB_CHALLENGE,
      notificationId = ("vc_${entry.id}".hashCode() and 0x7FFFFFFF).coerceAtLeast(6001),
      title = entry.title,
      shortBody = entry.challengeQuestion,
      bigBody = bigBody,
      deepLink = deepLink,
      actionLabel = "Start Challenge"
    )
  }

  fun showGrammarTipNotification(
    context: Context,
    entry: GrammarTipEntry = SmartEnglishContentCatalog.getTodayGrammarTip(),
    uniqueKey: String = "grammar_tip_${NotificationDeduplicator.todayDateKey()}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.GRAMMAR_TIP)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.GRAMMAR_TIP,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val bigBody = buildString {
      appendLine(entry.tipExplanation)
      appendLine()
      appendLine("Example: \"${entry.exampleCorrect}\"")
      append("Tap to open the grammar lesson and practice.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "grammar_tip",
      lessonId = entry.lessonId
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.GRAMMAR_TIP,
      notificationId = ("gt_${entry.id}".hashCode() and 0x7FFFFFFF).coerceAtLeast(7001),
      title = entry.title,
      shortBody = entry.tipExplanation,
      bigBody = bigBody,
      deepLink = deepLink,
      actionLabel = "Open Grammar Lesson"
    )
  }

  fun showSpeakingPracticeReminderNotification(
    context: Context,
    entry: SpeakingReminderEntry = SmartEnglishContentCatalog.getTodaySpeakingReminder(),
    uniqueKey: String = "speaking_reminder_${NotificationDeduplicator.todayDateKey()}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.SPEAKING_REMINDER)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.SPEAKING_REMINDER,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val bigBody = buildString {
      appendLine(entry.promptMessage)
      appendLine()
      appendLine("Suggested Topic: ${entry.scenarioTitle}")
      append("Tap to start speaking practice now.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "speaking_reminder"
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.SPEAKING_REMINDER,
      notificationId = ("sr_${entry.id}".hashCode() and 0x7FFFFFFF).coerceAtLeast(8001),
      title = entry.title,
      shortBody = entry.promptMessage,
      bigBody = bigBody,
      deepLink = deepLink,
      actionLabel = "Practice Speaking"
    )
  }

  fun showDailyStreakReminderNotification(
    context: Context,
    streakDays: Int? = null,
    userName: String? = null,
    uniqueKey: String = "streak_reminder_${NotificationDeduplicator.todayDateKey()}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.STREAK_REMINDER)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.STREAK_REMINDER,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val prefs = context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    val resolvedStreak = streakDays ?: prefs.getInt("cachedStreakDays", 0)
    val resolvedName = userName ?: prefs.getString("cachedUserName", "").orEmpty()
    val entry = SmartEnglishContentCatalog.getTodayStreakReminder(resolvedStreak, resolvedName)

    val bigBody = buildString {
      appendLine(entry.friendlyMessage)
      appendLine()
      appendLine("💡 ${entry.actionTip}")
      append("Tap to continue your learning streak now.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "streak_reminder"
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.STREAK_REMINDER,
      notificationId = ("streak_${entry.id}".hashCode() and 0x7FFFFFFF).coerceAtLeast(8501),
      title = entry.title,
      shortBody = entry.friendlyMessage,
      bigBody = bigBody,
      deepLink = deepLink,
      actionLabel = "Keep Streak Alive"
    )
  }

  fun showSpeakingInvitationNotification(
    context: Context,
    senderId: String,
    senderName: String,
    invitationMessage: String,
    uniqueKey: String = "invite_${senderId}_${System.currentTimeMillis() / 60_000L}",
    bypassCooldown: Boolean = false
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.SPEAKING_INVITATION)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.SPEAKING_INVITATION,
        bypassCooldown = bypassCooldown
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val title = "Speaking Invitation from $senderName"
    val bigBody = buildString {
      appendLine(invitationMessage)
      appendLine()
      append("Tap to open conversation and practice speaking with $senderName.")
    }

    val deepLink = SmartNotificationDeepLink(
      route = "speaking_invitation",
      partnerId = senderId,
      partnerName = senderName
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.SPEAKING_INVITATION,
      notificationId = ("inv_${senderId}_${uniqueKey}".hashCode() and 0x7FFFFFFF).coerceAtLeast(9001),
      title = title,
      shortBody = invitationMessage,
      bigBody = bigBody,
      deepLink = deepLink,
      actionLabel = "Open Conversation",
      highPriority = true
    )
  }

  fun showMissedCallNotification(
    context: Context,
    callerId: String,
    callerName: String,
    callType: String,
    roomId: String,
    uniqueKey: String = "missed_call_$roomId"
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    if (!isCategoryEnabled(context, SmartNotificationCategory.MISSED_CALL)) return false
    if (!NotificationDeduplicator.shouldDeliver(
        context = context,
        uniqueKey = uniqueKey,
        category = SmartNotificationCategory.MISSED_CALL,
        bypassCooldown = true
      )
    ) {
      return false
    }

    ensureChannelsCreated(context)

    val modeLabel = if (callType.equals("video", ignoreCase = true)) "Video" else "Voice"
    val title = "Missed $modeLabel Call from $callerName"
    val shortBody = "You missed a live English speaking call from $callerName. Tap to message or call back."

    val deepLink = SmartNotificationDeepLink(
      route = "missed_call",
      partnerId = callerId,
      partnerName = callerName,
      roomId = roomId
    )

    return postSystemNotification(
      context = context,
      category = SmartNotificationCategory.MISSED_CALL,
      notificationId = ("missed_$roomId".hashCode() and 0x7FFFFFFF).coerceAtLeast(9501),
      title = title,
      shortBody = shortBody,
      bigBody = shortBody,
      deepLink = deepLink,
      actionLabel = "Call Back / Chat",
      highPriority = true
    )
  }

  /**
   * Bridges a newly arrived Firestore [UserNotification] document into a native Android
   * notification if it hasn't been delivered yet.
   */
  fun handleRealtimeUserNotification(context: Context, notif: UserNotification) {
    if (notif.isRead || notif.id.isBlank()) return
    val uniqueKey = "fs_notif_${notif.id}"
    when (notif.type.lowercase()) {
      "word_of_the_day" -> {
        val entry = WordOfTheDayEntry(
          word = notif.word.ifBlank { "intriguing" },
          meaning = notif.meaning.ifBlank { "Very interesting or mysterious." },
          pronunciation = notif.pronunciation.ifBlank { "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)" },
          exampleSentence = notif.exampleSentence.ifBlank { "That's an intriguing idea!" },
          lessonId = notif.targetId.ifBlank { "en_vocab_1" }
        )
        showWordOfTheDayNotification(context, entry, uniqueKey = uniqueKey)
      }
      "vocab_challenge" -> {
        showDailyVocabChallengeNotification(
          context = context,
          entry = VocabChallengeEntry(
            id = notif.id,
            title = notif.title.ifBlank { "Daily Vocabulary Challenge" },
            challengeQuestion = notif.message,
            targetWords = notif.word.ifBlank { "intriguing • eloquent • resilient" },
            lessonId = notif.targetId.ifBlank { "en_vocab_1" }
          ),
          uniqueKey = uniqueKey
        )
      }
      "grammar_tip" -> {
        showGrammarTipNotification(
          context = context,
          entry = GrammarTipEntry(
            id = notif.id,
            title = notif.title.ifBlank { "English Grammar Tip" },
            tipExplanation = notif.message,
            exampleCorrect = notif.exampleSentence.ifBlank { "I have practiced English every day this week." },
            lessonId = notif.targetId.ifBlank { "en_gram_1" }
          ),
          uniqueKey = uniqueKey
        )
      }
      "speaking_reminder" -> {
        showSpeakingPracticeReminderNotification(
          context = context,
          entry = SpeakingReminderEntry(
            id = notif.id,
            title = notif.title.ifBlank { "Speaking Practice Reminder" },
            promptMessage = notif.message,
            scenarioTitle = "Daily English Speaking Practice"
          ),
          uniqueKey = uniqueKey
        )
      }
      "streak_reminder" -> {
        showDailyStreakReminderNotification(
          context = context,
          uniqueKey = uniqueKey
        )
      }
      "speaking_invitation", "call_invite", "friend_request" -> {
        showSpeakingInvitationNotification(
          context = context,
          senderId = notif.senderId.ifBlank { notif.targetId },
          senderName = notif.senderName.ifBlank { "Speaking Partner" },
          invitationMessage = notif.message.ifBlank { notif.title },
          uniqueKey = uniqueKey
        )
      }
      "missed_call" -> {
        showMissedCallNotification(
          context = context,
          callerId = notif.senderId.ifBlank { notif.targetId },
          callerName = notif.senderName.ifBlank { "Speaking Partner" },
          callType = "voice",
          roomId = notif.targetId.ifBlank { notif.id },
          uniqueKey = uniqueKey
        )
      }
      "app_update" -> Unit
    }
  }

  /**
   * Handles incoming FCM data payloads for smart learning notifications when the app is
   * in the foreground, background, or closed.
   */
  fun handleFcmLearningPayload(context: Context, data: Map<String, String>): Boolean {
    val type = (data["type"] ?: data["notifType"] ?: "").trim().lowercase()
    val notifId = data["notificationId"] ?: data["id"] ?: "fcm_${type}_${NotificationDeduplicator.todayDateKey()}"
    return when (type) {
      "word_of_the_day" -> {
        val word = data["word"]?.takeIf { it.isNotBlank() } ?: "intriguing"
        val meaning = data["meaning"]?.takeIf { it.isNotBlank() } ?: "Very interesting or mysterious."
        val pronunciation = data["pronunciation"]?.takeIf { it.isNotBlank() } ?: "/ɪnˈtriːɡɪŋ/ (in-TREE-ging)"
        val example = data["example"] ?: data["exampleSentence"] ?: "That's an intriguing idea!"
        val lessonId = data["lessonId"] ?: "en_vocab_1"
        showWordOfTheDayNotification(
          context = context,
          entry = WordOfTheDayEntry(word, meaning, pronunciation, example, lessonId),
          uniqueKey = notifId
        )
      }
      "vocab_challenge" -> {
        val title = data["title"] ?: "Daily Vocabulary Challenge"
        val body = data["body"] ?: data["message"] ?: SmartEnglishContentCatalog.getTodayVocabChallenge().challengeQuestion
        val words = data["targetWords"] ?: "intriguing • eloquent • pragmatic"
        val lessonId = data["lessonId"] ?: "en_vocab_1"
        showDailyVocabChallengeNotification(
          context = context,
          entry = VocabChallengeEntry(notifId, title, body, words, lessonId),
          uniqueKey = notifId
        )
      }
      "grammar_tip" -> {
        val title = data["title"] ?: "English Grammar Tip"
        val body = data["body"] ?: data["message"] ?: SmartEnglishContentCatalog.getTodayGrammarTip().tipExplanation
        val example = data["example"] ?: SmartEnglishContentCatalog.getTodayGrammarTip().exampleCorrect
        val lessonId = data["lessonId"] ?: "en_gram_1"
        showGrammarTipNotification(
          context = context,
          entry = GrammarTipEntry(notifId, title, body, example, lessonId),
          uniqueKey = notifId
        )
      }
      "speaking_reminder" -> {
        val title = data["title"] ?: "Speaking Practice Reminder"
        val body = data["body"] ?: data["message"] ?: SmartEnglishContentCatalog.getTodaySpeakingReminder().promptMessage
        val scenario = data["scenario"] ?: "Daily English Speaking Practice"
        showSpeakingPracticeReminderNotification(
          context = context,
          entry = SpeakingReminderEntry(notifId, title, body, scenario),
          uniqueKey = notifId
        )
      }
      "speaking_invitation" -> {
        val senderId = data["senderId"] ?: data["partnerId"] ?: ""
        val senderName = data["senderName"] ?: data["partnerName"] ?: "Speaking Partner"
        val message = data["body"] ?: data["message"] ?: "$senderName invited you to practice speaking English!"
        showSpeakingInvitationNotification(
          context = context,
          senderId = senderId,
          senderName = senderName,
          invitationMessage = message,
          uniqueKey = notifId
        )
      }
      "missed_call" -> {
        val callerId = data["callerId"] ?: data["senderId"] ?: ""
        val callerName = data["callerName"] ?: data["senderName"] ?: "Speaking Partner"
        val callType = data["callType"] ?: "voice"
        val roomId = data["roomId"] ?: notifId
        showMissedCallNotification(
          context = context,
          callerId = callerId,
          callerName = callerName,
          callType = callType,
          roomId = roomId,
          uniqueKey = notifId
        )
      }
      "app_update", "release_update" -> false
      else -> false
    }
  }

  private fun postSystemNotification(
    context: Context,
    category: SmartNotificationCategory,
    notificationId: Int,
    title: String,
    shortBody: String,
    bigBody: String,
    deepLink: SmartNotificationDeepLink,
    actionLabel: String,
    highPriority: Boolean = false
  ): Boolean {
    return try {
      val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        SmartNotificationContract.applyToIntent(this, deepLink)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val builder = NotificationCompat.Builder(context, category.channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(shortBody)
        .setStyle(
          NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(bigBody)
        )
        .setPriority(
          if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        )
        .setCategory(
          if (highPriority) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_REMINDER
        )
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .addAction(
          android.R.drawable.ic_media_play,
          actionLabel,
          pendingIntent
        )

      NotificationManagerCompat.from(context).notify(notificationId, builder.build())
      true
    } catch (se: SecurityException) {
      Log.w(TAG, "Notification permission not granted: ${se.message}")
      false
    } catch (e: Exception) {
      Log.w(TAG, "Failed to post notification: ${e.message}")
      false
    }
  }
}

/**
 * Schedules daily background notifications via [AlarmManager] so Word of the Day,
 * Daily Vocabulary Challenges, Grammar Tips, Speaking Practice Reminders, and the
 * user-defined Daily Streak Reminder work even when the app is closed.
 */
object SmartNotificationScheduler {
  private const val TAG = "SmartNotifScheduler"
  private const val SCHEDULER_PREFS = "lingosphere_streak_scheduler_prefs"
  private const val REQ_WORD_OF_DAY = 4101
  private const val REQ_VOCAB_CHALLENGE = 4102
  private const val REQ_GRAMMAR_TIP = 4103
  private const val REQ_SPEAKING_REMINDER = 4104
  const val REQ_DAILY_STREAK_REMINDER = 4105

  fun computeNextTriggerTimeMillis(
    hourOfDay: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): Long {
    val safeHour = hourOfDay.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val calendar = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, safeHour)
      set(Calendar.MINUTE, safeMinute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= nowMillis) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }
    return calendar.timeInMillis
  }

  fun scheduleDailyLearningNotifications(context: Context) {
    val cleanCtx = context.applicationContext
    SmartNotificationManager.ensureChannelsCreated(cleanCtx)

    // Schedule each category at a distinct hour of the day to avoid notification spam
    scheduleDailyCategoryAlarm(cleanCtx, SmartNotificationCategory.WORD_OF_THE_DAY, hourOfDay = 9, minute = 0, requestCode = REQ_WORD_OF_DAY)
    scheduleDailyCategoryAlarm(cleanCtx, SmartNotificationCategory.VOCAB_CHALLENGE, hourOfDay = 13, minute = 0, requestCode = REQ_VOCAB_CHALLENGE)
    scheduleDailyCategoryAlarm(cleanCtx, SmartNotificationCategory.GRAMMAR_TIP, hourOfDay = 17, minute = 0, requestCode = REQ_GRAMMAR_TIP)
    scheduleDailyCategoryAlarm(cleanCtx, SmartNotificationCategory.SPEAKING_REMINDER, hourOfDay = 20, minute = 0, requestCode = REQ_SPEAKING_REMINDER)

    // Schedule the user-defined friendly daily streak reminder
    scheduleUserDailyStreakReminder(cleanCtx)
  }

  /**
   * Schedules (or cancels if disabled) a friendly daily streak reminder at the user-defined
   * [hourOfDay] and [minute]. Returns the scheduled next trigger timestamp in millis, or 0L if cancelled.
   */
  fun scheduleUserDailyStreakReminder(
    context: Context,
    hourOfDay: Int? = null,
    minute: Int? = null,
    nowMillis: Long = System.currentTimeMillis()
  ): Long {
    val cleanCtx = context.applicationContext
    SmartNotificationManager.ensureChannelsCreated(cleanCtx)

    val (savedHour, savedMinute) = SmartNotificationManager.getDailyReminderTime(cleanCtx)
    val targetHour = (hourOfDay ?: savedHour).coerceIn(0, 23)
    val targetMinute = (minute ?: savedMinute).coerceIn(0, 59)

    val prefs = cleanCtx.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)

    return try {
      val alarmManager = cleanCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      val intent = Intent(cleanCtx, SmartNotificationAlarmReceiver::class.java).apply {
        action = SmartNotificationContract.ACTION_TRIGGER_SCHEDULED_NOTIFICATION
        putExtra(SmartNotificationContract.EXTRA_SCHEDULED_CATEGORY, SmartNotificationCategory.STREAK_REMINDER.id)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        cleanCtx,
        REQ_DAILY_STREAK_REMINDER,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      if (!SmartNotificationManager.isCategoryEnabled(cleanCtx, SmartNotificationCategory.STREAK_REMINDER)) {
        alarmManager?.cancel(pendingIntent)
        prefs.edit()
          .putLong("next_streak_reminder_millis", 0L)
          .putBoolean("streak_reminder_active", false)
          .apply()
        return 0L
      }

      val nextTriggerMillis = computeNextTriggerTimeMillis(targetHour, targetMinute, nowMillis)
      alarmManager?.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        nextTriggerMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
      )

      prefs.edit()
        .putLong("next_streak_reminder_millis", nextTriggerMillis)
        .putInt("scheduled_hour", targetHour)
        .putInt("scheduled_minute", targetMinute)
        .putBoolean("streak_reminder_active", true)
        .apply()

      nextTriggerMillis
    } catch (e: Exception) {
      Log.w(TAG, "Failed to schedule daily streak reminder: ${e.message}")
      0L
    }
  }

  fun cancelUserDailyStreakReminder(context: Context) {
    val cleanCtx = context.applicationContext
    try {
      val alarmManager = cleanCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      val intent = Intent(cleanCtx, SmartNotificationAlarmReceiver::class.java).apply {
        action = SmartNotificationContract.ACTION_TRIGGER_SCHEDULED_NOTIFICATION
        putExtra(SmartNotificationContract.EXTRA_SCHEDULED_CATEGORY, SmartNotificationCategory.STREAK_REMINDER.id)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        cleanCtx,
        REQ_DAILY_STREAK_REMINDER,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      alarmManager?.cancel(pendingIntent)
      cleanCtx.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong("next_streak_reminder_millis", 0L)
        .putBoolean("streak_reminder_active", false)
        .apply()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to cancel daily streak reminder: ${e.message}")
    }
  }

  fun getNextScheduledStreakReminderMillis(context: Context): Long {
    val cleanCtx = context.applicationContext
    return cleanCtx.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)
      .getLong("next_streak_reminder_millis", 0L)
  }

  fun isDailyStreakReminderScheduled(context: Context): Boolean {
    val cleanCtx = context.applicationContext
    return cleanCtx.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)
      .getBoolean("streak_reminder_active", false)
  }

  fun formatNextReminderScheduleLabel(
    enabled: Boolean,
    hourOfDay: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis()
  ): String {
    if (!enabled) return "Daily streak reminder is paused"
    val formattedTime = SmartNotificationManager.formatReminderTime(hourOfDay, minute)
    val nextMillis = computeNextTriggerTimeMillis(hourOfDay, minute, nowMillis)
    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nextCal = Calendar.getInstance().apply { timeInMillis = nextMillis }
    val isToday = nowCal.get(Calendar.YEAR) == nextCal.get(Calendar.YEAR) &&
      nowCal.get(Calendar.DAY_OF_YEAR) == nextCal.get(Calendar.DAY_OF_YEAR)
    val dayWord = if (isToday) "Today" else "Tomorrow"
    return "Next friendly reminder: $dayWord at $formattedTime"
  }

  private fun scheduleDailyCategoryAlarm(
    context: Context,
    category: SmartNotificationCategory,
    hourOfDay: Int,
    minute: Int,
    requestCode: Int
  ) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val intent = Intent(context, SmartNotificationAlarmReceiver::class.java).apply {
        action = SmartNotificationContract.ACTION_TRIGGER_SCHEDULED_NOTIFICATION
        putExtra(SmartNotificationContract.EXTRA_SCHEDULED_CATEGORY, category.id)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      if (!SmartNotificationManager.isCategoryEnabled(context, category)) {
        alarmManager.cancel(pendingIntent)
        return
      }

      val nextTriggerMillis = computeNextTriggerTimeMillis(hourOfDay, minute)

      alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        nextTriggerMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed to schedule alarm for ${category.id}: ${e.message}")
    }
  }
}

class SmartNotificationAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action.orEmpty()
    if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      com.example.update.LingoAppUpdateController.reconcileInstalledVersionOnStartup(context)
      SmartNotificationScheduler.scheduleDailyLearningNotifications(context)
      return
    }

    if (action == SmartNotificationContract.ACTION_TRIGGER_SCHEDULED_NOTIFICATION) {
      val categoryId = intent?.getStringExtra(SmartNotificationContract.EXTRA_SCHEDULED_CATEGORY).orEmpty()
      when (categoryId) {
        SmartNotificationCategory.WORD_OF_THE_DAY.id -> {
          SmartNotificationManager.showWordOfTheDayNotification(
            context = context,
            entry = SmartEnglishContentCatalog.getTodayWordOfTheDay()
          )
        }
        SmartNotificationCategory.VOCAB_CHALLENGE.id -> {
          SmartNotificationManager.showDailyVocabChallengeNotification(
            context = context,
            entry = SmartEnglishContentCatalog.getTodayVocabChallenge()
          )
        }
        SmartNotificationCategory.GRAMMAR_TIP.id -> {
          SmartNotificationManager.showGrammarTipNotification(
            context = context,
            entry = SmartEnglishContentCatalog.getTodayGrammarTip()
          )
        }
        SmartNotificationCategory.SPEAKING_REMINDER.id -> {
          SmartNotificationManager.showSpeakingPracticeReminderNotification(
            context = context,
            entry = SmartEnglishContentCatalog.getTodaySpeakingReminder()
          )
        }
        SmartNotificationCategory.STREAK_REMINDER.id -> {
          SmartNotificationManager.showDailyStreakReminderNotification(
            context = context
          )
          // Keep next day's reminder timestamp refreshed
          SmartNotificationScheduler.scheduleUserDailyStreakReminder(context)
        }
      }
    }
  }
}
