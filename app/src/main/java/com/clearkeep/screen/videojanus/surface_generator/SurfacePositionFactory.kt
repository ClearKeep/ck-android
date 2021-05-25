package com.clearkeep.screen.videojanus.surface_generator

import android.content.Context
import com.clearkeep.screen.videojanus.surface_generator.impl.*

class SurfacePositionFactory {
    fun createSurfaceGenerator(context: Context, size: Int) : SurfaceGenerator {
        return when (size) {
            0 -> throw IllegalArgumentException("size must be not empty")
            1 -> OneSurfaceGenerator(context)
            2 -> TwoSurfaceGenerator(context)
            3 -> ThreeSurfaceGenerator(context)
            4 -> FourSurfaceGenerator(context)
            5 -> FiveSurfaceGenerator(context)
            6 -> SixSurfaceGenerator(context)
            else -> OtherSurfaceGenerator(size, context)
        }
    }
}