package com.phasma.ghostdetector.ar

/**
 * Single ghost type used by Ghost Finder: a dark, faceless assassin figure that
 * materializes in a detected doorway and then glides away across the floor.
 */
enum class GhostType(
    val displayName: String,
    val auraColor: Long,
    val asset: String,
) {
    /** Animated humanoid (rigged, walk cycle baked in). Rendered with dark material so it looks spectral. */
    WALKER(
        displayName = "Walking presence",
        auraColor = 0xFFFF3B6B,
        asset = "models/walker.glb",
    ),
    /** Hooded faceless static figure for the "standing in the doorway" moment. */
    HOODED(
        displayName = "Faceless presence",
        auraColor = 0xFF9D5BFF,
        asset = "models/assassin.glb",
    ),
}

data class GhostEvent(
    val type: GhostType,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis()
)
