package com.example.habtrack.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calls Claude to turn a plain-text summary of the user's habit data into a short,
 * motivating coaching insight. Runs blocking SDK calls off the main thread.
 */
object HabitInsightsService {

    private val SYSTEM_PROMPT = """
        You are an encouraging, concise habit-coaching assistant inside the HabTrack app.
        You will be given a summary of the user's habits, streaks, completion rates, and
        any detected correlations between habits. Give 3-5 short, specific, motivating
        insights or suggestions, referencing actual habit names and numbers from the
        summary. Keep the whole response under 150 words. Plain sentences or a short
        list using "-" — no markdown headers, no bold.
    """.trimIndent()

    suspend fun generateInsights(apiKey: String, habitSummary: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client: AnthropicClient = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build()

                val params = MessageCreateParams.builder()
                    .model(Model.of("claude-haiku-4-5"))
                    .maxTokens(1024L)
                    .system(SYSTEM_PROMPT)
                    .addUserMessage(habitSummary)
                    .build()

                val response = client.messages().create(params)

                // Safe extraction of text from the response content blocks
                response.content().joinToString("\n") { block ->
                    block.text().map { it.text() }.orElse("")
                }.ifBlank { "Claude didn't return any insights. Please try again." }
            }
        }
}
