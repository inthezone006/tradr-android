package com.rahul.stocksim.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rahul.stocksim.BuildConfig
import android.util.Log
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val gson = Gson()
    
    // Config for structured JSON output
    private val jsonConfig = generationConfig {
        responseMimeType = "application/json"
    }

    private val models = listOf(
        GenerativeModel(modelName = "gemini-3.1-flash-lite-preview", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-2.5-flash-lite", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = apiKey),
        GenerativeModel(modelName = "gemma-3-1b-it", apiKey = apiKey),
        GenerativeModel(modelName = "gemma-3-4b-it", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey)
    )

    private val jsonModels = listOf(
        GenerativeModel(modelName = "gemini-3.1-flash-lite-preview", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemini-2.5-flash-lite", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemma-3-1b-it", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemma-3-4b-it", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey, generationConfig = jsonConfig),
        GenerativeModel(modelName = "gemini-1.5-pro", apiKey = apiKey, generationConfig = jsonConfig)
    )

    suspend fun generateStockAnalysis(
        stock: Stock,
        news: List<FinnhubNewsArticle>,
        financials: FinnhubFinancialsResponse?
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext "AI Analysis unavailable: Gemini API Key is missing. Please add it to your local.properties or gradle.properties."
        }

        val prompt = """
            Analyze the stock ${stock.symbol} (${stock.name}).
            Current Price: ${'$'}${stock.price} (${stock.percentChange}% change today).
            
            Latest News Headlines:
            ${news.take(5).joinToString("\n") { "- " + it.headline }}
            
            Financial Metrics:
            ${financials?.metric?.entries?.take(10)?.joinToString("\n") { "${it.key}: ${it.value}" }}
            
            Provide a concise summary (max 150 words) including:
            1. Current Sentiment.
            2. Key Risks.
            3. Outlook for the next month.
            
            Use Markdown formatting like **bold** for emphasis on key terms.
        """.trimIndent()

        var lastException: Exception? = null

        for (model in models) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text != null) return@withContext text
            } catch (e: Exception) {
                lastException = e
                // If it's a fatal key error, stop trying
                if (e.message?.contains("API_KEY_INVALID") == true) break
                // Log the attempt failure to help debugging which models are failing
                FirebaseCrashlytics.getInstance().log("Model ${model.modelName} failed: ${e.message}")
                continue
            }
        }

        // If we reach here, all models failed
        val e = lastException ?: return@withContext "The AI was unable to generate a response."
        
        // Log the final exception to Firebase (ignore cancellations and timeouts)
        val isCancellation = e is kotlinx.coroutines.CancellationException || 
                           e is java.util.concurrent.CancellationException ||
                           e.cause is kotlinx.coroutines.CancellationException ||
                           e.cause is java.util.concurrent.CancellationException

        if (!isCancellation && e !is java.net.SocketTimeoutException) {
            FirebaseCrashlytics.getInstance().apply {
                recordException(e)
                setCustomKey("gemini_stock", stock.symbol)
                setCustomKey("gemini_error_msg", e.message ?: "unknown")
            }
        }

        val errorMsg = e.message ?: ""
        when {
            errorMsg.contains("404") -> "AI Insights are currently unavailable (Model not found)."
            errorMsg.contains("429") -> "Quota exceeded on all available models. Please try again later."
            errorMsg.contains("API_KEY_INVALID") -> "Invalid API Key. Please check your Gemini configuration."
            else -> "AI Analysis is temporarily unavailable. ${e.message}"
        }
    }

    suspend fun analyzePortfolio(
        portfolio: List<Pair<Stock, Long>>,
        balance: Double
    ): PortfolioAnalysis? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || portfolio.isEmpty()) return@withContext null

        val portfolioStr = portfolio.joinToString("\n") { (stock, qty) ->
            "- ${stock.symbol} (${stock.name}): $qty shares at $${stock.price}"
        }

        val prompt = """
            Analyze the following stock portfolio and cash balance.
            Cash Balance: $${String.format("%.2f", balance)}
            
            Portfolio:
            $portfolioStr
            
            Provide a structured analysis in JSON format with the following fields:
            - riskLevel: String (Low, Medium, High)
            - diversificationScore: Int (0-100)
            - recommendations: List of Strings (Specific actions to take)
            - outlook: String (Short-term market outlook for this portfolio)
        """.trimIndent()

        for (model in jsonModels) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text != null) {
                    return@withContext gson.fromJson(text, PortfolioAnalysis::class.java)
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Portfolio analysis failed with ${model.modelName}", e)
                continue
            }
        }
        null
    }

    suspend fun generateMarketRecommendations(
        gainers: List<Stock>,
        losers: List<Stock>,
        indices: List<Stock>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext "AI Recommendations unavailable."

        val prompt = """
            Analyze the current market state and provide investment recommendations.
            
            Top Gainers:
            ${gainers.take(5).joinToString("\n") { "- ${it.symbol}: ${it.percentChange}%" }}
            
            Top Losers:
            ${losers.take(5).joinToString("\n") { "- ${it.symbol}: ${it.percentChange}%" }}
            
            Global Indices:
            ${indices.take(5).joinToString("\n") { "- ${it.symbol}: ${it.percentChange}%" }}
            
            Based on this data, provide:
            1. Short-term strategy (Buy/Sell/Hold types).
            2. 3 specific stocks or sectors to watch and WHY.
            3. Overall market sentiment analysis.
            
            Keep it professional, concise, and highlight key tickers in **bold**.
        """.trimIndent()

        for (model in models) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text != null) return@withContext text
            } catch (e: Exception) {
                continue
            }
        }
        "Unable to generate recommendations at this time."
    }
}

data class PortfolioAnalysis(
    val riskLevel: String,
    val diversificationScore: Int,
    val recommendations: List<String>,
    val outlook: String
)
