package com.clearkeep.screen.videojanus.surface_generator.impl

import android.content.Context
import com.clearkeep.screen.videojanus.surface_generator.SurfaceGenerator

abstract class SurfaceGeneratorImpl(context: Context) : SurfaceGenerator {
    var widthPixels = 0

    var heightPixels = 0

    init {
        heightPixels = context.resources.displayMetrics.heightPixels
        widthPixels = context.resources.displayMetrics.widthPixels
    }

    fun getWidth() : Int {
        return widthPixels
    }

    fun getHeight() : Int {
        return heightPixels
    }
}