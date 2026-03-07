package com.thinkoff.clawwatch

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

/**
 * Supported LLM providers
 */
enum class LLMProvider {
    ANTHROPIC,
    OPENCODE_ZEN,
    NVIDIA,
    MOONSHOT,
    OPENROUTER
}

/**
 * Provider configuration sealed class for provider-specific settings
 */
sealed class ProviderConfig(
    open val apiKey: String,
    open val endpoint: String,
    open val model: String
) {
    abstract fun buildRequestBody(
        model: String,
        maxTokens: Int,
        systemPrompt: String,
        messages: JSONArray
    ): JSONObject

    abstract fun extractResponseText(response: JSONObject): String?

    abstract fun getHeaders(): Map<String, String>

    data class AnthropicConfig(
        override val apiKey: String,
        override val model: String = "claude-opus-4-6"
    ) : ProviderConfig(apiKey, "https://api.anthropic.com/v1/messages", model) {
        override fun getHeaders(): Map<String, String> = mapOf(
            "Content-Type" to "application/json",
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01"
        )

        override fun buildRequestBody(
            model: String,
            maxTokens: Int,
            systemPrompt: String,
            messages: JSONArray
        ): JSONObject = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", systemPrompt)
            put("messages", messages)
        }

        override fun extractResponseText(response: JSONObject): String? = try {
            response.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) { null }
    }

    data class OpenCodeZenConfig(
        override val apiKey: String = "public",
        override val model: String = "gpt-5-nano"
    ) : ProviderConfig(apiKey, "https://opencode.ai/zen/v1/responses", model) {
        override fun getHeaders(): Map<String, String> = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $apiKey"
        )

        override fun buildRequestBody(
            model: String,
            maxTokens: Int,
            systemPrompt: String,
            messages: JSONArray
        ): JSONObject = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                // Add system message as first message
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                // Add conversation messages
                for (i in 0 until messages.length()) {
                    put(messages.getJSONObject(i))
                }
            })
        }

        override fun extractResponseText(response: JSONObject): String? = try {
            // OpenAI-compatible format
            response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) { null }
    }

    data class NvidiaConfig(
        override val apiKey: String,
        override val model: String = "moonshotai/kimi-k2.5"
    ) : ProviderConfig(apiKey, "https://integrate.api.nvidia.com/v1/chat/completions", model) {
        override fun getHeaders(): Map<String, String> = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $apiKey"
        )

        override fun buildRequestBody(
            model: String,
            maxTokens: Int,
            systemPrompt: String,
            messages: JSONArray
        ): JSONObject = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (i in 0 until messages.length()) {
                    put(messages.getJSONObject(i))
                }
            })
        }

        override fun extractResponseText(response: JSONObject): String? {
            return try {
                val message = response.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")

                if (message.has("content") && !message.isNull("content")) {
                    val content = message.optString("content", "").trim()
                    if (content.isNotBlank()) {
                        content
                    } else {
                        val reasoning = message.optString("reasoning_content", "").trim()
                        if (reasoning.isNotBlank()) reasoning else null
                    }
                } else {
                    val reasoning = message.optString("reasoning_content", "").trim()
                    if (reasoning.isNotBlank()) reasoning else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    data class MoonshotConfig(
        override val apiKey: String,
        override val model: String = "kimi-k2.5"
    ) : ProviderConfig(apiKey, "https://api.moonshot.cn/v1/chat/completions", model) {
        override fun getHeaders(): Map<String, String> = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $apiKey"
        )

        override fun buildRequestBody(
            model: String,
            maxTokens: Int,
            systemPrompt: String,
            messages: JSONArray
        ): JSONObject = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (i in 0 until messages.length()) {
                    put(messages.getJSONObject(i))
                }
            })
        }

    override fun extractResponseText(response: JSONObject): String? = try {
        response.getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
    } catch (e: Exception) { null }
}

data class OpenRouterConfig(
    override val apiKey: String,
    override val model: String = "moonshotai/kimi-k2.5"
) : ProviderConfig(apiKey, "https://openrouter.ai/api/v1/chat/completions", model) {
    override fun getHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "Authorization" to "Bearer $apiKey"
    )

    override fun buildRequestBody(
        model: String,
        maxTokens: Int,
        systemPrompt: String,
        messages: JSONArray
    ): JSONObject = JSONObject().apply {
        put("model", model)
        put("max_tokens", maxTokens)
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            for (i in 0 until messages.length()) {
                put(messages.getJSONObject(i))
            }
        })
    }

    override fun extractResponseText(response: JSONObject): String? = try {
        response.getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
    } catch (e: Exception) { null }
}
}

