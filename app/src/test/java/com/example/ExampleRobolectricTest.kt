package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UDM Mobile Repair", appName)
    }

    @Test
    fun `test 4-digit job number formatting`() {
        val num = 14
        val formatted = String.format(Locale.US, "%04d", num)
        assertEquals("0014", formatted)

        val singleDigit = 1
        assertEquals("0001", String.format(Locale.US, "%04d", singleDigit))

        val fourDigit = 1250
        assertEquals("1250", String.format(Locale.US, "%04d", fourDigit))
    }

    @Test
    fun `test job number normalization from OCR`() {
        val rawOcr1 = "JOB NO: 0014"
        val digits1 = rawOcr1.replace(Regex("[^0-9]"), "")
        val normalized1 = String.format(Locale.US, "%04d", digits1.toInt())
        assertEquals("0014", normalized1)

        val rawOcr2 = "14"
        val digits2 = rawOcr2.replace(Regex("[^0-9]"), "")
        val normalized2 = String.format(Locale.US, "%04d", digits2.toInt())
        assertEquals("0014", normalized2)
    }
}
