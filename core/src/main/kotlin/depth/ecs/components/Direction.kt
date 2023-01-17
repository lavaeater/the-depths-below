package depth.ecs.components

sealed class Direction {
    object Left: Direction()
    object Right: Direction()
    object Up: Direction()
    object Down: Direction()
    object Forward: Direction()
    object Reverse: Direction()
    object Neutral: Direction()
}
