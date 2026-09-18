package com.noah.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.noah.demo.data.SIMON_SPRITES
import com.noah.demo.data.ToneBox
import com.noah.demo.ui.game.Difficulty
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val LEAD_IN_MS = 700L

private enum class TimerPhase { Setup, Running, Fired }

/**
 * Button 3: a user-set countdown that, on reaching zero, drops into a Simon-style
 * memory game played with 8-bit sprites rather than colours.
 */
@Composable
fun TimerRoute(modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(TimerPhase.Setup) }
    var minutes by remember { mutableStateOf("0") }
    var seconds by remember { mutableStateOf("5") }
    var remaining by remember { mutableIntStateOf(0) }

    when (phase) {
        TimerPhase.Setup -> TimerSetupScreen(
            minutes = minutes,
            seconds = seconds,
            onMinutesChange = { minutes = it },
            onSecondsChange = { seconds = it },
            onStart = {
                remaining = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
                phase = TimerPhase.Running
            },
            modifier = modifier,
        )

        TimerPhase.Running -> {
            LaunchedEffect(Unit) {
                while (remaining > 0) {
                    delay(1_000)
                    remaining--
                }
                phase = TimerPhase.Fired
            }
            TimerRunningScreen(
                remainingSeconds = remaining,
                onCancel = { phase = TimerPhase.Setup },
                modifier = modifier,
            )
        }

        TimerPhase.Fired -> SimonGame(modifier)
    }
}

@Composable
private fun SimonGame(modifier: Modifier = Modifier) {
    val sequence = remember { mutableStateListOf<Int>() }
    var phase by remember { mutableStateOf<SimonPhase>(SimonPhase.Showing(null)) }
    var expectedIndex by remember { mutableIntStateOf(0) }
    var generation by remember { mutableIntStateOf(0) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }

    // A run on Easy is not comparable to one on Hard, so each speed keeps its own
    // best rather than sharing one number that the slowest setting would dominate.
    val bests = remember { mutableStateMapOf<Difficulty, Int>() }

    val tones = remember { ToneBox() }
    DisposableEffect(Unit) { onDispose { tones.release() } }

    // Keyed on difficulty as well as generation: changing speed cancels any
    // in-flight playback and starts a clean round, so input never opens over a
    // half-cleared sequence.
    LaunchedEffect(generation, difficulty) {
        // Input stays shut for the whole lead-in and replay. Leaving it open here
        // is what previously let a tap index past the end of the sequence.
        phase = SimonPhase.Showing(null)
        expectedIndex = 0

        delay(LEAD_IN_MS)
        sequence.add(Random.nextInt(SIMON_SPRITES.size))

        for (step in sequence) {
            phase = SimonPhase.Showing(step)
            tones.play(SIMON_SPRITES[step].note)
            delay(difficulty.flashMs)
            phase = SimonPhase.Showing(null)
            delay(difficulty.gapMs)
        }

        expectedIndex = 0
        phase = SimonPhase.AwaitingInput
    }

    SimonScreen(
        phase = phase,
        round = sequence.size,
        best = bests[difficulty] ?: 0,
        difficulty = difficulty,
        onDifficultyChange = { chosen ->
            if (chosen == difficulty) return@SimonScreen
            // Clear before switching: the LaunchedEffect restart is what re-arms
            // input, and it must find an empty sequence.
            sequence.clear()
            expectedIndex = 0
            phase = SimonPhase.Showing(null)
            difficulty = chosen
        },
        onPadTap = { tapped ->
            // Two guards, not one. The phase check is the intent; the bounds check
            // means no future edit to the phase machine can walk off the end.
            if (phase != SimonPhase.AwaitingInput) return@SimonScreen
            if (expectedIndex !in sequence.indices) return@SimonScreen

            if (tapped != sequence[expectedIndex]) {
                tones.play(ToneBox.Note.WRONG)
                bests[difficulty] = maxOf(bests[difficulty] ?: 0, sequence.size - 1)
                phase = SimonPhase.GameOver(sequence.size)
                return@SimonScreen
            }

            tones.play(SIMON_SPRITES[tapped].note)
            expectedIndex++

            if (expectedIndex == sequence.size) {
                bests[difficulty] = maxOf(bests[difficulty] ?: 0, sequence.size)
                // Shut input immediately. generation++ only schedules the restart;
                // without this the screen stays interactive through the lead-in.
                phase = SimonPhase.Showing(null)
                generation++
            }
        },
        onRestart = {
            sequence.clear()
            expectedIndex = 0
            phase = SimonPhase.Showing(null)
            generation++
        },
        modifier = modifier,
    )
}