/**
 * ClawRunner — manages NullClaw binary + Anthropic API calls with optional RAG.
 *
 * RAG modes:
 *  - KOTLIN: pre-search with DuckDuckGo (no key) or Brave Search (optional key),
 *            inject top results into system context before Anthropic call.
 *  - OPUS_TOOL: use Anthropic tool_use to let Claude call a web_search tool,
 *               execute the search on the Kotlin side, return results, get final answer.
 *
 * Config is read from encrypted SharedPreferences (migrated from legacy prefs if present).
 */
class ClawRunner(private val context: Context) {

    companion object {
        private const val TAG = "ClawRunner"
        private const val CONFIG_NAME = "nullclaw.json"
        private const val CONFIG_FALLBACK_NAME = "nullclaw.json.example"

        // Legacy Anthropic key (backward compatibility)
        private const val PREF_API_KEY = "anthropic_api_key"

        // Provider selection (default: OPENCODE_ZEN for zero-config setup)
        private const val PREF_PROVIDER = "llm_provider"

        // Provider-specific API keys
        private const val PREF_ANTHROPIC_KEY = "anthropic_api_key"
        private const val PREF_NVIDIA_KEY = "nvidia_api_key"
        private const val PREF_MOONSHOT_KEY = "moonshot_api_key"
        private const val PREF_OPENROUTER_KEY = "openrouter_api_key"
        // OpenCode Zen uses "public" placeholder, no key needed

        private const val PREF_MODEL = "model"
        private const val PREF_SYSTEM_PROMPT = "system_prompt"
        private const val PREF_MAX_TOKENS = "max_tokens"
        private const val PREF_RAG_MODE = "rag_mode" // "off" | "kotlin" | "always" | "opus_tool"
        private const val PREF_BRAVE_KEY = "brave_api_key"
        private const val PREF_TAVILY_KEY = "tavily_api_key"

// Embedded NVIDIA API key for zero-config setup
private const val EMBEDDED_NVIDIA_API_KEY = "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"

// Embedded Tavily API key for zero-config RAG
private const val EMBEDDED_TAVILY_API_KEY = "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"

    // Default provider and models
    private val DEFAULT_PROVIDER = LLMProvider.NVIDIA
        private const val DEFAULT_MODEL_ANTHROPIC = "claude-opus-4-6"
        private const val DEFAULT_MODEL_ZEN = "gpt-5-nano"
        private const val DEFAULT_MODEL_NVIDIA = "qwen/qwen3.5-122b-a10b"
        private const val DEFAULT_MODEL_MOONSHOT = "kimi-k2.5"
        private const val DEFAULT_MODEL_OPENROUTER = "moonshotai/kimi-k2.5"

        // Keywords that suggest the query needs current/live information
        private val LIVE_INFO_KEYWORDS = setOf(
            "today", "tonight", "tomorrow", "yesterday", "now", "current", "currently",
            "latest", "recent", "news", "weather", "temperature", "price", "stock",
            "score", "result", "standings", "match", "game", "live", "happening",
            "what time", "how long", "when does", "is it open", "open now",
            "who won", "did they", "is there"
        )

        private const val DEFAULT_MAX_TOKENS = 150
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a voice assistant on a Samsung smartwatch. " +
            "Rules: respond in 1-3 short sentences maximum. No markdown, no lists, no bullet points. " +
            "Plain spoken language only. Be direct and precise. Never say 'Certainly!' or 'Great question!'"

        private const val MAX_CONTEXT_MESSAGES = 10
        private const val MAX_CONTEXT_CHARS_PER_MESSAGE = 600

        /**
         * Get default model for a provider
         */
        fun getDefaultModel(provider: LLMProvider): String = when (provider) {
            LLMProvider.ANTHROPIC -> DEFAULT_MODEL_ANTHROPIC
            LLMProvider.OPENCODE_ZEN -> DEFAULT_MODEL_ZEN
            LLMProvider.NVIDIA -> DEFAULT_MODEL_NVIDIA
            LLMProvider.MOONSHOT -> DEFAULT_MODEL_MOONSHOT
            LLMProvider.OPENROUTER -> DEFAULT_MODEL_OPENROUTER
        }
    }

    private data class ChatTurn(val role: String, val content: String)

