package com.phasma.ghostdetector.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Fullscreen, looping, **center-crop** scare video. Uses a custom TextureView so
 * we can apply a transform Matrix that fills the entire surface with no black
 * bars — same idea as ImageView's CENTER_CROP scale type, but for video.
 */
@Composable
fun JumpScareVideo(
    @RawRes rawResId: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    onCompleted: () -> Unit = {},
) {
    val playerRef = remember { PlayerRef() }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val textureView = TextureView(ctx)
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    val surface = Surface(st)
                    val mp = MediaPlayer().apply {
                        val afd = ctx.resources.openRawResourceFd(rawResId)
                        afd.use {
                            setDataSource(it.fileDescriptor, it.startOffset, it.declaredLength)
                        }
                        setSurface(surface)
                        setVolume(0f, 0f)              // muted — we layer our own scream
                        isLooping = loop
                        setOnVideoSizeChangedListener { _, vw, vh ->
                            if (vw > 0 && vh > 0) applyCenterCrop(textureView, vw, vh)
                        }
                        setOnPreparedListener { player ->
                            applyCenterCrop(textureView, player.videoWidth, player.videoHeight)
                            runCatching { start() }
                        }
                        setOnCompletionListener { onCompleted() }
                        prepareAsync()
                    }
                    playerRef.mp = mp
                    playerRef.surface = surface
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                    val mp = playerRef.mp ?: return
                    if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                        applyCenterCrop(textureView, mp.videoWidth, mp.videoHeight)
                    }
                }

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    playerRef.release()
                    return true
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
            }
            textureView
        }
    )
    DisposableEffect(Unit) {
        onDispose { playerRef.release() }
    }
}

/**
 * Apply a transform to the TextureView that fills the whole view with the video,
 * keeping the video's aspect ratio (crop on the longer side). This is the
 * equivalent of `ImageView.ScaleType.CENTER_CROP` for video.
 */
private fun applyCenterCrop(view: TextureView, videoW: Int, videoH: Int) {
    val viewW = view.width.toFloat()
    val viewH = view.height.toFloat()
    if (viewW <= 0f || viewH <= 0f || videoW <= 0 || videoH <= 0) return
    // The TextureView is sized viewW × viewH but the underlying surface buffer
    // is rendered to the video's native dimensions. We compose a matrix that
    // (1) maps the video's native rect to the view's rect, then
    // (2) scales by max(viewW/videoW, viewH/videoH) so the video fills both
    //     dimensions, with the longer dimension cropped.
    val sx = viewW / videoW
    val sy = viewH / videoH
    val scale = maxOf(sx, sy)
    val dx = (viewW - videoW * scale) / 2f
    val dy = (viewH - videoH * scale) / 2f
    val m = Matrix()
    // Step 1: bring the video frame to fit the view exactly (sx, sy)
    m.setScale(sx, sy)
    // Step 2: pre-scale by overall scale factor relative to fit
    m.postScale(scale / sx, scale / sy, viewW / 2f, viewH / 2f)
    // The post-scale is centered, so no translation is needed
    view.setTransform(m)
    view.invalidate()
}

private class PlayerRef {
    var mp: MediaPlayer? = null
    var surface: Surface? = null
    fun release() {
        runCatching { mp?.stop() }
        runCatching { mp?.release() }
        runCatching { surface?.release() }
        mp = null
        surface = null
    }
}
