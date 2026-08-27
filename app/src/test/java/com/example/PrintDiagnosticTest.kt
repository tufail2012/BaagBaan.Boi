package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.util.PrintDiagnosticTrace
import com.example.util.ReceiptGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrintDiagnosticTest {

    @Test
    fun `test printReceiptBitmap initiates and logs trace`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)

        // Call printReceiptBitmap
        ReceiptGenerator.printReceiptBitmap(context, bitmap, "TEST-101")

        val logs = PrintDiagnosticTrace.logs
        assertTrue("Expected diagnostic logs to be populated", logs.isNotEmpty())
        assertTrue("Expected log to contain job or adapter information",
            logs.any { it.contains("TEST-101") || it.contains("PrintDocumentAdapter") || it.contains("PrintJob created") || it.contains("PrintManager") })
    }
}
