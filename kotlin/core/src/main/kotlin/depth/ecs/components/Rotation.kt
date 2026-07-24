package depth.ecs.components

sealed class Rotation {
    object YawLeft: Rotation()
    object YawRight: Rotation()
    object PitchUp: Rotation()
    object PitchDown: Rotation()
    object RollLeft: Rotation()
    object RollRight: Rotation()
}
