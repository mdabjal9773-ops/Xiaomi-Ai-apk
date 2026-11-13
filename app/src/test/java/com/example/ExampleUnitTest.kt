package com.example

import com.example.data.GeminiService
import com.example.model.AssistantPersona
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun assistantPersona_hasFemalePersonasAndAdultAge() {
        val babyJaan = AssistantPersona.BABY_JAAN
        assertTrue(babyJaan.titleBn.contains("বেবি জান"))
        assertTrue(babyJaan.greetingBn.contains("সোনা"))
        assertEquals(18, babyJaan.age)

        val zara = AssistantPersona.ZARA
        assertTrue(zara.titleBn.contains("জারা"))
        assertTrue(zara.age >= 18)

        val neelima = AssistantPersona.NEELIMA
        assertTrue(neelima.age >= 18)
    }

    @Test
    fun geminiService_smartBengaliResponse() = runBlocking {
        val service = GeminiService()
        val response = service.getAssistantResponse("তুমি কেমন আছো?", AssistantPersona.BABY_JAAN)
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        // Check for expressive emojis
        val hasEmoji = response.contains("❤️") || response.contains("🥰") || response.contains("🫶") || response.contains("🌹")
        assertTrue(hasEmoji)
    }

    @Test
    fun geminiService_understandsEnglishQueries() = runBlocking {
        val service = GeminiService()
        val response = service.getAssistantResponse("How are you baby?", AssistantPersona.BABY_JAAN)
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        assertTrue(response.contains("love") || response.contains("heart") || response.contains("wonderfully") || response.contains("smile"))
    }

    @Test
    fun geminiService_understandsHyperOsEnglish() = runBlocking {
        val service = GeminiService()
        val response = service.getAssistantResponse("Tell me about Xiaomi HyperOS features", AssistantPersona.ZARA)
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        assertTrue(response.contains("HyperOS") || response.contains("Xiaomi") || response.contains("Features") || response.contains("features"))
    }
}
