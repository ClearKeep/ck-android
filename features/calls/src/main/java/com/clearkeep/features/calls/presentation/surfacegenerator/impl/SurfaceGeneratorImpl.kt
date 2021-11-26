package com.clearkeep.features.calls.presentation.surfacegenerator.impl

import com.clearkeep.features.calls.presentation.surfacegenerator.SurfaceGenerator

abstract class SurfaceGeneratorImpl(private val width: Int, private val height: Int) :
    SurfaceGenerator {
    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }
}