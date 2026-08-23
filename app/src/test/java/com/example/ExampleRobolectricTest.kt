package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
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
    assertEquals("Electric Meter Tracker", appName)
  }

  @Test
  fun `units calculation and 100 unit alert under threshold`() {
    val previousBill = 1250.0
    val currentReading = 1320.0
    val unitsSinceBill = currentReading - previousBill
    val isAlert = unitsSinceBill >= 100.0

    assertEquals(70.0, unitsSinceBill, 0.001)
    assertFalse(isAlert)
  }

  @Test
  fun `units calculation and 100 unit high usage alert trigger`() {
    val previousBill = 1250.0
    val currentReading = 1360.0
    val unitsSinceBill = currentReading - previousBill
    val isAlert = unitsSinceBill >= 100.0

    assertEquals(110.0, unitsSinceBill, 0.001)
    assertTrue(isAlert)
  }

  @Test
  fun `reading model data integrity with sync metadata`() {
    val reading = DailyReading(
      id = 1L,
      meterId = 1L,
      meterName = "Muhammad Iqbal S/O Luqman",
      dateString = "14 Aug 2026",
      previousBillReading = 1200.0,
      currentReading = 1315.0,
      unitsSinceBill = 115.0,
      isAlert = true,
      alertStatusText = "ALERT",
      deviceId = "device_test_123",
      syncStatus = "PENDING"
    )

    assertEquals(115.0, reading.unitsSinceBill, 0.001)
    assertTrue(reading.isAlert)
    assertEquals("ALERT", reading.alertStatusText)
    assertEquals("PENDING", reading.syncStatus)
  }
}
