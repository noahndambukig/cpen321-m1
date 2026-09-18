package com.noah.demo.ui.game

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** One background star: position as a fraction of the surface, so it scales. */
private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Aperture-grille tints. A real shadow mask splits the beam into RGB triads. */
private val GrilleRed = Color(0xFFFF2A2A)
private val GrilleGreen = Color(0xFF2AFF5A)
private val GrilleBlue = Color(0xFF2A6AFF)

/**
 * The inside of a CRT: dark ground, then the content, then the artefacts on top.
 *
 * Fills the whole window including behind the system bars, so the app theme's
 * wallpaper-derived colour never frames the screen. Insets are applied to the
 * content rather than the surface.
 *
 * The overlays use `drawWithContent` rather than `drawBehind`: `drawBehind` paints
 * underneath the child, which would put the artefacts behind the very thing they
 * are supposed to fall across.
 */
@Composable
fun CrtSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // The app theme is light, so the system bars default to dark icons. On this
    // near-black surface they are almost invisible, so flip them while these
    // screens are shown and restore on the way out.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            previous?.let {
                controller.isAppearanceLightStatusBars = it
                controller.isAppearanceLightNavigationBars = it
            }
        }
    }

    // One slow bright band travelling down the tube, as if the screen were being
    // refreshed. Deliberately slow and faint: it should register as atmosphere, not
    // as something demanding attention.
    val reducedMotion = rememberReducedMotion()
    val animatedSweep by rememberInfiniteTransition(label = "crt").animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    // Parked off screen when motion is reduced, so the band never draws.
    val sweep = if (reducedMotion) 2f else animatedSweep

    // A fixed starfield behind everything. Seeded so it is identical on every
    // launch and never churns on recomposition. It gives the tube some depth
    // without competing with the sprites, and suits an alien and a star.
    val stars = remember {
        val rng = Random(seed = 7)
        List(140) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                radius = if (rng.nextFloat() > 0.86f) 2.0f else 1.1f,
                alpha = 0.06f + rng.nextFloat() * 0.20f,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Arcade.Void)
            .drawWithContent {
                // Background layer, painted under the content.
                stars.forEach { star ->
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha),
                        radius = star.radius,
                        center = Offset(star.x * size.width, star.y * size.height),
                    )
                }

                drawContent()

                // Aperture grille: vertical RGB triads, the mask a colour CRT uses
                // to separate the beams. Very low alpha; it reads as texture.
                var x = 0f
                val triad = 3f
                var index = 0
                while (x < size.width) {
                    val tint = when (index % 3) {
                        0 -> GrilleRed
                        1 -> GrilleGreen
                        else -> GrilleBlue
                    }
                    drawRect(
                        color = tint.copy(alpha = 0.05f),
                        topLeft = Offset(x, 0f),
                        size = Size(1f, size.height),
                    )
                    x += triad
                    index++
                }

                // Scanlines: one dark line every third pixel row.
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = Arcade.Void.copy(alpha = 0.55f),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, 1f),
                    )
                    y += 3f
                }

                // The refresh band.
                if (sweep in -0.3f..1.3f) {
                    val bandHeight = size.height * 0.18f
                    val centre = size.height * sweep
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.030f),
                            1f to Color.Transparent,
                            startY = centre - bandHeight / 2f,
                            endY = centre + bandHeight / 2f,
                        ),
                        topLeft = Offset(0f, centre - bandHeight / 2f),
                        size = Size(size.width, bandHeight),
                    )
                }

                // Vignette: the tube is brighter in the middle than at the corners.
                drawRect(
                    brush = Brush.radialGradient(
                        0.55f to Arcade.Void.copy(alpha = 0f),
                        1.0f to Arcade.Void.copy(alpha = 0.85f),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width, size.height) * 0.75f,
                    ),
                    size = size,
                )
            },
    ) {
        Box(Modifier.safeDrawingPadding()) { content() }
    }
}

/**
 * True when the user has turned animations off system-wide. Motion is decorative
 * here, so every animation collapses to an instant change rather than being
 * approximated.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
