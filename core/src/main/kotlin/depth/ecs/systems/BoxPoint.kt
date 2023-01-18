package depth.ecs.systems

data class BoxPoint(val coord: PointCoord, val isoValue: Float) {
    val x:Int get() = coord.x
    val y:Int get() = coord.y
    val z:Int get() = coord.z
}
