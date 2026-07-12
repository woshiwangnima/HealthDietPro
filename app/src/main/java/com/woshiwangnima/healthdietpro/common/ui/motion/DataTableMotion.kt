package com.woshiwangnima.healthdietpro.common.ui.motion

internal data class DataTableMotion(
    val rowInsert: MotionSpec,
    val rowDelete: MotionSpec,
    val rowSortMovement: MotionSpec,
    val background: MotionSpec,
)

internal fun dataTableMotion(scheme: MotionScheme): DataTableMotion =
    DataTableMotion(
        rowInsert = scheme.standardEffects.copy(easing = EasingCurve.CubicOut),
        rowDelete = scheme.standardEffects.copy(easing = EasingCurve.CubicIn),
        rowSortMovement = scheme.spatialMovement,
        background = scheme.fastEffects.copy(easing = EasingCurve.SineInOut),
    )
