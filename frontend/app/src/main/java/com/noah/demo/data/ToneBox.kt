package com.noah.demo.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Plays short musical notes for the Simon pads.
 *
 * Pads are tuned to a C major pentatonic scale, so whatever order the game happens
 * to generate is consonant. The DTMF tones this replaced sounded like a phone
 * keypad because they are literally telephone signalling tones.
 *
 * Notes are queued through a single consumer on one dedicated thread.
 * `AudioTrack.write` blocks, and concurrent writers would interleave their samples
 * into each other's frames, so ordering has to be enforced rather than assumed.
 */
class ToneBox {

    /** C major pentatonic, plus a low detuned note for a wrong answer. */
    enum class Note(val hz: Double) {
        C5(523.25),
        E5(659.25),
        G5(783.99),
        A5(880.00),
        WRONG(123.47),
    }

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ToneBox").apply { isDaemon = true }
    }
    private val scope = CoroutineScope(executor.asCoroutineDispatcher())

    /** Dropping is the right failure mode: a late note is worse than a missing one. */
    private val queue = Channel<Note>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(SAMPLE_RATE),
        )
        .build()

    @Volatile
    private var released = false

    init {
        runCatching { track.play() }
        scope.launch {
            for (note in queue) {
                if (released) break
                val samples = render(note.hz, NOTE_MS)
                runCatching { track.write(samples, 0, samples.size) }
            }
        }
    }

    /** Non-blocking. Safe to call from a composable or the main thread. */
    fun play(note: Note) {
        if (!released) queue.trySend(note)
    }

    /**
     * Stops accepting notes, lets the in-flight write finish, then tears down.
     * Releasing while a write is still running throws IllegalStateException.
     */
    fun release() {
        if (released) return
        released = true
        queue.close()
        // Runs after the consumer loop's current iteration, because both are on the
        // same single thread.
        executor.execute {
            runCatching {
                track.stop()
                track.release()
            }
        }
        executor.shutdown()
    }

    private fun render(hz: Double, durationMs: Int): ShortArray {
        val total = SAMPLE_RATE * durationMs / 1000
        val attack = SAMPLE_RATE * ATTACK_MS / 1000
        val release = SAMPLE_RATE * RELEASE_MS / 1000
        val out = ShortArray(total)
        for (i in 0 until total) {
            // Without the ramps the abrupt start and end of the wave click audibly.
            val envelope = when {
                i < attack -> i.toFloat() / attack
                i > total - release -> (total - i).toFloat() / release
                else -> 1f
            }
            out[i] = (sin(2.0 * PI * hz * i / SAMPLE_RATE) * envelope * AMPLITUDE)
                .toInt()
                .toShort()
        }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val AMPLITUDE = 7000
        const val NOTE_MS = 260
        const val ATTACK_MS = 6
        const val RELEASE_MS = 70
    }
}
