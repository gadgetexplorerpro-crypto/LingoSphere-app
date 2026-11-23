package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.notifications.SmartNotificationManager
import com.example.notifications.SmartNotificationScheduler
import com.example.presence.PresenceManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class LingoApplication : Application(), ImageLoaderFactory {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .callFactory {
        OkHttpClient.Builder()
          .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
              .header("User-Agent", "LingoSphere/1.0 (Android; Language Learning App; contact: support@lingosphere.app)")
              .build()
            chain.proceed(request)
          }
          .followRedirects(true)
          .followSslRedirects(true)
          .build()
      }
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(0.25)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("image_cache"))
          .maxSizeBytes(80L * 1024 * 1024)
          .build()
      }
      .respectCacheHeaders(false)
      .crossfade(true)
      .build()
  }

  override fun onCreate() {
    super.onCreate()
    try {
      appScope.launch {
        try {
          // Defer non-critical background warm-up until MainActivity completes initial SurfaceSyncGroup transactions
          delay(3800L)
          runCatching { com.example.ai.GeminiService.initialize(this@LingoApplication) }
          delay(2100L)
          if (FirebaseApp.getApps(this@LingoApplication).isNotEmpty()) {
            PresenceManager.initialize(this@LingoApplication)
          }
          delay(600L)
          SmartNotificationManager.ensureChannelsCreated(this@LingoApplication)
          SmartNotificationScheduler.scheduleDailyLearningNotifications(this@LingoApplication)
        } catch (e: Exception) {
          Log.w("LingoApplication", "Background init warning", e)
        }
      }
    } catch (e: Exception) {
      Log.e("LingoApplication", "Error during initialization", e)
    }
  }
}

