package com.noah.demo.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noah.demo.data.SIMON_SPRITES
import com.noah.demo.data.SPRITE_SIZE
import com.noah.demo.data.Sprite
import com.noah.demo.ui.game.Arcade
import com.noah.demo.ui.game.CrtSurface
import com.noah.demo.ui.game.Difficulty
import com.noah.demo.ui.game.rememberReducedMotion
import androidx.compose.material3.Text

/** What the game is doing right now. */
sealed interface SimonPhase {
    /** Replaying the sequence; [litIndex] is the pad currently flashing, if any. */
    data class Showing(val litIndex: Int?) : SimonPhase
    data object AwaitingInput : SimonPhase
    data class GameOver(val reached: Int) : SimonPhase
}

@Composable
fun SimonScreen(
    phase: SimonPhase,
    round: Int,
    best: Int,
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onPadTap: (Int) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val isOver = phase is SimonPhase.GameOver

    // A single red wash on failure, rather than recolouring the whole screen.
    val failWash by animateFloatAsState(
        // Enough to read as a failure state, not so much that it washes the
        // sprites out; at 0.16 the whole screen went maroon.
        targetValue = if (isOver) 0.07f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 260),
        label = "failWash",
    )

    CrtSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Arcade.Danger.copy(alpha = failWash)) }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "NOAH_DEMO",
                style = Arcade.MarqueeText,
                color = Arcade.Marquee,
            )

            Spacer(Modifier.height(14.dp))
            Divider()
            Spacer(Modifier.height(14.dp))

            ScoreRow(round = round, best = best, difficulty = difficulty)

            // Weighted on both sides so the play area sits centred between the
            // score row and the control panel rather than hugging the top.
            Spacer(Modifier.weight(1f))

            PadGrid(
                phase = phase,
                reducedMotion = reducedMotion,
                onPadTap = onPadTap,
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = when (phase) {
                    is SimonPhase.Showing -> "WATCH THE SEQUENCE"
                    SimonPhase.AwaitingInput -> "YOUR TURN"
                    is SimonPhase.GameOver -> "WRONG. YOU REACHED ${phase.reached}"
                },
                style = Arcade.LabelText,
                color = if (isOver) Arcade.Danger else Arcade.Marquee,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isOver) {
                Spacer(Modifier.height(14.dp))
                ArcadeButton(
                    label = "PLAY AGAIN",
                    accent = Arcade.Marquee,
                    selected = true,
                    enabled = true,
                    onClick = onRestart,
                )
            }

            Spacer(Modifier.weight(1f))
            Divider()
            Spacer(Modifier.height(14.dp))

            ControlPanel(
                difficulty = difficulty,
                // Changing speed mid-playback would desync what you are watching.
                enabled = phase !is SimonPhase.Showing,
                onSelect = onDifficultyChange,
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Arcade.Bezel),
    )
}

@Composable
private fun ScoreRow(round: Int, best: Int, difficulty: Difficulty) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "ROUND %02d".format(round),
            style = Arcade.ScoreText,
            color = Arcade.Marquee,
        )
        Text(
            // Best is per difficulty, so the label says which one it belongs to.
            text = "BEST %02d %s".format(best, difficulty.label),
            style = Arcade.ScoreText,
            color = Arcade.Dim,
        )
    }
}

@Composable
private fun PadGrid(
    phase: SimonPhase,
    reducedMotion: Boolean,
    onPadTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lit = (phase as? SimonPhase.Showing)?.litIndex
    val interactive = phase == SimonPhase.AwaitingInput
    val isOver = phase is SimonPhase.GameOver

    // Shake the whole grid once on failure.
    val shake by animateFloatAsState(
        targetValue = if (isOver) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = 0.22f,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "shake",
    )

    Column(
        modifier = modifier.graphicsLayer { translationX = shake * 18f },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (rowStart in listOf(0, 2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (index in rowStart until rowStart + 2) {
                    SpritePad(
                        sprite = SIMON_SPRITES[index],
                        isLit = lit == index,
                        enabled = interactive,
                        reducedMotion = reducedMotion,
                        onTap = { onPadTap(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpritePad(
    sprite: Sprite,
    isLit: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The signature: bloom rises fast and falls slowly, the way phosphor persists
    // after the beam has moved on. An abrupt off would look like a toggling div.
    val bloom by animateFloatAsState(
        targetValue = if (isLit) 1f else 0f,
        animationSpec = when {
            reducedMotion -> tween(0)
            isLit -> tween(80)
            else -> tween(280)
        },
        label = "bloom",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squash by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(if (reducedMotion) 0 else 120),
        label = "squash",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(squash)
            .clip(RoundedCornerShape(14.dp))
            .background(Arcade.Panel)
            .border(
                width = 1.dp,
                color = lerpToward(Arcade.Bezel, sprite.body, bloom),
                shape = RoundedCornerShape(14.dp),
            )
            .drawBehind {
                if (bloom > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to sprite.body.copy(alpha = 0.42f * bloom),
                            1f to Color.Transparent,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.72f,
                        ),
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onTap,
            )
            .semantics { contentDescription = sprite.name }
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // Lit pads grow slightly, so the cue is brightness and size, not
                // colour alone.
                .scale(1f + 0.10f * bloom),
        ) {
            val cell = size.width / SPRITE_SIZE
            for (y in 0 until SPRITE_SIZE) {
                for (x in 0 until SPRITE_SIZE) {
                    val base = sprite.colorAt(x, y) ?: continue
                    drawRect(
                        // Unlit pads sit back; the lit one comes forward.
                        color = base.copy(alpha = 0.55f + 0.45f * bloom),
                        topLeft = Offset(x * cell, y * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    difficulty: Difficulty,
    enabled: Boolean,
    onSelect: (Difficulty) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Difficulty.entries.forEach { option ->
            ArcadeButton(
                label = option.label,
                accent = SIMON_SPRITES[Difficulty.entries.indexOf(option)].body,
                selected = option == difficulty,
                enabled = enabled,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ArcadeButton(
    label: String,
    accent: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = when {
        selected -> accent
        enabled -> Arcade.Dim
        else -> Arcade.Bezel
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, tint, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            // Horizontal padding matters as much as vertical: without it the label
            // runs into the border on a wrap-content button like "PLAY AGAIN".
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Arcade.LabelText,
            color = if (selected) accent else tint,
        )
    }
}

/** Blends [from] toward [to]; used so a lit pad's border takes its sprite colour. */
private fun lerpToward(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = 1f,
)
