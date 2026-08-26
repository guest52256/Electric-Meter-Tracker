package sadaqat.kinzadigitalhub.electricmetertracker.model

data class Meter(
    val id: String,
    val name: String,
    val meterNumber: String,
    val initialReading: Double = 0.0,
    val billingCycleDay: Int = 1
)

data class MeterReading(
    val id: String,
    val meterId: String,
    val readingValue: Double,
    val dateEpochMillis: Long,
    val notes: String = "",
    val unitsConsumed: Double = 0.0,
    val estimatedCost: Double = 0.0
)

data class TariffSlab(
    val fromUnit: Double,
    val toUnit: Double,
    val ratePerUnit: Double
)

data class BillingSettings(
    val currencySymbol: String = "$",
    val fixedCharges: Double = 5.0,
    val taxPercentage: Double = 16.0,
    val slabs: List<TariffSlab> = listOf(
        TariffSlab(0.0, 100.0, 0.15),
        TariffSlab(101.0, 300.0, 0.22),
        TariffSlab(301.0, 700.0, 0.30),
        TariffSlab(701.0, Double.MAX_VALUE, 0.38)
    )
)

data class CalculationSummary(
    val totalUnitsConsumed: Double,
    val energyCharges: Double,
    val fixedCharges: Double,
    val taxAmount: Double,
    val totalBillAmount: Double,
    val averageDailyUnits: Double,
    val projectedUnits: Double,
    val projectedBillAmount: Double
)
