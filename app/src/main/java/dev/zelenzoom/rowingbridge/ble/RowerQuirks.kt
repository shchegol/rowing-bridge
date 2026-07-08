package dev.zelenzoom.rowingbridge.ble

/**
 * Per-model corrections for machines whose FTMS implementation deviates from
 * the spec in a known, confirmed way. Defaults trust the spec as-is - only
 * add a field here once a deviation has been empirically confirmed on real
 * hardware (see RowerQuirksRegistry), never guessed for an untested machine.
 */
data class RowerQuirks(
    val totalEnergyScale: Float = 1f,
)

/** Looked up by the connected rower's advertised BLE device name. Unknown/unmatched devices get the spec-compliant defaults. */
object RowerQuirksRegistry {

    private val byNameContains: List<Pair<String, RowerQuirks>> = listOf(
        // TODO: confirm the exact advertised BLE name for the Neeze TG002B
        // Rowing Machine Z2 (sold on Amazon.es as "Neezee") and adjust the
        // match string below. This machine sends Total Energy in centikcal
        // instead of the whole kcal the FTMS spec defines (raw 213 matched a
        // physiologically-plausible ~2 kcal burn) - confirmed on real hardware.
        "Neeze" to RowerQuirks(totalEnergyScale = 0.01f),
    )

    fun forDeviceName(name: String?): RowerQuirks {
        if (name == null) return RowerQuirks()
        return byNameContains.firstOrNull { (pattern, _) -> name.contains(pattern, ignoreCase = true) }
            ?.second
            ?: RowerQuirks()
    }
}
