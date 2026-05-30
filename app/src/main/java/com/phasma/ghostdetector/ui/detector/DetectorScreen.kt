package com.phasma.ghostdetector.ui.detector

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.ar.core.Config
import com.google.ar.core.TrackingFailureReason
import com.phasma.ghostdetector.PhasmaApp
import com.phasma.ghostdetector.ar.GhostEvent
import com.phasma.ghostdetector.ar.GhostManager
import com.phasma.ghostdetector.audio.Sfx
import com.phasma.ghostdetector.audio.SfxGroup
import com.phasma.ghostdetector.ui.components.EmfMeter
import com.phasma.ghostdetector.ui.theme.PhasmaBone
import com.phasma.ghostdetector.ui.theme.PhasmaDanger
import com.phasma.ghostdetector.ui.theme.PhasmaGlow
import com.phasma.ghostdetector.ui.theme.PhasmaInk
import com.phasma.ghostdetector.ui.theme.PhasmaSpectral
import com.phasma.ghostdetector.ui.theme.PhasmaVoid
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberView
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DetectorScreen(
    onExit: () -> Unit,
    vm: DetectorViewModel = viewModel()
) {
    val cameraPerm = rememberPermissionState(Manifest.permission.CAMERA)
    LaunchedEffect(cameraPerm.status) {
        if (!cameraPerm.status.isGranted) cameraPerm.launchPermissionRequest()
        vm.setPermission(cameraPerm.status.isGranted)
    }

    Box(modifier = Modifier.fillMaxSize().background(PhasmaVoid)) {
        if (cameraPerm.status.isGranted) {
            ARDetectorContent(vm = vm)
        } else {
            PermissionPrompt(onRequest = { cameraPerm.launchPermissionRequest() })
        }
        HudOverlay(state = vm.state, onExit = onExit)
    }
}

@Composable
private fun ARDetectorContent(vm: DetectorViewModel) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode: ARCameraNode = io.github.sceneview.ar.rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val haptics = LocalHapticFeedback.current
    val sound = remember { PhasmaApp.instance.sound }

    val ghostManager = remember { GhostManager(engine, modelLoader, materialLoader) }

    // Ambient drone while hunting; stops on exit
    DisposableEffect(Unit) {
        sound.startAmbient()
        onDispose { sound.stopAmbient() }
    }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        childNodes = childNodes,
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        collisionSystem = collisionSystem,
        planeRenderer = true,
        sessionConfiguration = { session, config ->
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.depthMode = runCatching {
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                    Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            }.getOrDefault(Config.DepthMode.DISABLED)
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        },
        onSessionUpdated = { session, frame ->
            if (frame.camera.trackingState != com.google.ar.core.TrackingState.TRACKING) {
                return@ARScene
            }
            val cameraPose = frame.camera.pose
            val signal = ghostManager.onFrame(
                session = session,
                frame = frame,
                childNodes = childNodes,
                cameraPose = cameraPose,
                onEvent = { event: GhostEvent ->
                    vm.logEvent(event)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Layer a sting + random whisper for impact
                    sound.play(Sfx.StingAppear, volumeMul = 0.9f)
                    sound.playRandom(SfxGroup.Whispers, volumeMul = 0.6f)
                    sound.vibrateAlert()
                }
            )
            vm.setSignal(signal)
        },
        onTrackingFailureChanged = { reason: TrackingFailureReason? ->
            vm.setStatus(
                when (reason) {
                    null, TrackingFailureReason.NONE ->
                        "Searching for an open door…"
                    TrackingFailureReason.INSUFFICIENT_LIGHT ->
                        "Too dark for spirits to manifest."
                    TrackingFailureReason.EXCESSIVE_MOTION ->
                        "Hold the camera still."
                    TrackingFailureReason.INSUFFICIENT_FEATURES ->
                        "Aim at a textured wall or doorway."
                    TrackingFailureReason.BAD_STATE ->
                        "Connection to the other side lost. Re-scan."
                    TrackingFailureReason.CAMERA_UNAVAILABLE ->
                        "Camera unavailable."
                }
            )
        }
    )
}

@Composable
private fun HudOverlay(state: DetectorState, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top bar: exit + signal title + EMF
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HudIconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = "Exit", tint = PhasmaBone)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SPECTRAL  SIGNAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = PhasmaBone.copy(alpha = 0.75f)
                )
                Text(
                    "${(state.signalLevel * 100).toInt()}%",
                    color = signalColor(state.signalLevel),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            EmfMeter(
                level = state.signalLevel,
                modifier = Modifier.size(width = 96.dp, height = 36.dp)
            )
        }

        // Center reticle
        ScannerReticle(
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp),
            level = state.signalLevel
        )

        // Status + event log on the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                state.statusText,
                style = MaterialTheme.typography.labelLarge,
                color = PhasmaBone.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PhasmaInk.copy(alpha = 0.78f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PhasmaGlow.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "ENCOUNTER  LOG",
                        style = MaterialTheme.typography.labelLarge,
                        color = PhasmaGlow
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.events.isEmpty()) {
                        Text(
                            "No encounters yet. Scan the room.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PhasmaBone.copy(alpha = 0.5f)
                        )
                    } else {
                        state.events.forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(e.type.auraColor.toInt()))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${e.type.displayName} — ${e.message}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PhasmaBone.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Pulse flash overlay when signal is high
        AnimatedVisibility(
            visible = state.signalLevel > 0.6f,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                "⚠  PRESENCE NEAR  ⚠",
                color = PhasmaDanger,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(top = 64.dp)
                    .background(PhasmaVoid.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HudIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PhasmaInk.copy(alpha = 0.7f))
            .border(1.dp, PhasmaGlow.copy(alpha = 0.4f), CircleShape)
    ) { content() }
}

@Composable
private fun ScannerReticle(modifier: Modifier, level: Float) {
    val transition = rememberInfiniteTransition(label = "reticle")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "rt"
    )
    val color = signalColor(level)
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = minOf(size.width, size.height) / 2f
        // Concentric rings
        for (i in 0..2) {
            val r = outer * (0.45f + i * 0.18f)
            drawCircle(
                color = color.copy(alpha = 0.18f + 0.10f * sin((t + i * 0.3f) * 6.28f)),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6f)
            )
        }
        // Sweep arc
        drawArc(
            color = color.copy(alpha = 0.6f),
            startAngle = -90f + t * 360f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(cx - outer * 0.79f, cy - outer * 0.79f),
            size = Size(outer * 1.58f, outer * 1.58f),
            style = Stroke(width = 3f)
        )
        // Crosshair
        val ch = outer * 0.12f
        drawLine(color, Offset(cx - ch, cy), Offset(cx + ch, cy), strokeWidth = 2f)
        drawLine(color, Offset(cx, cy - ch), Offset(cx, cy + ch), strokeWidth = 2f)
        // Center dot
        drawCircle(color, radius = 4f, center = Offset(cx, cy))
    }
}

private fun signalColor(level: Float): Color = when {
    level < 0.35f -> PhasmaSpectral
    level < 0.7f -> PhasmaGlow
    else -> PhasmaDanger
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CAMERA REQUIRED",
            style = MaterialTheme.typography.displayMedium,
            color = PhasmaBone
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Phasma needs the camera to scan your surroundings for spectral activity.",
            style = MaterialTheme.typography.bodyLarge,
            color = PhasmaBone.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.Button(
            onClick = onRequest,
        ) { Text("GRANT CAMERA ACCESS") }
    }
}
