package sadaqat.kinzadigitalhub.electricmetertracker.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import sadaqat.kinzadigitalhub.electricmetertracker.model.BillingSettings
import sadaqat.kinzadigitalhub.electricmetertracker.model.CalculationSummary
import sadaqat.kinzadigitalhub.electricmetertracker.model.Meter
import sadaqat.kinzadigitalhub.electricmetertracker.model.MeterReading
import sadaqat.kinzadigitalhub.electricmetertracker.model.TariffSlab

class MeterTrackerState {
    var meters by mutableStateOf(
        listOf(
            Meter(id = "1", name = "Home Main Meter", meterNumber = "MTR-88231", initialReading = 12500.0, billingCycleDay = 1),
            Meter(id = "2", name = "Upper Floor Sub-Meter", meterNumber = "SUB-44120", initialReading = 3400.0, billingCycleDay = 1)
        )
    )
        private set

    var selectedMeterId by mutableStateOf("1")

    var readings by mutableStateOf(
        listOf(
            MeterReading(id = "r1", meterId = "1", readingValue = 12500.0, dateEpochMillis = 1724000000000L, notes = "Billing start cycle"),
            MeterReading(id = "r2", meterId = "1", readingValue = 12620.0, dateEpochMillis = 1724300000000L, notes = "Mid week check"),
            MeterReading(id = "r3", meterId = "1", readingValue = 12785.0, dateEpochMillis = 1724700000000L, notes = "Heavy AC usage weekend"),
            MeterReading(id = "r4", meterId = "1", readingValue = 12890.0, dateEpochMillis = 1725000000000L, notes = "Latest reading"),
            MeterReading(id = "r5", meterId = "2", readingValue = 3400.0, dateEpochMillis = 1724000000000L, notes = "Start reading"),
            MeterReading(id = "r6", meterId = "2", readingValue = 3495.0, dateEpochMillis = 1725000000000L, notes = "End of month")
        )
    )
        private set

    var billingSettings by mutableStateOf(BillingSettings())

    val selectedMeter: Meter?
        get() = meters.find { it.id == selectedMeterId } ?: meters.firstOrNull()

    val currentMeterReadings: List<MeterReading>
        get() = readings
            .filter { it.meterId == selectedMeterId }
            .sortedByDescending { it.dateEpochMillis }

    fun addReading(readingValue: Double, dateMillis: Long, notes: String) {
        val newId = "r_${System.currentTimeMillis()}"
        val newReading = MeterReading(
            id = newId,
            meterId = selectedMeterId,
            readingValue = readingValue,
            dateEpochMillis = dateMillis,
            notes = notes
        )
        readings = readings + newReading
    }

    fun deleteReading(readingId: String) {
        readings = readings.filterNot { it.id == readingId }
    }

    fun addMeter(name: String, meterNumber: String, initialReading: Double) {
        val newId = "m_${System.currentTimeMillis()}"
        val meter = Meter(
            id = newId,
            name = name,
            meterNumber = meterNumber,
            initialReading = initialReading
        )
        meters = meters + meter
        selectedMeterId = newId
    }

    fun calculateCost(units: Double, settings: BillingSettings): Double {
        if (units <= 0) return 0.0
        var remaining = units
        var energyCharge = 0.0
        for (slab in settings.slabs) {
            val slabCapacity = slab.toUnit - slab.fromUnit + 1
            if (remaining > 0) {
                val unitsInThisSlab = minOf(remaining, slabCapacity)
                energyCharge += unitsInThisSlab * slab.ratePerUnit
                remaining -= unitsInThisSlab
            }
        }
        val tax = (energyCharge + settings.fixedCharges) * (settings.taxPercentage / 100.0)
        return energyCharge + settings.fixedCharges + tax
    }

    fun getSummary(): CalculationSummary {
        val meter = selectedMeter ?: return CalculationSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val sortedAsc = readings.filter { it.meterId == meter.id }.sortedBy { it.dateEpochMillis }
        
        val totalConsumed = if (sortedAsc.isNotEmpty()) {
            val first = sortedAsc.first().readingValue
            val last = sortedAsc.last().readingValue
            maxOf(0.0, last - first)
        } else {
            0.0
        }

        var energyCharge = 0.0
        var tempUnits = totalConsumed
        for (slab in billingSettings.slabs) {
            val slabCapacity = slab.toUnit - slab.fromUnit + 1
            if (tempUnits > 0) {
                val take = minOf(tempUnits, slabCapacity)
                energyCharge += take * slab.ratePerUnit
                tempUnits -= take
            }
        }

        val fixed = billingSettings.fixedCharges
        val tax = (energyCharge + fixed) * (billingSettings.taxPercentage / 100.0)
        val totalBill = energyCharge + fixed + tax

        val daysCount = if (sortedAsc.size > 1) {
            val diffMs = sortedAsc.last().dateEpochMillis - sortedAsc.first().dateEpochMillis
            maxOf(1, (diffMs / (1000 * 60 * 60 * 24)).toInt())
        } else {
            1
        }
        val dailyAvg = if (daysCount > 0) totalConsumed / daysCount else 0.0
        val projectedUnits = dailyAvg * 30
        val projectedBill = calculateCost(projectedUnits, billingSettings)

        return CalculationSummary(
            totalUnitsConsumed = totalConsumed,
            energyCharges = energyCharge,
            fixedCharges = fixed,
            taxAmount = tax,
            totalBillAmount = totalBill,
            averageDailyUnits = dailyAvg,
            projectedUnits = projectedUnits,
            projectedBillAmount = projectedBill
        )
    }
}
