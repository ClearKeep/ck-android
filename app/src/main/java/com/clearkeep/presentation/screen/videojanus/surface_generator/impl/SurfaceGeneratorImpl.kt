package com.clearkeep.presentation.screen.videojanus.surface_generator.impl

import com.clearkeep.presentation.screen.videojanus.surface_generator.SurfaceGenerator

abstract class SurfaceGeneratorImpl(private val width: Int, private val height: Int) :
    SurfaceGenerator {
    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }
}