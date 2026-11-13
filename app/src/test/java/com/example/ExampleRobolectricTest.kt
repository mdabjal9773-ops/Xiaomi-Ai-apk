package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.XiaomiAssistantViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Xiaomi AI", appName)

    val ageGateTitle = context.getString(R.string.age_gate_title)
    assertTrue(ageGateTitle.contains("১৮+"))
  }

  @Test
  fun `age verification gate initial and confirmed flow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Clean up preferences first
    val prefs = context.getSharedPreferences(XiaomiAssistantViewModel.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val viewModel = XiaomiAssistantViewModel()
    viewModel.loadAgeVerification(context)
    // Initially unverified
    assertFalse(viewModel.isAgeVerified.value)

    // User confirms age gate
    viewModel.confirmAgeVerification(context)
    assertTrue(viewModel.isAgeVerified.value)
    assertTrue(prefs.getBoolean(XiaomiAssistantViewModel.KEY_AGE_VERIFIED_18, false))

    // Re-loading from disk maintains true
    val freshViewModel = XiaomiAssistantViewModel()
    freshViewModel.loadAgeVerification(context)
    assertTrue(freshViewModel.isAgeVerified.value)

    // Resetting age gate
    viewModel.resetAgeVerification(context)
    assertFalse(viewModel.isAgeVerified.value)
    assertFalse(prefs.getBoolean(XiaomiAssistantViewModel.KEY_AGE_VERIFIED_18, false))
  }
}
