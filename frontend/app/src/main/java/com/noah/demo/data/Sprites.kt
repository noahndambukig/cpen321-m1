package com.noah.demo.data

import androidx.compose.ui.graphics.Color

/** Every sprite is a 16x16 bitmap, matching the Button 2 canvas. */
const val SPRITE_SIZE = 16

/**
 * An original 8-bit style sprite.
 *
 * `#` is the body colour, `O` the accent, `.` transparent. Deliberately drawn
 * from scratch in the arcade idiom rather than copying any existing character
 * art, which would be someone else's copyright.
 */
data class Sprite(
    val name: String,
    val body: Color,
    val accent: Color,
    /** The note this pad sounds; the four form a pentatonic scale. */
    val note: ToneBox.Note,
    val rows: List<String>,
) {
    init {
        require(rows.size == SPRITE_SIZE) { "$name has ${rows.size} rows" }
        require(rows.all { it.length == SPRITE_SIZE }) { "$name has a malformed row" }
    }

    fun colorAt(x: Int, y: Int): Color? = when (rows[y][x]) {
        '#' -> body
        'O' -> accent
        else -> null
    }
}

private val ALIEN = Sprite(
    name = "Alien",
    body = Color(0xFF5BD75B),
    accent = Color(0xFF1B4D1B),
    note = ToneBox.Note.C5,
    rows = listOf(
        "................",
        "................",
        "....#......#....",
        ".....#....#.....",
        "..############..",
        ".###.######.###.",
        "################",
        "##.##########.##",
        "##.OO######OO.##",
        "################",
        "..############..",
        "....##....##....",
        "...##..##..##...",
        "..##...##...##..",
        "..#....##....#..",
        "................",
    ),
)

private val GHOST = Sprite(
    name = "Ghost",
    body = Color(0xFF7EC8F5),
    accent = Color(0xFF10233A),
    note = ToneBox.Note.E5,
    rows = listOf(
        "................",
        ".....######.....",
        "...##########...",
        "..############..",
        ".##############.",
        ".###OO####OO###.",
        ".##OOOO##OOOO##.",
        ".##OOOO##OOOO##.",
        ".###OO####OO###.",
        ".##############.",
        ".##############.",
        ".##############.",
        ".##############.",
        ".##.##.##.##.##.",
        ".#..#..##..#..#.",
        "................",
    ),
)

private val MUSHROOM = Sprite(
    name = "Mushroom",
    body = Color(0xFFE8453C),
    accent = Color(0xFFDCBF89),
    note = ToneBox.Note.G5,
    rows = listOf(
        "................",
        ".....######.....",
        "...##########...",
        "..############..",
        "..###OOOO#####..",
        ".####OOOO######.",
        ".##############.",
        ".##OOO####OOO##.",
        ".###OO####OO###.",
        ".##############.",
        "..####OOOO####..",
        "....OOOOOOOO....",
        "....O.OOOO.O....",
        "....OOOOOOOO....",
        ".....OOOOOO.....",
        "................",
    ),
)

private val STAR = Sprite(
    name = "Star",
    body = Color(0xFFFFC93C),
    accent = Color(0xFF8A5A00),
    note = ToneBox.Note.A5,
    rows = listOf(
        "................",
        ".......##.......",
        ".......##.......",
        "......####......",
        "......####......",
        ".##############.",
        ".##############.",
        "..############..",
        "...##########...",
        "...##########...",
        "..############..",
        "..###......###..",
        ".###........###.",
        ".##..........##.",
        "................",
        "................",
    ),
)

/** The four pads, in fixed order: indices are what the sequence stores. */
val SIMON_SPRITES: List<Sprite> = listOf(ALIEN, GHOST, MUSHROOM, STAR)
