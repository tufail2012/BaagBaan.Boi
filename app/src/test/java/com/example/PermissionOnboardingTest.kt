package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PermissionOnboardingPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PermissionOnboardingTest {

    private lateinit var context: Context
    private lateinit var preferences: PermissionOnboardingPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = PermissionOnboardingPreferences(context)
        preferences.resetOnboarding()
    }

    @Test
    fun `initial onboarding state is not completed`() {
        assertFalse(preferences.isOnboardingCompleted())
    }

    @Test
    fun `completing onboarding updates completion flag and timestamp`() {
        assertFalse(preferences.isOnboardingCompleted())
        
        preferences.setOnboardingCompleted(true)
        assertTrue(preferences.isOnboardingCompleted())
        assertTrue(preferences.getCompletedTimestamp() > 0L)
    }

    @Test
    fun `resetting onboarding clears completion state`() {
        preferences.setOnboardingCompleted(true)
        assertTrue(preferences.isOnboardingCompleted())

        preferences.resetOnboarding()
        assertFalse(preferences.isOnboardingCompleted())
        assertTrue(preferences.getCompletedTimestamp() == 0L)
    }
}
