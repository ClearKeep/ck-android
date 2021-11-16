package com.clearkeep.presentation.screen.videojanus.surfacegenerator.impl

import com.clearkeep.presentation.screen.videojanus.surfacegenerator.SurfaceGenerator

abstract class SurfaceGeneratorImpl(private val width: Int, private val height: Int) :
    SurfaceGenerator {
    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }
}