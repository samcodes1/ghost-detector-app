package com.phasma.ghostdetector.ar

import android.util.Log
import com.google.android.filament.Engine
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spawns a dark assassin in a detected doorway, holds him there for a beat,
 * then animates him walking out into the room on the floor (Y locked).
 *
 * Important model facts (verified from the GLB at build time):
 *   - Native bbox: X≈0.238, Y≈0.402, Z≈1.000
 *   - Z is the body LENGTH (the model ships lying on its back, head along +Z)
 *   - We rotate the inner ModelNode -90° around X to stand him upright.
 *   - Height after standing+scaling = WALK_HEIGHT_M.
 *
 * Procedural walk: since the GLB has zero animations / zero skin, we fake a
 * stride by oscillating yaw + lean + vertical bob in time with a step cadence.
 */
class GhostManager(
    private val engine: Engine,
    private val modelLoader: ModelLoader,
    private val materialLoader: MaterialLoader,
) {

    private enum class Phase { IDLE_AT_DOOR, WALKING_AWAY, FADING_OUT }

    private data class Doorway(
        val worldX: Float,
        val floorY: Float,
        val worldZ: Float,
        /** Unit vector pointing OUT of the wall (into the room, toward the camera-side). */
        val outwardX: Float,
        val outwardZ: Float,
    )

    private data class Active(
        val anchorNode: AnchorNode,
        val outer: Node,        // we control this (position + yaw + lean + scale)
        val spawnedMs: Long,
        val floorY: Float,
        val startX: Float,
        val startZ: Float,
        val outX: Float,
        val outZ: Float,
        var phase: Phase = Phase.IDLE_AT_DOOR,
        /** Smoke wisps spawned at dissolution time. Each carries its own motion. */
        val smoke: MutableList<SmokeWisp> = mutableListOf(),
        var smokeSpawned: Boolean = false,
    )

    /** A single floating dark cloud puff that drifts up & shrinks. */
    private data class SmokeWisp(
        val node: io.github.sceneview.node.SphereNode,
        val originX: Float,
        val originY: Float,
        val originZ: Float,
        // Velocity in m/s (mostly upward + slight outward)
        val vX: Float,
        val vY: Float,
        val vZ: Float,
        // Time offset so wisps don't all behave identically
        val seed: Float,
        val maxRadius: Float,
    )

    private var sessionStartMs = 0L
    private var lastSpawnMs = 0L
    private var active: Active? = null

    // ── Tunables ─────────────────────────────────────────────────────────────
    private val WALK_HEIGHT_M = 2.0f
    // Spawn position offset from doorway center. We randomize per-spawn so each
    // appearance feels different.
    private val FRONT_OFFSET_MIN = 0.35f
    private val FRONT_OFFSET_MAX = 0.85f
    private val SIDE_OFFSET_MAX = 0.5f      // ± along wall edge
    private val WALK_SPEED_MPS = 0.30f
    // Cooldown between spawns is randomized (so the prank-victim never knows when)
    private val COOLDOWN_MIN_MS = 9_000L
    private val COOLDOWN_MAX_MS = 18_000L

    companion object { private const val TAG = "GhostManager" }

    private val idleDurationMs = 1_800L   // short — built-in anim plays "in place" during this
    private val walkDurationMs = 7_500L   // longer to give more screen time
    private val fadeDurationMs = 2_200L
    private val totalLifetimeMs get() = idleDurationMs + walkDurationMs + fadeDurationMs
    private var nextCooldownMs = 12_000L    // re-rolled after each spawn

    // Smoke knobs
    private val SMOKE_WISP_COUNT = 22

    fun reset() {
        active?.let { g ->
            g.smoke.forEach { w -> runCatching { w.node.destroy() } }
            runCatching { g.anchorNode.destroy() }
        }
        active = null
        sessionStartMs = 0L
        lastSpawnMs = 0L
    }

    fun onFrame(
        session: Session,
        frame: Frame,
        childNodes: MutableList<Node>,
        cameraPose: Pose,
        onEvent: (GhostEvent) -> Unit,
    ): Float {
        val now = System.currentTimeMillis()
        if (sessionStartMs == 0L) sessionStartMs = now

        if (active == null
            && (lastSpawnMs == 0L || now - lastSpawnMs > nextCooldownMs)
            && now - sessionStartMs > 4_000L
        ) {
            findDoorway(frame, cameraPose)?.let { door ->
                spawnAtDoorway(frame, door, childNodes, onEvent)
                lastSpawnMs = now
                // Roll a new random cooldown so the prank never feels predictable.
                nextCooldownMs = COOLDOWN_MIN_MS +
                    (Math.random() * (COOLDOWN_MAX_MS - COOLDOWN_MIN_MS)).toLong()
            }
        }

        active?.let { g ->
            driveAssassin(g, now)
            if (now - g.spawnedMs > totalLifetimeMs) {
                // Clean up any spawned smoke wisps first
                g.smoke.forEach { w -> runCatching { w.node.destroy() } }
                g.smoke.clear()
                runCatching { g.anchorNode.destroy() }
                childNodes.remove(g.anchorNode)
                active = null
            }
        }

        return active?.let { g ->
            val pos = g.outer.worldPosition
            val dist = hypot(pos.x - cameraPose.tx(), pos.z - cameraPose.tz())
            val proximity = (1f - (dist / 6f).coerceIn(0f, 1f))
            val intensity = when (g.phase) {
                Phase.IDLE_AT_DOOR -> 1f
                Phase.WALKING_AWAY -> 0.75f
                Phase.FADING_OUT -> 0.3f
            }
            (proximity * intensity).coerceIn(0f, 1f)
        } ?: 0f
    }

    // ── Doorway detection ────────────────────────────────────────────────────

    private fun findDoorway(frame: Frame, cameraPose: Pose): Doorway? {
        val planes = frame.getUpdatedTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
        val floors = planes.filter { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
        val walls = planes.filter { it.type == Plane.Type.VERTICAL }
        if (floors.isEmpty() || walls.isEmpty()) return null

        val floor = floors.maxByOrNull { it.extentX * it.extentZ } ?: return null
        val floorY = floor.centerPose.ty()

        // Camera forward in XZ (ARCore camera looks down -Z of its pose)
        val camFwd = cameraPose.zAxis
        val fwdX = -camFwd[0]
        val fwdZ = -camFwd[2]
        val fwdLen = sqrt(fwdX * fwdX + fwdZ * fwdZ).coerceAtLeast(1e-4f)
        val fX = fwdX / fwdLen
        val fZ = fwdZ / fwdLen

        var best: Doorway? = null
        var bestScore = -Float.MAX_VALUE
        for (wall in walls) {
            val c = wall.centerPose
            val wallHeight = wall.extentZ
            if (wallHeight < 1.2f) continue
            val bottomY = c.ty() - wallHeight / 2f
            if (abs(bottomY - floorY) > 0.5f) continue

            // Outward normal (ARCore vertical-plane normal faces toward the observed side)
            val n = c.yAxis
            val nLen = sqrt(n[0] * n[0] + n[2] * n[2]).coerceAtLeast(1e-4f)
            val nX = n[0] / nLen
            val nZ = n[2] / nLen

            val dx = c.tx() - cameraPose.tx()
            val dz = c.tz() - cameraPose.tz()
            val dist = sqrt(dx * dx + dz * dz)
            if (dist > 5f || dist < 0.4f) continue
            val cosFwd = (dx / dist) * fX + (dz / dist) * fZ
            if (cosFwd < 0.2f) continue

            val score = cosFwd - dist * 0.15f
            if (score > bestScore) {
                bestScore = score
                best = Doorway(c.tx(), floorY, c.tz(), nX, nZ)
            }
        }
        return best
    }

    // ── Spawn ────────────────────────────────────────────────────────────────

    private fun spawnAtDoorway(
        frame: Frame,
        d: Doorway,
        childNodes: MutableList<Node>,
        onEvent: (GhostEvent) -> Unit,
    ) {
        val floor = frame.getUpdatedTrackables(Plane::class.java)
            .filter {
                it.trackingState == TrackingState.TRACKING &&
                    it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
            }
            .maxByOrNull { it.extentX * it.extentZ } ?: return

        // Randomize the spawn so the prank-victim never knows exactly where it'll appear.
        // Vary distance from wall (random forward offset) AND lateral position along
        // the wall (perpendicular to the outward normal).
        val forwardOffset = FRONT_OFFSET_MIN +
            Math.random().toFloat() * (FRONT_OFFSET_MAX - FRONT_OFFSET_MIN)
        val sideOffset = (Math.random().toFloat() - 0.5f) * 2f * SIDE_OFFSET_MAX
        // Perpendicular vector in the floor plane (rotate outward normal 90° around Y)
        val perpX = -d.outwardZ
        val perpZ = d.outwardX
        val spawnX = d.worldX + d.outwardX * forwardOffset + perpX * sideOffset
        val spawnZ = d.worldZ + d.outwardZ * forwardOffset + perpZ * sideOffset

        // Anchor pose: at the spawn point on the floor. Yaw is driven on outer Node.
        val translation = floatArrayOf(spawnX, d.floorY, spawnZ)
        val identityQuat = floatArrayOf(0f, 0f, 0f, 1f)
        val pose = Pose(translation, identityQuat)
        val anchor = runCatching { floor.createAnchor(pose) }.getOrNull() ?: return
        val anchorNode = AnchorNode(engine, anchor)
        childNodes.add(anchorNode)

        // Outer wrapper Node — we move/rotate this every frame.
        val outer = Node(engine)
        outer.parent = anchorNode

        // Inner ModelNode — "standing upright" baked in:
        //   1) Rotate -90° around X so the body's long axis (native Z) maps to world Y
        //   2) Scale so the body length (native 1.0) becomes WALK_HEIGHT_M
        //   3) Translate up by half-height so feet are at the outer node's origin
        // Load the WALKER (CesiumMan) — a properly rigged humanoid with a built-in
        // walk-cycle animation. autoAnimate=true on ModelNode auto-plays animation
        // index 0, which IS the walk cycle. So legs and arms actually move.
        val instance = runCatching { modelLoader.createModelInstance(GhostType.WALKER.asset) }
            .getOrNull()
        if (instance != null) {
            val bbox = instance.asset.boundingBox
            val he = bbox.halfExtent
            val rawHeight = he[1] * 2f
            val measuredHeight = when {
                rawHeight < 0.05f -> 1.5f
                rawHeight > 100f  -> 1.5f
                else              -> rawHeight
            }
            val scaleFactor = WALK_HEIGHT_M / measuredHeight
            val bottomY = bbox.center[1] - he[1]
            Log.i(
                TAG,
                "Walker bbox center=[${bbox.center[0]},${bbox.center[1]},${bbox.center[2]}] " +
                    "halfExtent=[${he[0]},${he[1]},${he[2]}] " +
                    "rawHeight=$rawHeight → scale=$scaleFactor → renderedHeight=${measuredHeight * scaleFactor}m"
            )
            val modelNode = ModelNode(
                modelInstance = instance,
                autoAnimate = true,           // <<< plays the walk cycle automatically
            ).apply {
                parent = outer
                scale = Float3(scaleFactor)
                position = Float3(0f, -bottomY * scaleFactor, 0f)
                isShadowCaster = false
            }
            // Override the material so the character renders as a dark spectral shape
            // instead of a brightly textured man. Filament's standard PBR materials
            // expose baseColorFactor / metallicFactor / roughnessFactor; runCatching
            // each call so unrelated material types don't crash us.
            instance.materialInstances.forEach { mat ->
                runCatching { mat.setParameter("baseColorFactor", 0.06f, 0.04f, 0.12f, 1f) }
                runCatching { mat.setParameter("metallicFactor", 0f) }
                runCatching { mat.setParameter("roughnessFactor", 1f) }
                runCatching { mat.setParameter("emissiveFactor", 0.02f, 0.0f, 0.04f) }
            }
            // Keep a reference so we can sanity-check that the model loaded
            modelNode.isVisible = true
        } else {
            // GLB failed to load — black sphere fallback so the user sees SOMETHING.
            val mat = materialLoader.createColorInstance(
                color = dev.romainguy.kotlin.math.Float4(0.04f, 0.04f, 0.06f, 1f),
                metallic = 0f, roughness = 1f,
            )
            SphereNode(
                engine = engine,
                radius = 0.35f,
                center = Float3(0f, WALK_HEIGHT_M / 2f, 0f),
                materialInstance = mat,
            ).apply {
                parent = outer
                isShadowCaster = false
            }
        }

        active = Active(
            anchorNode = anchorNode,
            outer = outer,
            spawnedMs = System.currentTimeMillis(),
            floorY = d.floorY,
            startX = spawnX,
            startZ = spawnZ,
            outX = d.outwardX,
            outZ = d.outwardZ,
        )
        onEvent(GhostEvent(GhostType.WALKER, "Something is walking toward you…"))
    }

    // ── Per-frame motion ─────────────────────────────────────────────────────

    private fun driveAssassin(g: Active, now: Long) {
        val age = (now - g.spawnedMs).coerceAtLeast(0L)
        val tSec = age / 1000.0
        val baseYawDeg = Math.toDegrees(atan2(g.outX.toDouble(), g.outZ.toDouble())).toFloat()

        when {
            age < idleDurationMs -> {
                g.phase = Phase.IDLE_AT_DOOR
                // Subtle menacing sway (no stepping yet)
                val sway = sin(tSec * 2.0).toFloat() * 0.8f   // degrees
                placeOuter(
                    g,
                    posX = g.startX,
                    posZ = g.startZ,
                    posYOffset = 0f,
                    yawDeg = baseYawDeg + sway,
                    leanDeg = 0f,
                    scaleMul = 1f,
                )
            }

            age < idleDurationMs + walkDurationMs -> {
                g.phase = Phase.WALKING_AWAY
                val tWalk = ((age - idleDurationMs).toFloat() / walkDurationMs).coerceIn(0f, 1f)
                val ease = tWalk * tWalk * (3f - 2f * tWalk)
                val distance = WALK_SPEED_MPS * (walkDurationMs / 1000f) * ease

                // CesiumMan has REAL walk animation baked into the GLB (legs + arms
                // move via the skeleton). We just translate the outer node along the
                // floor — no procedural bob/sway, otherwise the two animations fight.
                placeOuter(
                    g,
                    posX = g.startX + g.outX * distance,
                    posZ = g.startZ + g.outZ * distance,
                    posYOffset = 0f,
                    yawDeg = baseYawDeg,
                    leanDeg = 0f,
                    scaleMul = 1f,
                )
            }

            else -> {
                g.phase = Phase.FADING_OUT
                val tFade = ((age - idleDurationMs - walkDurationMs).toFloat() / fadeDurationMs)
                    .coerceIn(0f, 1f)
                val endDist = WALK_SPEED_MPS * (walkDurationMs / 1000f)
                val endX = g.startX + g.outX * endDist
                val endZ = g.startZ + g.outZ * endDist

                // Spawn smoke wisps the first frame we enter the fade phase
                if (!g.smokeSpawned) {
                    spawnSmokeWisps(g, endX, endZ)
                    g.smokeSpawned = true
                }

                // The body itself collapses non-uniformly: shrink in Y faster than
                // X/Z (the head goes first, body drops into the smoke). Also sink
                // slightly below floor so the last visible bit feels swallowed.
                val bodyEase = tFade
                val bodyScaleX = (1f - bodyEase * 0.6f).coerceAtLeast(0.001f)
                val bodyScaleY = (1f - bodyEase * 1.2f).coerceAtLeast(0.001f)
                val sinkY = -bodyEase * 0.18f
                g.outer.worldPosition = Float3(endX, g.floorY + sinkY, endZ)
                g.outer.worldRotation = Float3(0f, baseYawDeg, 0f)
                g.outer.scale = Float3(bodyScaleX, bodyScaleY, bodyScaleX)

                // Drive the wisps every frame
                driveSmoke(g, tFade)
            }
        }
    }

    /**
     * Locks the outer node's world position to (X, floorY+offset, Z) and rotation
     * to a forward-lean (around the walk-axis) plus yaw around world-up. Scale is
     * uniform.
     *
     * The inner ModelNode keeps the "stand up + scale + feet-at-origin" baked-in
     * transform; we never touch it after spawn.
     */
    private fun placeOuter(
        g: Active,
        posX: Float,
        posZ: Float,
        posYOffset: Float,
        yawDeg: Float,
        leanDeg: Float,
        scaleMul: Float,
    ) {
        g.outer.worldPosition = Float3(posX, g.floorY + posYOffset, posZ)
        // World rotation as ZYX Euler in degrees: lean is forward tilt around the
        // walk axis. Since yaw rotates the body, the lean axis needs to come BEFORE
        // yaw in compose order — Euler(leanDeg, yawDeg, 0) does this via SceneView
        // (XYZ) convention. We use Float3(leanDeg, yawDeg, 0).
        g.outer.worldRotation = Float3(leanDeg, yawDeg, 0f)
        g.outer.scale = Float3(scaleMul)
    }

    // ── Smoke dissolution ──────────────────────────────────────────────────

    /**
     * At the start of the fade phase, we spawn SMOKE_WISP_COUNT little dark
     * SphereNodes inside the assassin's volume. Each gets a random velocity
     * (mostly upward, slight outward drift). Per frame, [driveSmoke] integrates
     * position, expands the wisp to its maxRadius then shrinks it to 0.
     *
     * Net visual: the body collapses down while a billowing dark cloud rises
     * up out of it and dissipates — which reads as "ghost vanishes in smoke".
     */
    private fun spawnSmokeWisps(g: Active, atX: Float, atZ: Float) {
        // Dark, matte material — looks like soot/shadow in AR
        val mat = materialLoader.createColorInstance(
            color = dev.romainguy.kotlin.math.Float4(0.08f, 0.05f, 0.10f, 1f),
            metallic = 0f,
            roughness = 1f,
            reflectance = 0f,
        )
        repeat(SMOKE_WISP_COUNT) { i ->
            val angle = (Math.random() * 2 * Math.PI).toFloat()
            val outwardSpeed = 0.10f + Math.random().toFloat() * 0.22f
            val vX = kotlin.math.cos(angle) * outwardSpeed
            val vZ = kotlin.math.sin(angle) * outwardSpeed
            val vY = 0.35f + Math.random().toFloat() * 0.55f       // mostly upward
            // Start spread INSIDE the body's silhouette
            val startSpread = 0.18f
            val ox = atX + (Math.random().toFloat() - 0.5f) * 2f * startSpread
            val oz = atZ + (Math.random().toFloat() - 0.5f) * 2f * startSpread
            val oy = g.floorY + 0.2f + Math.random().toFloat() * (WALK_HEIGHT_M * 0.7f)
            val maxR = 0.16f + Math.random().toFloat() * 0.18f      // 0.16..0.34 m
            val sphere = io.github.sceneview.node.SphereNode(
                engine = engine,
                radius = 0.001f,                                   // start tiny, will grow
                center = Float3(0f, 0f, 0f),
                materialInstance = mat,
            ).apply {
                parent = g.outer.parent  // attach under the anchor so it stays world-locked
                worldPosition = Float3(ox, oy, oz)
                isShadowCaster = false
            }
            g.smoke.add(
                SmokeWisp(
                    node = sphere,
                    originX = ox, originY = oy, originZ = oz,
                    vX = vX, vY = vY, vZ = vZ,
                    seed = Math.random().toFloat(),
                    maxRadius = maxR,
                )
            )
        }
    }

    private fun driveSmoke(g: Active, tFade: Float) {
        // Per-wisp animation:
        //   - position: origin + velocity * (tFade * durationSec)
        //   - radius (size): grows quickly to maxRadius (by t≈0.4), then shrinks to 0
        val durationSec = fadeDurationMs / 1000f
        val elapsedSec = tFade * durationSec
        g.smoke.forEach { w ->
            // Per-wisp time offset so they don't pulse in unison
            val t = (tFade + w.seed * 0.10f).coerceIn(0f, 1.05f)
            // Position
            val drift = sin((elapsedSec + w.seed) * 4.0).toFloat() * 0.015f
            val px = w.originX + w.vX * elapsedSec + drift
            val py = w.originY + w.vY * elapsedSec + sin((elapsedSec + w.seed) * 6.0).toFloat() * 0.01f
            val pz = w.originZ + w.vZ * elapsedSec + drift
            w.node.worldPosition = Float3(px, py, pz)
            // Size envelope: grow 0..maxR over t in [0, 0.35], hold to ~0.55, shrink to 0
            val sizeFactor = when {
                t < 0.35f -> t / 0.35f                            // grow
                t < 0.55f -> 1f                                   // hold
                t < 1.0f  -> (1f - (t - 0.55f) / 0.45f)           // shrink
                else      -> 0f
            }.coerceIn(0f, 1f)
            val s = (w.maxRadius * sizeFactor) / 0.001f           // SphereNode starts at 0.001 radius
            w.node.scale = Float3(s.coerceAtLeast(0.001f))
        }
    }
}