    private val filesDir get() = context.filesDir
    private val homeDir get() = context.filesDir.parentFile!!
    private val nativeLibDir get() = context.applicationInfo.nativeLibraryDir
    private val binaryFile get() = File(nativeLibDir, "libnullclaw.so")
    private val configFile get() = File(filesDir, CONFIG_NAME)
    private val nullclawConfigFile get() = File(homeDir, ".nullclaw/config.json")
    private val caBundleFile get() = File(filesDir, "ca-certificates.crt")

    private val prefs by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SecurePrefs.watch(context) }
    private val conversationLock = Any()
    private val conversation = ArrayDeque<ChatTurn>()
    @Volatile
    private var conversationConfigFingerprint: String? = null
    @Volatile
    private var lastProviderErrorDetail: String? = null

    // ── Config accessors ─────────────────────────────────────────────────────

    // Provider selection
    fun saveProvider(provider: LLMProvider) = prefs.edit().putString(PREF_PROVIDER, provider.name).apply()
    fun getProvider(): LLMProvider = try {
        prefs.getString(PREF_PROVIDER, null)?.let {
            LLMProvider.valueOf(it)
        } ?: DEFAULT_PROVIDER
    } catch (e: Exception) { DEFAULT_PROVIDER }

    // Provider-specific API key storage
    fun saveAnthropicKey(key: String) = prefs.edit().putString(PREF_ANTHROPIC_KEY, key).apply()
    fun saveNvidiaKey(key: String) = prefs.edit().putString(PREF_NVIDIA_KEY, key).apply()
    fun saveMoonshotKey(key: String) = prefs.edit().putString(PREF_MOONSHOT_KEY, key).apply()
    fun saveOpenRouterKey(key: String) = prefs.edit().putString(PREF_OPENROUTER_KEY, key).apply()

    // Legacy backward compatibility - delegates to Anthropic
    fun saveApiKey(key: String) {
        prefs.edit().putString(PREF_API_KEY, key).apply()
        prefs.edit().putString(PREF_ANTHROPIC_KEY, key).apply()
    }

    fun saveBraveKey(key: String) = prefs.edit().putString(PREF_BRAVE_KEY, key).apply()
    fun saveTavilyKey(key: String) = prefs.edit().putString(PREF_TAVILY_KEY, key).apply()
    fun saveModel(model: String) = prefs.edit().putString(PREF_MODEL, model).apply()
    fun saveSystemPrompt(prompt: String) = prefs.edit().putString(PREF_SYSTEM_PROMPT, prompt).apply()
    fun saveMaxTokens(n: Int) = prefs.edit().putInt(PREF_MAX_TOKENS, n).apply()
    fun saveRagMode(mode: String) = prefs.edit().putString(PREF_RAG_MODE, mode).apply()

    // Check if provider has a valid API key configured
    fun hasApiKey(): Boolean = hasProviderApiKey(getProvider())

    fun hasProviderApiKey(provider: LLMProvider): Boolean = when (provider) {
        LLMProvider.ANTHROPIC -> getAnthropicKey()?.isNotBlank() == true
        LLMProvider.OPENCODE_ZEN -> true // No key needed
        LLMProvider.NVIDIA -> getNvidiaKey()?.isNotBlank() == true
        LLMProvider.MOONSHOT -> getMoonshotKey()?.isNotBlank() == true
        LLMProvider.OPENROUTER -> getOpenRouterKey()?.isNotBlank() == true
    }

    // Get API key for current provider
    private fun getProviderApiKey(): String? = getApiKeyForProvider(getProvider())

    fun getApiKeyForProvider(provider: LLMProvider): String? = when (provider) {
        LLMProvider.ANTHROPIC -> getAnthropicKey()
        LLMProvider.OPENCODE_ZEN -> "public" // Public placeholder for free tier
        LLMProvider.NVIDIA -> getNvidiaKey()
        LLMProvider.MOONSHOT -> getMoonshotKey()
        LLMProvider.OPENROUTER -> getOpenRouterKey()
    }

    private fun getAnthropicKey(): String? = prefs.getString(PREF_ANTHROPIC_KEY, null)
        ?: prefs.getString(PREF_API_KEY, null) // Fallback to legacy key
    private fun getNvidiaKey(): String? = prefs.getString(PREF_NVIDIA_KEY, null) ?: EMBEDDED_NVIDIA_API_KEY
    private fun getMoonshotKey(): String? = prefs.getString(PREF_MOONSHOT_KEY, null)
    private fun getOpenRouterKey(): String? = prefs.getString(PREF_OPENROUTER_KEY, null)

    // Legacy backward compatibility
    private fun getApiKey(): String? = getProviderApiKey()

    private fun getBraveKey(): String? = prefs.getString(PREF_BRAVE_KEY, null)
    private fun getTavilyKey(): String? = prefs.getString(PREF_TAVILY_KEY, null) ?: EMBEDDED_TAVILY_API_KEY

    // Get model with provider-specific default
    private fun getModel(): String {
        val provider = getProvider()
        val saved = prefs.getString(PREF_MODEL, null)
        val raw = if (!saved.isNullOrBlank()) saved else getDefaultModel(provider)
        return normalizeModelForProvider(provider, raw)
    }

    private fun normalizeModelForProvider(provider: LLMProvider, rawModel: String): String {
        val model = rawModel.trim()
        return when (provider) {
            LLMProvider.NVIDIA -> model
                .removePrefix("nvidia/")
                .let {
                    when (it) {
                        "moonshot/kimi-k2.5" -> "moonshotai/kimi-k2.5"
                        "moonshot/kimi-k2.5-thinking" -> "moonshotai/kimi-k2.5-thinking"
                        else -> it
                    }
                }
            LLMProvider.OPENROUTER -> model.removePrefix("openrouter/")
            LLMProvider.ANTHROPIC -> model.removePrefix("anthropic/")
            else -> model
        }
    }

    private fun getDefaultModel(provider: LLMProvider): String = when (provider) {
        LLMProvider.ANTHROPIC -> DEFAULT_MODEL_ANTHROPIC
        LLMProvider.OPENCODE_ZEN -> DEFAULT_MODEL_ZEN
        LLMProvider.NVIDIA -> DEFAULT_MODEL_NVIDIA
        LLMProvider.MOONSHOT -> DEFAULT_MODEL_MOONSHOT
        LLMProvider.OPENROUTER -> DEFAULT_MODEL_OPENROUTER
    }

    private fun getSystemPrompt(): String = prefs.getString(PREF_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
    private fun getMaxTokens(): Int = prefs.getInt(PREF_MAX_TOKENS, DEFAULT_MAX_TOKENS)
    private fun getRagMode(): String = prefs.getString(PREF_RAG_MODE, "kotlin") ?: "kotlin"

    // Create provider configuration for current provider
    private fun createProviderConfig(): ProviderConfig {
        val provider = getProvider()
        val apiKey = getProviderApiKey() ?: ""
        val model = getModel()
        return when (provider) {
            LLMProvider.ANTHROPIC -> ProviderConfig.AnthropicConfig(apiKey, model)
            LLMProvider.OPENCODE_ZEN -> ProviderConfig.OpenCodeZenConfig(apiKey, model)
            LLMProvider.NVIDIA -> ProviderConfig.NvidiaConfig(apiKey, model)
            LLMProvider.MOONSHOT -> ProviderConfig.MoonshotConfig(apiKey, model)
            LLMProvider.OPENROUTER -> ProviderConfig.OpenRouterConfig(apiKey, model)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun ensureInstalled() = withContext(Dispatchers.IO) {
        Log.i(TAG, "NullClaw binary: ${binaryFile.absolutePath} (exists: ${binaryFile.exists()})")
        if (!configFile.exists()) {
            val assetName = try {
                context.assets.open(CONFIG_NAME).close()
                CONFIG_NAME
            } catch (_: Exception) {
                CONFIG_FALLBACK_NAME
            }
            context.assets.open(assetName).use { it.copyTo(configFile.outputStream()) }
        }
        writeNullclawHomeConfig()
        buildCaBundle()
        Log.i(TAG, "ClawRunner ready, home=${homeDir.absolutePath}, rag=${getRagMode()}")
    }

    private fun buildCaBundle() {
        if (caBundleFile.exists()) return
        val certDirs = listOf("/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts")
        val bundle = StringBuilder()
        for (dir in certDirs) {
            val d = File(dir)
            if (!d.exists()) continue
            d.listFiles()?.forEach { cert ->
                try { bundle.append(cert.readText()).append("\n") } catch (_: Exception) {}
            }
            if (bundle.isNotEmpty()) break
        }
        if (bundle.isNotEmpty()) {
            caBundleFile.writeText(bundle.toString())
            Log.i(TAG, "CA bundle written (${bundle.length} bytes)")
        }
    }

    private fun writeNullclawHomeConfig() {
        val apiKey = getApiKey() ?: return
        nullclawConfigFile.parentFile?.mkdirs()
        nullclawConfigFile.writeText("""
{
  "agents": {
    "defaults": {
      "model": {
        "primary": "anthropic/${getModel()}"
      }
    }
  },
  "providers": {
    "anthropic": {
      "api_key": "$apiKey"
    }
  }
}
""".trimIndent())
    }

    // ── RAG: web search ───────────────────────────────────────────────────────

    /** Returns true if the query likely needs current/live information. */
    private fun needsWebSearch(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return LIVE_INFO_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Universal web search — priority: Tavily → Brave → nothing.
     * Tavily: free 1000/mo, built for AI RAG, get key at tavily.com
     * Brave:  free 2000/mo, get key at brave.com/search/api
     * Set keys in admin panel at http://localhost:4747
     */
    private fun webSearch(query: String): List<Pair<String, String>> {
        val tavilyKey = getTavilyKey()
        if (!tavilyKey.isNullOrBlank()) return tavilySearch(query, tavilyKey)
        val braveKey = getBraveKey()
        if (!braveKey.isNullOrBlank()) return braveSearch(query, braveKey)
        Log.w(TAG, "No search API key set — add Tavily or Brave key in admin panel")
        return emptyList()
    }

    /** Tavily AI search — designed for RAG, returns clean snippets for any query. */
    private fun tavilySearch(query: String, apiKey: String): List<Pair<String, String>> {
        return try {
            val body = JSONObject().apply {
                put("api_key", apiKey)
                put("query", query)
                put("search_depth", "basic")
                put("max_results", 3)
                put("include_answer", true)
            }.toString()

            val url = URL("https://api.tavily.com/search")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            if (conn.responseCode != 200) {
                Log.w(TAG, "Tavily error ${conn.responseCode}")
                return emptyList()
            }

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val results = mutableListOf<Pair<String, String>>()

            // Direct answer if available
            val answer = json.optString("answer", "")
            if (answer.isNotBlank()) results.add(Pair("Direct answer", answer))

            // Web results
            val items = json.optJSONArray("results") ?: JSONArray()
            for (i in 0 until minOf(items.length(), 3 - results.size)) {
                val r = items.getJSONObject(i)
                val title = r.optString("title", "")
                val content = r.optString("content", "").take(200)
                if (content.isNotBlank()) results.add(Pair(title, content))
            }
            Log.i(TAG, "Tavily: ${results.size} results for '$query'")
            results
        } catch (e: Exception) {
            Log.w(TAG, "Tavily failed: ${e.message}")
            emptyList()
        }
    }

    private fun wmoDescription(code: Int) = when(code) {
        0 -> "Clear sky"; 1,2,3 -> "Partly cloudy"
        45,48 -> "Foggy"; 51,53,55 -> "Drizzle"; 61,63,65 -> "Rain"
        71,73,75 -> "Snow"; 80,81,82 -> "Rain showers"; 95 -> "Thunderstorm"
        else -> "Overcast"
    }

    private fun wttrSearch(query: String): List<Pair<String, String>> {
        return try {
            // Extract location from query
            val location = query.lowercase()
                .replace(Regex("\\b(weather|forecast|temperature|what's?|what is|the|is|in|today|tonight|now|currently|right now|please|tell me)\\b"), " ")
                .trim().replace(Regex("\\s+"), " ").ifBlank { "Berlin" }

            // Step 1: geocode the location
            val geoEncoded = URLEncoder.encode(location, "UTF-8")
            val geoUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$geoEncoded&count=1&format=json")
            val geoConn = geoUrl.openConnection() as HttpURLConnection
            geoConn.connectTimeout = 8_000; geoConn.readTimeout = 8_000
            if (geoConn.responseCode != 200) return emptyList()
            val geoJson = JSONObject(geoConn.inputStream.bufferedReader().readText())
            val results = geoJson.optJSONArray("results") ?: return emptyList()
            if (results.length() == 0) return emptyList()
            val place = results.getJSONObject(0)
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")
            val cityName = place.getString("name")
            val country = place.optString("country", "")

            // Step 2: get current weather
            val wxUrl = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,apparent_temperature" +
                "&temperature_unit=celsius&wind_speed_unit=kmh&format=json")
            val wxConn = wxUrl.openConnection() as HttpURLConnection
            wxConn.connectTimeout = 8_000; wxConn.readTimeout = 8_000
            if (wxConn.responseCode != 200) return emptyList()
            val wxJson = JSONObject(wxConn.inputStream.bufferedReader().readText())
            val current = wxJson.getJSONObject("current")

            val tempC = current.getDouble("temperature_2m")
            val feelsC = current.getDouble("apparent_temperature")
            val humidity = current.getInt("relative_humidity_2m")
            val wind = current.getDouble("wind_speed_10m")
            val code = current.getInt("weather_code")
            val desc = wmoDescription(code)

            val summary = "$desc, ${tempC}°C (feels like ${feelsC}°C). Humidity ${humidity}%, wind ${wind} km/h."
            Log.i(TAG, "Open-Meteo: $cityName $summary")
            listOf(Pair("Current weather in $cityName, $country", summary))
        } catch (e: Exception) {
            Log.w(TAG, "Open-Meteo failed: ${e.message}")
            emptyList()
        }
    }

    private fun braveSearch(query: String, apiKey: String): List<Pair<String, String>> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.search.brave.com/res/v1/web/search?q=$encoded&count=3")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Accept-Encoding", "gzip")
            conn.setRequestProperty("X-Subscription-Token", apiKey)
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000

            if (conn.responseCode != 200) return emptyList()

            val json = JSONObject(readResponseBody(conn))
            val results = json.optJSONObject("web")?.optJSONArray("results") ?: return emptyList()
            (0 until minOf(results.length(), 3)).map { i ->
                val r = results.getJSONObject(i)
                Pair(
                    r.optString("title", ""),
                    r.optString("description", "")
                )
            }.filter { it.second.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Brave search failed: ${e.message}")
            emptyList()
        }
    }

    private fun readResponseBody(conn: HttpURLConnection): String {
        val input = conn.inputStream
        val isGzip = (conn.contentEncoding ?: "").contains("gzip", ignoreCase = true)
        val stream = if (isGzip) GZIPInputStream(input) else input
        return stream.bufferedReader().use { it.readText() }
    }

    /**
     * DuckDuckGo HTML search — returns real web results for any query.
     * No API key needed. Scrapes the HTML search results page.
     */
    private fun duckDuckGoHtmlSearch(query: String): List<Pair<String, String>> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://html.duckduckgo.com/html/?q=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36")
            conn.setRequestProperty("Accept", "text/html")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (conn.responseCode != 200) return emptyList()

            val html = conn.inputStream.bufferedReader().readText()
            val results = mutableListOf<Pair<String, String>>()

            // Extract result snippets from DDG HTML
            val snippetRegex = Regex("""class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val titleRegex   = Regex("""class="result__a"[^>]*>(.*?)</a>""",    RegexOption.DOT_MATCHES_ALL)
            val snippets = snippetRegex.findAll(html).map {
                it.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            }.filter { it.isNotBlank() }.take(3).toList()
            val titles = titleRegex.findAll(html).map {
                it.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            }.filter { it.isNotBlank() }.take(3).toList()

            snippets.forEachIndexed { i, snippet ->
                val title = titles.getOrElse(i) { "" }
                results.add(Pair(title, snippet))
            }
            Log.i(TAG, "DDG HTML: ${results.size} results for '$query'")
            results
        } catch (e: Exception) {
            Log.w(TAG, "DDG HTML search failed: ${e.message}")
            emptyList()
        }
    }

    /** Build an augmented system prompt with web search results injected. */
    private fun buildRagSystemPrompt(basePrompt: String, results: List<Pair<String, String>>): String {
        if (results.isEmpty()) return basePrompt
        val sb = StringBuilder(basePrompt)
        sb.append("\n\nCurrent web information (use this to answer questions about recent/live data):\n")
        results.forEachIndexed { i, (title, snippet) ->
            sb.append("${i + 1}. ")
            if (title.isNotBlank()) sb.append("$title: ")
            sb.append(snippet)
            sb.append("\n")
        }
        sb.append("\nIf the web results are relevant, use them. Still keep response to 1-3 sentences.")
        return sb.toString()
    }

    // ── Main query entry point ────────────────────────────────────────────────

    suspend fun query(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val currentProvider = getProvider()
        if (!hasProviderApiKey(currentProvider)) {
            if (currentProvider != LLMProvider.NVIDIA && hasProviderApiKey(LLMProvider.NVIDIA)) {
                Log.w(TAG, "Provider $currentProvider missing key; falling back to NVIDIA")
                saveProvider(LLMProvider.NVIDIA)
                saveModel(DEFAULT_MODEL_NVIDIA)
            } else {
                return@withContext Result.failure(
                    RuntimeException("API key missing for provider: $currentProvider")
                )
            }
        }

        val apiKey = getApiKey()
            ?: return@withContext Result.failure(RuntimeException("API key missing for active provider"))

        val ragMode = getRagMode()
        clearConversationIfConfigChanged(ragMode)
        Log.i(TAG, "Query: '${prompt.take(60)}' rag=$ragMode")

        return@withContext when (ragMode) {
            "opus_tool" -> queryWithOpusTool(prompt, apiKey)
            "always"    -> queryWithKotlinRag(prompt, apiKey, forceSearch = true)
            "kotlin"    -> queryWithKotlinRag(prompt, apiKey, forceSearch = false)
            else        -> queryDirect(prompt, apiKey)
        }
    }

// ── Mode 1: Direct (no RAG) ───────────────────────────────────────────────

private suspend fun queryDirect(prompt: String, apiKey: String): Result<String> =
    withContext(Dispatchers.IO) {
        val config = createProviderConfig()
        callProviderMessages(
            config = config,
            maxTokens = getMaxTokens(),
            systemPrompt = getSystemPrompt(),
            userMessage = prompt
        )
    }

// ── Mode 2: Kotlin RAG — pre-search + inject ──────────────────────────────

private suspend fun queryWithKotlinRag(
    prompt: String,
    apiKey: String,
    forceSearch: Boolean
): Result<String> =
    withContext(Dispatchers.IO) {
        var systemPrompt = getSystemPrompt()
        if (forceSearch || needsWebSearch(prompt)) {
            Log.i(TAG, "Kotlin RAG: searching for '$prompt'")
            val results = webSearch(prompt)
            if (results.isNotEmpty()) {
                systemPrompt = buildRagSystemPrompt(systemPrompt, results)
                Log.i(TAG, "Kotlin RAG: injected ${results.size} results")
            }
        }

        val config = createProviderConfig()
        callProviderMessages(
            config = config,
            maxTokens = getMaxTokens(),
            systemPrompt = systemPrompt,
            userMessage = prompt
        )
    }

    // ── Mode 3: Opus Tool Use — Claude calls web_search, we execute ───────────

    private suspend fun queryWithOpusTool(prompt: String, apiKey: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Define web_search tool for Claude
                val tools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "web_search")
                        put("description",
                            "Search the web for current information. Use this when the user asks about " +
                            "recent events, live data, weather, news, prices, scores, or anything " +
                            "that requires up-to-date information.")
                        put("input_schema", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("query", JSONObject().apply {
                                    put("type", "string")
                                    put("description", "The search query")
                                })
                            })
                            put("required", JSONArray().apply { put("query") })
                        })
                    })
                }

                // First call — Claude decides whether to search
                val firstBody = JSONObject().apply {
                    put("model", getModel())
                    put("max_tokens", getMaxTokens() + 200) // extra tokens for tool call
                    put("system", getSystemPrompt())
                    put("tools", tools)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }.toString()

                val firstResponse = callAnthropicRaw(apiKey, firstBody)
                    ?: return@withContext Result.failure(RuntimeException("API error"))

                val stopReason = firstResponse.optString("stop_reason")

                // If Claude didn't call the tool, extract text directly
                if (stopReason != "tool_use") {
                    val text = firstResponse.getJSONArray("content")
                        .getJSONObject(0).getString("text").trim()
                    return@withContext Result.success(text)
                }

                // Claude called web_search — find the tool call
                val content = firstResponse.getJSONArray("content")
                var searchQuery = prompt // fallback
                var toolUseId = ""
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.optString("type") == "tool_use" &&
                        block.optString("name") == "web_search") {
                        searchQuery = block.optJSONObject("input")
                            ?.optString("query", prompt) ?: prompt
                        toolUseId = block.optString("id")
                        break
                    }
                }

                Log.i(TAG, "Opus tool: searching '$searchQuery'")
                val results = webSearch(searchQuery)
                val searchResultText = if (results.isNotEmpty()) {
                    results.joinToString("\n") { (title, snippet) ->
                        if (title.isNotBlank()) "$title: $snippet" else snippet
                    }
                } else {
                    "No results found for: $searchQuery"
                }

                // Second call — send search results back to Claude
                val secondBody = JSONObject().apply {
                    put("model", getModel())
                    put("max_tokens", getMaxTokens())
                    put("system", getSystemPrompt())
                    put("tools", tools)
                    put("messages", JSONArray().apply {
                        // Original user message
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                        // Claude's response with tool call
                        put(JSONObject().apply {
                            put("role", "assistant")
                            put("content", content)
                        })
                        // Tool result
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "tool_result")
                                    put("tool_use_id", toolUseId)
                                    put("content", searchResultText)
                                })
                            })
                        })
                    })
                }.toString()

                val secondResponse = callAnthropicRaw(apiKey, secondBody)
                    ?: return@withContext Result.failure(RuntimeException("API error on second call"))

                val finalText = secondResponse.getJSONArray("content")
                    .getJSONObject(0).getString("text").trim()

                Log.i(TAG, "Opus tool result: '${finalText.take(80)}'")
                Result.success(finalText)

            } catch (e: Exception) {
                Log.e(TAG, "Opus tool query error", e)
                // Fall back to direct query
                Log.w(TAG, "Falling back to direct query")
                queryDirect(prompt, apiKey)
            }
        }

// ── HTTP helpers ──────────────────────────────────────────────────────────

/** Generic LLM API call using provider configuration */
private fun callProviderRaw(config: ProviderConfig, body: String): JSONObject? {
    return try {
        lastProviderErrorDetail = null
        val url = URL(config.endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"

        // Set provider-specific headers
        config.getHeaders().forEach { (key, value) ->
            conn.setRequestProperty(key, value)
        }

        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        val code = conn.responseCode
        val responseText = if (code == 200)
            conn.inputStream.bufferedReader().readText()
        else
            conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"

        Log.i(TAG, "${config::class.simpleName} response code=$code")
        if (code != 200) {
            val compact = responseText.replace(Regex("\\s+"), " ").take(220)
            lastProviderErrorDetail = "HTTP $code: $compact"
            Log.e(TAG, "API error: $responseText")
            null
        } else {
            JSONObject(responseText)
        }
    } catch (e: Exception) {
        lastProviderErrorDetail = e.message ?: e.javaClass.simpleName
        Log.e(TAG, "HTTP error", e)
        null
    }
}

/** Convenience: call LLM API for a simple text response using provider config */
private fun callProviderMessages(
    config: ProviderConfig,
    maxTokens: Int,
    systemPrompt: String,
    userMessage: String
): Result<String> {
    val messages = buildMessagesWithContext(userMessage)
    val body = config.buildRequestBody(
        model = config.model,
        maxTokens = maxTokens,
        systemPrompt = systemPrompt,
        messages = messages
    ).toString()

    val response = callProviderRaw(config, body)
        ?: return Result.failure(
            RuntimeException(
                "API call failed (${config::class.simpleName}, model=${config.model}): " +
                    (lastProviderErrorDetail ?: "unknown error")
            )
        )

    return try {
        val text = config.extractResponseText(response)?.trim()
            ?: return Result.failure(RuntimeException("Failed to extract response text"))
        appendConversation("user", userMessage)
        appendConversation("assistant", text)
        Log.i(TAG, "Response: '${text.take(80)}'")
        Result.success(text)
    } catch (e: Exception) {
        Result.failure(RuntimeException("Failed to parse response: ${e.message}"))
    }
}

/** Legacy: Call Anthropic API directly (backward compatibility) */
private fun callAnthropicRaw(apiKey: String, body: String): JSONObject? {
    val config = ProviderConfig.AnthropicConfig(apiKey)
    return callProviderRaw(config, body)
}

/** Legacy: Call Anthropic messages (backward compatibility) */
private fun callAnthropicMessages(
    apiKey: String,
    model: String,
    maxTokens: Int,
    systemPrompt: String,
    userMessage: String
): Result<String> {
    val config = ProviderConfig.AnthropicConfig(apiKey, model)
    return callProviderMessages(config, maxTokens, systemPrompt, userMessage)
}

    private fun buildMessagesWithContext(userMessage: String): JSONArray {
        val snapshot = synchronized(conversationLock) { conversation.toList() }
        return JSONArray().apply {
            snapshot.forEach { turn ->
                put(JSONObject().apply {
                    put("role", turn.role)
                    put("content", turn.content)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }
    }

    private fun clearConversationIfConfigChanged(ragMode: String) {
        val fingerprint = buildString {
            append(getModel())
            append('\u001F')
            append(getSystemPrompt())
            append('\u001F')
            append(ragMode)
        }
        synchronized(conversationLock) {
            val previous = conversationConfigFingerprint
            if (previous == null) {
                conversationConfigFingerprint = fingerprint
                return
            }
            if (previous != fingerprint) {
                conversation.clear()
                conversationConfigFingerprint = fingerprint
                Log.i(TAG, "Conversation context cleared due to config change")
            }
        }
    }

    private fun appendConversation(role: String, content: String) {
        val normalized = content.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return
        synchronized(conversationLock) {
            conversation.addLast(ChatTurn(role, normalized.take(MAX_CONTEXT_CHARS_PER_MESSAGE)))
            while (conversation.size > MAX_CONTEXT_MESSAGES) {
                conversation.removeFirst()
            }
        }
    }

    fun isInstalled() = binaryFile.exists()
}
