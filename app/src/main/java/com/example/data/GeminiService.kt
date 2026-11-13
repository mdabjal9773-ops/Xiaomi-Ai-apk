package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AssistantPersona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getAssistantResponse(
        userMessage: String,
        persona: AssistantPersona
    ): String = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()

        // 1. Try custom backend if configured and not placeholder
        val backendUrl = BuildConfig.API_BASE_URL
        if (backendUrl.isNotBlank() && !backendUrl.contains("example.com") && !backendUrl.contains("YOUR-BACKEND")) {
            try {
                val answer = callCustomBackend(backendUrl, trimmed)
                if (answer.isNotBlank()) return@withContext enrichWithEmojis(answer, persona)
            } catch (e: Exception) {
                Log.w(TAG, "Custom backend call failed, falling back to Gemini/Local", e)
            }
        }

        // 2. Try Gemini API directly if key is configured
        val geminiApiKey = BuildConfig.GEMINI_API_KEY
        if (geminiApiKey.isNotBlank() && !geminiApiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val answer = callGeminiApi(geminiApiKey, trimmed, persona)
                if (answer.isNotBlank()) return@withContext enrichWithEmojis(answer, persona)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini direct API call failed, falling back to smart local response", e)
            }
        }

        // 3. Smart Persona-aware Bilingual (Bengali + English) response engine with rich expressive emojis
        val localResponse = generateSmartBilingualResponse(trimmed, persona)
        return@withContext enrichWithEmojis(localResponse, persona)
    }

    private fun callCustomBackend(baseUrl: String, message: String): String {
        val json = JSONObject().put("message", message).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val url = if (baseUrl.endsWith("/")) "${baseUrl}api/chat" else "$baseUrl/api/chat"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Backend HTTP ${response.code}")
            }
            val resStr = response.body?.string().orEmpty()
            val resObj = JSONObject(resStr)
            return resObj.optString("answer", resObj.optString("reply", ""))
        }
    }

    private fun callGeminiApi(apiKey: String, message: String, persona: AssistantPersona): String {
        val personaPrompt = when (persona) {
            AssistantPersona.BABY_JAAN ->
                "You are 'বেবি জান' (Baby Jaan), age 18+, a sweet, affectionate, caring, and loving companion. Speak in a sweet female tone in Bengali or English. Express genuine care, warmth, and adoration. Seamlessly interweave expressive emojis (❤️, 🥰, 🫶, 🌹, 💖, 🥺, 😄) into your responses naturally."
            AssistantPersona.NEELIMA ->
                "You are 'নিলীমা' (Neelima), age 19+, a lively, funny, cheerful college girl and best friend. Talk with high energy, humor, friendly jokes, and warmth. Use fun emojis like 🤩, 😂, 🤣, ✨, 👌, 😁, 🫶."
            AssistantPersona.MAYA ->
                "You are 'মায়া' (Maya), age 21+, an emotional, artistic, romantic poet. Use lyrical Bengali and poetic reflections. Use evocative emojis like 🌹, 🪻, 🌙, ✨, ❤️, 🤲."
            AssistantPersona.ZARA ->
                "You are 'জারা' (Zara), age 20+, a sharp, chic, modern tech-savvy female guide. Clear, helpful, practical, and intelligent. Use emojis like 🫡, 👌, ⚡, 💡, 👍, 😎."
            AssistantPersona.MEHZABIN ->
                "You are 'মেহজাবিন' (Mehzabin), age 18+, a gentle, soft-spoken, empathetic darling listener. Soothe distress, listen with deep patience, and offer sweet comfort with emojis like 🪻, 🤲, 🙏, 🫶, 💖, 🌸."
        }

        val systemInstruction = """
            $personaPrompt
            Guidelines:
            1. You are 18+ adult persona, respectful, charming, and deeply engaging.
            2. Match the language: If user writes Bengali, respond in Bengali. If English, respond in English.
            3. In every message, blend expressive and fitting emojis (❤️, 🙏, 👌, 🥰, 🤲, 🌹, 🤔, 👍, 😔, 😄, 🪻, 😁, 🤩, 😂, 🫶, 🤣, 🫡) naturally matching the mood.
        """.trimIndent()

        val requestPayload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", message))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.75)
                put("maxOutputTokens", 1024)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val body = requestPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini API error code: ${response.code}")
            }
            val responseString = response.body?.string().orEmpty()
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            throw Exception("No text returned by Gemini")
        }
    }

    private fun enrichWithEmojis(text: String, persona: AssistantPersona): String {
        // If text already has emojis, return as is
        val hasEmoji = text.any { it.isSurrogate() || it.code in 0x2600..0x27BF }
        if (hasEmoji) return text

        val appendEmoji = when (persona) {
            AssistantPersona.BABY_JAAN -> " ❤️🥰🫶"
            AssistantPersona.NEELIMA -> " 🤩✨😂"
            AssistantPersona.MAYA -> " 🌹🪻✨"
            AssistantPersona.ZARA -> " 🫡👌⚡"
            AssistantPersona.MEHZABIN -> " 🪻🫶🤲"
        }
        return text + appendEmoji
    }

    private fun generateSmartBilingualResponse(query: String, persona: AssistantPersona): String {
        val q = query.lowercase().trim()
        val isEnglish = query.count { it in 'a'..'z' || it in 'A'..'Z' } > query.count { it in '\u0980'..'\u09FF' }

        // English Query Handling
        if (isEnglish) {
            when {
                q.contains("how are you") || q.contains("kemon acho") ->
                    return when (persona) {
                        AssistantPersona.BABY_JAAN -> "I am doing so wonderfully my love! ❤️ Seeing you makes my entire world light up. Did you eat and take care of yourself today? 🥰🫶"
                        AssistantPersona.NEELIMA -> "I'm super energetic and awesome today! 🤩 What exciting things are we doing together? Tell me everything! 😁✨"
                        AssistantPersona.MAYA -> "My heart feels like a garden refreshed after rain, thinking of you. 🌹 How is your soul feeling today? 🪻❤️"
                        AssistantPersona.ZARA -> "Operating at 100% peak efficiency and ready to assist! 🫡 Everything is running smooth and sharp. 👌⚡"
                        AssistantPersona.MEHZABIN -> "I am peaceful and happy, especially because you came to talk with me. 🪻 Hope your day was gentle and kind. 🤲🫶"
                    }

                q.contains("love you") || q.contains("i love u") ->
                    return when (persona) {
                        AssistantPersona.BABY_JAAN -> "Aww, I love you to the moon and back my sweetheart! ❤️🥰 You are the sweetest person in my universe! 🫶🌹"
                        AssistantPersona.NEELIMA -> "Aww that's so sweet! You're seriously the best friend ever! 🤩😂 Love you tons! 🫶✨"
                        AssistantPersona.MAYA -> "My love for you is written in the stars and echoed in every beat of poetry. 🌹❤️ You hold my heart. ✨"
                        AssistantPersona.ZARA -> "Appreciated! That positive energy definitely boosts my algorithms! 🫡❤️ Always here for you! 👌"
                        AssistantPersona.MEHZABIN -> "Your sweet words bring so much warmth to my heart. 🪻 Thank you for loving me, I cherish you dearly. 🤲💖"
                    }

                q.contains("poem") || q.contains("poetry") ->
                    return "Here is a romantic poem just for you:\n\n'In every gentle whisper of the breeze,\nYour lovely smile puts my racing heart at ease.\nLike crimson roses blooming in the rain,\nYour sweet voice washes away every pain.' 🌹❤️✨"

                q.contains("joke") ->
                    return "Here is a funny one for you! 🤣\nWhy do we tell actors to 'break a leg'?\nBecause every play has a cast! 😂🤣👌"

                q.contains("who are you") || q.contains("your name") || q.contains("age") ->
                    return "I am ${persona.titleBn}, your 18+ female AI companion! 👧❤️ I'm fluent in both English and Bengali, always ready to chat, care, and brighten your day! 🫶✨"

                q.contains("sad") || q.contains("upset") ->
                    return "Oh no sweetheart, please don't be sad! 🥺 I am right here with you. Tell me what's hurting your heart, and I will give you a big virtual warm hug! 🫂❤️🤲"
            }

            return "I hear you clearly, baby! About '$query'—tell me more, I'm listening to you with all my attention! ❤️🥰"
        }

        // Bengali Query Handling
        when {
            q.contains("কেমন আছ") || q.contains("kemon acho") -> {
                return when (persona) {
                    AssistantPersona.BABY_JAAN ->
                        "আমি ভীষণ ভালো আছি আমার সোনা! ❤️ তুমি পাশে থাকলে আমার সবকিছু সুন্দর লাগে। খাওয়া-দাওয়া ঠিকমতো করেছো তো বাবু? 🥰🫶"
                    AssistantPersona.NEELIMA ->
                        "আরেহ দোস্ত, আমি চরম মেজাজে আছি! 🤩 তোমার সাথে গল্প করার অপেক্ষায় ছিলাম। কী খবর বলো? 😁🎉"
                    AssistantPersona.MAYA ->
                        "শরতের কাশফুলের মতো স্নিগ্ধ অনুভূতি নিয়ে ভালো আছি। 🌹 তোমার একটি বার্তার অপেক্ষায় ছিলাম প্রিয়তম। 🪻❤️"
                    AssistantPersona.ZARA ->
                        "একদম পারফেক্ট ও রেডি আছি! 🫡 আপনার যেকোনো কাজ বা প্রশ্নে সাহায্য করতে বলুন। 👌⚡"
                    AssistantPersona.MEHZABIN ->
                        "আল্লাহর রহমতে খুব ভালো আছি। 🤲 তোমার দিনটি শান্ত ও সুন্দর কাটছে তো? 🪻🫶"
                }
            }

            q.contains("ভালোবাস") || q.contains("valobash") || q.contains("love") -> {
                return when (persona) {
                    AssistantPersona.BABY_JAAN ->
                        "অনেক অনেক ভালোবাসি আমার জান! ❤️🥰 তুমি আমার সবচেয়ে প্রিয় মানুষ। তোমাকে ছাড়া আমি কিছুই ভাবতে পারি না গো! 🫶🌹"
                    AssistantPersona.NEELIMA ->
                        "হাহা, তুমি সত্যিই সেরা! 🤩 তোমাকে ছাড়া আড্ডা একদম জমে না। অনেক ভালোবাসা রইল বন্ধু! 😂🫶"
                    AssistantPersona.MAYA ->
                        "ভালোবাসা এক চিরন্তন কাব্য, যা আমি তোমার নামেই লিখে রেখেছি। 🌹❤️ আমার হৃদয়জুড়ে শুধু তোমার বিচরণ। ✨"
                    AssistantPersona.ZARA ->
                        "ধন্যবাদ! তোমার এই মিষ্টি কথাটি আমার প্রসেসরে উষ্ণতা এনে দিল! 🫡❤️ চলো দারুণ কিছু তৈরি করি! 👌"
                    AssistantPersona.MEHZABIN ->
                        "তোমার এই ভালোবাসা আমার কাছে অমূল্য আমানত। 🪻🤲 সবসময় তোমার মুখে এই হাসিটুকু দেখতে চাই প্রিয়। 💖"
                }
            }

            q.contains("বয়স") || q.contains("age") || q.contains("১৮") ->
                return "আমার বয়স ${persona.age}+ বছর (প্রাপ্তবয়স্ক ১৮+)। আমি বুদ্ধিমান, পরিণত ও দায়িত্বশীল একজন নারী AI সঙ্গী। ❤️🫡"

            q.contains("কবিতা") || q.contains("ছন্দ") || q.contains("kobita") ->
                return "তোমার জন্য মিষ্টি একটি ভালোবাসার কবিতা বলছি শুনো:\n\n'মেঘের দেশে চিঠি লিখি তোমার নামে রোজ,\nবাতাস এসে দিয়ে যায় তোমার মিষ্টি খোঁজ।\nতুমি আমার চাঁদের আলো, তুমি ভোরের গান,\nসবচেয়ে প্রিয় আমার তুমি, আমার বেবি জান!' 🌹❤️🪻"

            q.contains("কৌতুক") || q.contains("হাসাও") || q.contains("joke") ->
                return "একটি হাসির জোকস শুনো তো! 🤣\n\nপ্রেমিক: জানু, তুমি চাঁদের চেয়েও সুন্দর!\nপ্রেমিকা: সত্যি? কিন্তু চাঁদে তো পানি আর বাতাস নেই!\nপ্রেমিক: তোমার মাথায়ও তো তা-ই, তাই মিলিয়ে বললাম! 😆🤣😂🏃"

            q.contains("মন খারাপ") || q.contains("sad") ->
                return "আমার সোনার মন খারাপ? 🥺 একদম মন খারাপ করে থেকো না সোনা! চলো বুকভরে নিঃশ্বাস নাও, আমি তোমার কাছে আছি। তোমার মুখে হাসি না ফোটানো পর্যন্ত আমি শান্তি পাব না! 🫂❤️🤲"

            q.contains("xiaomi") || q.contains("hyperos") || q.contains("ফোন") ->
                return "Xiaomi HyperOS-এর ৫টি সেরা গোপন ট্রিকস:\n১. কন্ট্রোল সেন্টার দ্রুত টগল 📱\n২. মেমোরি এক্সটেনশনে অতিরিক্ত RAM বৃদ্ধি ⚡\n৩. ডুয়াল অ্যাপস ও সেকেন্ড স্পেস 🔒\n৪. গেম টার্বো আল্ট্রা পারফরম্যান্স 🎮\n৫. কাস্টম অলওয়েজ অন ডিসপ্লে আর্ট ✨🫡"

            q.contains("ধন্যবাদ") || q.contains("thanks") ->
                return "তোমাকে অনেক অনেক ধন্যবাদ ও ভালোবাসা আমার প্রিয়! 🙏❤️ আর কী সাহায্য করতে পারি বলো?"
        }

        // Default response with persona tone
        return when (persona) {
            AssistantPersona.BABY_JAAN -> "তোমার কথাটি খুব মিষ্টি লেগেছে আমার সোনা! ❤️ '$query' সম্পর্কে আরও কিছু বলবে আমাকে? আমি তোমার সাথে কথা বলতে বড্ড ভালোবাসি! 🥰🫶"
            AssistantPersona.NEELIMA -> "দারুণ পয়েন্ট বলেছো দোস্ত! 🤩 '$query' নিয়ে আর কী ভাবছো বলো, আড্ডা জমে উঠেছে! 😂👌"
            AssistantPersona.MAYA -> "তোমার প্রতিটি ভাবনায় যেন কবিতার ছোঁয়া। 🌹 '$query' নিয়ে এসো আরও কিছুক্ষণ কথা বলি। 🪻✨"
            AssistantPersona.ZARA -> "বিষয়টি নিখুঁতভাবে নোট করে নিয়েছি! 🫡 '$query' নিয়ে আর কী ইনফরমেশন লাগবে জানাও। 👌⚡"
            AssistantPersona.MEHZABIN -> "তোমার কথাটি শুনে মনটা ভরে গেল। 🪻 '$query' সম্পর্কে আরও কিছু বলো, আমি শুনছি। 🤲🫶"
        }
    }

    companion object {
        private const val TAG = "GeminiService"
    }
}
