package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.MemeDialogue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DailyMemeTrendWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("DailyMemeTrendWorker", "Starting daily viral Indian meme trend worker execution...")
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                Log.e("DailyMemeTrendWorker", "Gemini API key missing in BuildConfig")
                return@withContext Result.failure()
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build()

            val prompt = "Find and summarize the top 10 absolute most popular viral Desi/Hindi/Hinglish meme dialogues, social media trends, movie dialogue catchphrases, viral reels, or slang phrases currently trending in India right now in 2026. " +
                         "For each trending meme, specify a suitable single emoji, the dialogue text (in Hinglish, i.e., Roman script, like 'Moye Moye' or 'Just looking like a wow'), a category (choose exactly from: '😂 Hasna', '😎 Swag', '😭 Dukh', '😱 Shock', '❤️ Pyaar', '😡 Gussa', '🤝 Dosti'), and the mood (choose exactly from: 'FUNNY', 'SWAG', 'SAD', 'SHOCK', 'LOVE', 'ANGRY'). " +
                         "Format the output strictly as a JSON array of 10 objects. Do not include any other text, markdown blocks, or translations. " +
                         "Example: [ { \"emoji\": \"😭\", \"dialogue\": \"Yeh dukh kaahe khatam nahi hota be\", \"category\": \"😭 Dukh\", \"mood\": \"SAD\" } ]"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearchRetrieval", JSONObject())
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.4)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("DailyMemeTrendWorker", "Gemini API error code: ${response.code}")
                    return@withContext Result.retry()
                }

                val responseBody = response.body?.string() ?: return@withContext Result.retry()
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var rawText = parts.getJSONObject(0).optString("text", "").trim()
                            if (rawText.startsWith("```json")) {
                                rawText = rawText.removePrefix("```json").trim()
                            }
                            if (rawText.endsWith("```")) {
                                rawText = rawText.removeSuffix("```").trim()
                            }

                            val jsonArray = JSONArray(rawText)
                            val parsedMemes = mutableListOf<MemeDialogue>()
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                parsedMemes.add(
                                    MemeDialogue(
                                        id = 0,
                                        emoji = obj.optString("emoji", "🔥"),
                                        dialogue = obj.optString("dialogue", ""),
                                        category = obj.optString("category", "😂 Hasna"),
                                        mood = obj.optString("mood", "FUNNY"),
                                        isCustom = true
                                    )
                                )
                            }

                            if (parsedMemes.isNotEmpty()) {
                                // 1. Save raw JSON to SharedPreferences for instant caching
                                val prefs = appContext.getSharedPreferences("trending_memes_cache", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("cached_trending_json", rawText)
                                    .putLong("last_updated_time", System.currentTimeMillis())
                                    .apply()

                                // 2. Insert generated 10 viral memes into Room DB
                                val dao = AppDatabase.getDatabase(appContext).memeDao()
                                parsedMemes.forEach { meme ->
                                    if (meme.dialogue.isNotBlank()) {
                                        dao.insertDialogue(meme)
                                    }
                                }

                                Log.d("DailyMemeTrendWorker", "Successfully generated and saved ${parsedMemes.size} viral memes!")
                                return@withContext Result.success()
                            }
                        }
                    }
                }
            }
            Result.retry()
        } catch (e: Exception) {
            Log.e("DailyMemeTrendWorker", "Worker execution failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "DailyMemeTrendWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // 1. Periodic Worker every 24 Hours
            val periodicWorkRequest = PeriodicWorkRequestBuilder<DailyMemeTrendWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            // 2. Initial One-time Request to fetch immediately on start
            val immediateWorkRequest = OneTimeWorkRequestBuilder<DailyMemeTrendWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        }
    }
}
