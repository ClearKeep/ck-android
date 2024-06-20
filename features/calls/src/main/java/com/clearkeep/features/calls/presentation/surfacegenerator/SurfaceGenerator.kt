package com.clearkeep.features.calls.presentation.surfacegenerator

interface SurfaceGenerator {
    fun getLocalSurface(): SurfacePosition
    fun getRemoteSurfaces(): List<SurfacePosition>
}