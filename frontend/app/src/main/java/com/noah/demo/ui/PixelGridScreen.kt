package com.noah.demo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.noah.demo.data.GRID_SIZE
import com.noah.demo.data.PixelUpdate

/**
 * The 16x16 canvas, repainted one cell at a time as updates arrive.
 *
 * Cells live in a plain IntArray because reallocating a list 20 times a second
 * would churn; [version] is the snapshot state that drives recomposition.
 */
class PixelGridState {
    private val cells = IntArray(GRID_SIZE * GRID_SIZE)

    var version by mutableIntStateOf(0)
        private set

    fun paint(update: PixelUpdate) {
        cells[update.y * GRID_SIZE + update.x] = update.color
        version++
    }

    fun clear() {
        cells.fill(0)
        version++
    }

    fun colorAt(x: Int, y: Int): Int = cells[y * GRID_SIZE + x]
}

@Composable
fun PixelGridScreen(
    grid: PixelGridState,
    status: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("Live Updates", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            // Read version so painting a cell invalidates this draw.
            @Suppress("UNUSED_EXPRESSION")
            grid.version

            val cell = size.width / GRID_SIZE
            drawRect(color = Color.White, size = Size(size.width, size.width))

            for (y in 0 until GRID_SIZE) {
                for (x in 0 until GRID_SIZE) {
                    val argb = grid.colorAt(x, y)
                    if (argb != 0) {
                        drawRect(
                            color = Color(argb),
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell, cell),
                        )
                    }
                }
            }
        }
    }
}
