package com.noah.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noah.demo.data.SIMON_SPRITES
import com.noah.demo.ui.game.Arcade
import com.noah.demo.ui.game.CrtSurface

/** Button 3's timer, before it fires. */
@Composable
fun TimerSetupScreen(
    minutes: String,
    seconds: String,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secondsValue = seconds.toIntOrNull() ?: 0
    val totalSeconds = (minutes.toIntOrNull() ?: 0) * 60 + secondsValue
    val secondsInvalid = secondsValue > 59
    val canStart = totalSeconds > 0 && !secondsInvalid

    CrtSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("NOAH_DEMO", style = Arcade.MarqueeText, color = Arcade.Marquee)

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Arcade.Bezel),
            )
            Spacer(Modifier.height(28.dp))

            Text(
                "SET A COUNTDOWN",
                style = Arcade.LabelText,
                color = Arcade.Marquee,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "SOMETHING HAPPENS AT ZERO",
                style = Arcade.LabelText,
                color = Arcade.Dim,
            )

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ArcadeField(
                    value = minutes,
                    onValueChange = { onMinutesChange(it.filter(Char::isDigit).take(2)) },
                    label = "MINUTES",
                    modifier = Modifier.weight(1f),
                )
                ArcadeField(
                    value = seconds,
                    onValueChange = { onSecondsChange(it.filter(Char::isDigit).take(2)) },
                    label = "SECONDS",
                    isError = secondsInvalid,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = if (secondsInvalid) "SECONDS MUST BE 59 OR LESS" else "TOTAL ${totalSeconds}S",
                style = Arcade.LabelText,
                color = if (secondsInvalid) Arcade.Danger else Arcade.Dim,
            )

            Spacer(Modifier.height(28.dp))

            StartButton(enabled = canStart, onClick = onStart)
        }
    }
}

@Composable
private fun ArcadeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val accent = if (isError) Arcade.Danger else SIMON_SPRITES[3].body
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = Arcade.LabelText) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = Arcade.ScoreText.copy(color = Arcade.Marquee),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = Arcade.Bezel,
            errorBorderColor = Arcade.Danger,
            focusedLabelColor = accent,
            unfocusedLabelColor = Arcade.Dim,
            cursorColor = accent,
            focusedTextColor = Arcade.Marquee,
            unfocusedTextColor = Arcade.Marquee,
            focusedContainerColor = Arcade.Panel,
            unfocusedContainerColor = Arcade.Panel,
            errorContainerColor = Arcade.Panel,
        ),
        modifier = modifier,
    )
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit) {
    val accent = if (enabled) SIMON_SPRITES[0].body else Arcade.Bezel
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) accent.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "START TIMER",
            style = Arcade.LabelText,
            color = if (enabled) accent else Arcade.Dim,
        )
    }
}

/** The countdown itself. */
@Composable
fun TimerRunningScreen(
    remainingSeconds: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The digits are the only light source on this screen, so they get the bloom
    // the game pads use, standing in for the glow of a powered display.
    val glow = SIMON_SPRITES[3].body

    CrtSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // A fixed-size glow plate rather than one sized to the text: the
            // gradient needs room to reach full transparency before the edge, or
            // it gets cut off square at the top and bottom.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to glow.copy(alpha = 0.26f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.height * 0.48f,
                            ),
                        )
                    },
            ) {
                Text(
                    text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                    style = Arcade.CountdownText,
                    color = glow,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Arcade.Dim, RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            ) {
                Text("CANCEL", style = Arcade.LabelText, color = Arcade.Dim)
            }
        }
    }
}
