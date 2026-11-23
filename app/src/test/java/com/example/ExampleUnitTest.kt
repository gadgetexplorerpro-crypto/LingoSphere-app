package com.example

import com.example.data.repository.YouTubeVideoService
import com.example.ui.components.sanitizeCreatorName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun curatedVideos_arePopulatedAndHaveFallbackIds() {
    val languages = listOf("es", "fr", "de", "ja", "ko", "ar", "zh")
    for (lang in languages) {
      val videos = YouTubeVideoService.getCuratedRealYouTubeVideos(
        languageCode = lang,
        proficiencyLevel = "All",
        topicCategory = "All Topics",
        searchQuery = ""
      )
      assertTrue("Expected curated videos for $lang", videos.isNotEmpty())
      val firstId = videos.first().videoId
      val fallbacks = YouTubeVideoService.getFallbackVideoIdsForLanguage(lang, firstId)
      assertTrue("Expected fallback video IDs for $lang", fallbacks.isNotEmpty())
      assertFalse("Fallback IDs should not contain current video ID", fallbacks.contains(firstId))
    }
  }

  @Test
  fun sanitizeCreatorName_removesThirdPartyBranding() {
    assertEquals("Butterfly Spanish", sanitizeCreatorName("Butterfly Spanish • YouTube"))
    assertEquals("Global Relay", sanitizeCreatorName("Cloudflare Relay"))
  }
}
