package dev.zelenzoom.rowingbridge.ble

/**
 * Per-model corrections for machines whose FTMS implementation deviates from
 * the spec in a known, confirmed way. Defaults trust the spec as-is - only
 * add a field here once a deviation has been empirically confirmed on real
 * hardware, never guessed for an untested machine.
 */
data class RowerQuirks(
    val totalEnergyScale: Float = 1f,
)

/**
 * Known rower profiles, selectable manually in Settings as a fallback for
 * when BLE-name auto-detection doesn't match. GENERIC trusts the FTMS spec
 * as-is; add new entries here as more machines get confirmed on real hardware.
 */
enum class RowerModel(val quirks: RowerQuirks) {
    GENERIC(RowerQuirks()),
    NEEZE_TG002B(RowerQuirks(totalEnergyScale = 0.01f)),
}

/** Auto-detects a RowerModel from the connected device's advertised BLE name. */
object RowerQuirksRegistry {

    private val byNameContains: List<Pair<String, RowerModel>> = listOf(
        // TODO: confirm the exact advertised BLE name for the Neeze TG002B
        // Rowing Machine Z2 (sold on Amazon.es as "Neezee") and adjust the
        // match string below. This machine sends Total Energy in centikcal
        // instead of the whole kcal the FTMS spec defines (raw 213 matched a
        // physiologically-plausible ~2 kcal burn) - confirmed on real hardware.
        "Neeze" to RowerModel.NEEZE_TG002B,
    )

    fun detectModel(deviceName: String?): RowerModel {
        if (deviceName == null) return RowerModel.GENERIC
        return byNameContains.firstOrNull { (pattern, _) -> deviceName.contains(pattern, ignoreCase = true) }
            ?.second
            ?: RowerModel.GENERIC
    }
}
