package com.example

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.AnmaSoftFooterBar
import com.example.ui.theme.HisabatiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnmaSoftFooterUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun footerBar_rendersDeveloperCreditAndDialerLaunch() {
        composeTestRule.setContent {
            HisabatiTheme {
                AnmaSoftFooterBar()
            }
        }

        composeTestRule.onNodeWithText("عمل انما سوفت").assertIsDisplayed()
        composeTestRule.onNodeWithText("772317950").assertIsDisplayed()
        composeTestRule.onNodeWithTag("anma_soft_footer_phone").performClick()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_DIAL, startedIntent.action)
        assertEquals("tel:772317950", startedIntent.dataString)
    }
}
