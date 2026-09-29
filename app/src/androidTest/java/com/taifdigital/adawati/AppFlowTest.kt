package com.taifdigital.adawati

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun arabicQrCanBeCreatedAndSurvivesActivityRecreation() {
        // Navigate from any restored screen to the home page.
        if (rule.onAllNodesWithText("رجوع إلى الأدوات").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("رجوع إلى الأدوات").performClick()
        }
        rule.onNodeWithText("أدوات QR").performScrollTo().performClick()
        val text = "اختبار أدواتي بالعربية 💙"
        rule.onNodeWithText("الرابط أو النص").performTextReplacement(text)
        rule.onNodeWithText("إنشاء وحفظ QR").performScrollTo().performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithText("تم حفظ رمز QR في ملفاتي").fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText(text).assertExists()
        rule.onNodeWithText("حفظ نسخة باسم…").assertExists()
        rule.onNodeWithText("رجوع إلى الأدوات").performScrollTo().performClick()
        rule.onNodeWithText("ملفاتي").performScrollTo().performClick()
        rule.onAllNodesWithText("حفظ نسخة باسم…").onFirst().assertExists()
    }
}
