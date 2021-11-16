package com.clearkeep.presentation.screen.videojanus.surfacegenerator

import android.content.Context
import com.clearkeep.presentation.screen.videojanus.surfacegenerator.impl.*

class SurfacePositionFactory {
    fun createSurfaceGenerator(
        context: Context,
        size: Int,
        width: Int,
        height: Int
    ): SurfaceGenerator {
        return when (size) {
            0 -> throw IllegalArgumentException("size must be not empty")
            1 -> OneSurfaceGenerator(context, width, height)
            2 -> TwoSurfaceGenerator(context, width, height)
            3 -> ThreeSurfaceGenerator(context, width, height)
            4 -> FourSurfaceGenerator(context, width, height)
            5 -> FiveSurfaceGenerator(context, width, height)
            6 -> SixSurfaceGenerator(context, width, height)
            else -> OtherSurfaceGenerator(size, context, width, height)
        }
    }
}